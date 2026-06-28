# AI Retrieval Pipeline

**Location:** `docs/03-ai/AI-Retrieval-Pipeline.md`

---

# Purpose

The AI Retrieval Pipeline converts natural language questions into accurate, grounded responses by retrieving structured data from the Life State Engine before calling the LLM.

The LLM never has direct access to the entire database. Only the objects most relevant to the current query are included in the prompt. This minimises hallucinations, reduces token usage, and ensures every AI response is traceable to stored structured data.

---

# Philosophy

AI retrieves. It does not remember.

Every response is generated from structured data retrieved at query time. Conversation history provides conversational continuity only. Facts always come from the Life State Engine.

If the same Life State exists tomorrow, the same question should produce the same factual answer.

---

# Entry Point

All AI interaction enters through `HomeViewModel.sendMessage()`.

There is one AI entry point in the application. The standalone `features/ai` module and `AiChatViewModel` have been removed. See ADR-008.

---

# Full Pipeline

```text
User Message
    ↓
HomeViewModel.sendMessage()
    ↓
RetrievalEngine.retrieve(profileId, userQuery)
    ↓
PromptBuilder.build(context, userQuery)
    ↓
AiProvider.complete(systemPrompt, userMessage, history)
    ↓
parseAiResponse() → AiProposal?
    ↓
User reviews proposal card (in AI_WORKSPACE mode)
    ↓
executeProposal()
    ↓
PlanningEngine / MetadataRepository / ObjectRepository
    ↓
Life State Updated
```

---

# Stage 1 — RetrievalEngine

**Interface:** `domain/engine/RetrievalEngine.kt`

```kotlin
interface RetrievalEngine {
    suspend fun retrieve(profileId: String, userQuery: String): RetrievalContext
}
```

**Implementation:** `data/engine/RetrievalEngineImpl.kt`

**Algorithm:**

The engine scores all objects belonging to the profile using keyword matching against the user's query:

| Field | Score Per Matching Keyword |
|-------|---------------------------|
| Object title | 3.0 |
| Object type | 2.0 |
| Object domain | 1.5 |
| Metadata values | 0.5 |

Objects with a score above zero are ranked descending. The top 5 are selected.

For each selected object, `ObjectReasoner.buildSnapshot()` is called to produce a rich `ObjectSnapshot`.

The engine also fetches:
- All object index entries (title + type + domain) for the low-cost index section of the prompt
- Up to 10 pending tasks for the profile
- Up to 10 upcoming reminders (next 30 days)

The result is a `RetrievalContext` that becomes the sole input to `PromptBuilder`.

---

# Stage 2 — ObjectReasoner

**Interface:** `domain/engine/ObjectReasoner.kt`

```kotlin
interface ObjectReasoner {
    suspend fun buildSnapshot(profileId: String, objectId: String): ObjectSnapshot?
}
```

**Implementation:** `data/engine/ObjectReasonerImpl.kt`

For each object the `RetrievalEngine` selects, `ObjectReasoner`:
1. Fetches all metadata entries for the object
2. Queries the pending task count for the object
3. Queries the document count for the object
4. Parses the `ai_context` metadata field into an `AiObjectContext` (if present)
5. Returns an `ObjectSnapshot`

---

# Domain Models

## RetrievalContext

The complete input to `PromptBuilder`. Contains everything needed to build the system prompt.

```kotlin
data class RetrievalContext(
    val profileId: String,
    val profileName: String,
    val relevantSnapshots: List<ObjectSnapshot>,   // up to 5 rich snapshots
    val totalObjectCount: Int,
    val allObjectIndex: List<ObjectIndexEntry>,    // lightweight index of all objects
    val allObjectMetadata: Map<String, List<MetadataEntry>>,
    val pendingTasks: List<Task>,                  // up to 10
    val upcomingReminders: List<Reminder>          // up to 10, next 30 days
)
```

## ObjectSnapshot

The per-object view passed from `RetrievalEngine` to `PromptBuilder`.

```kotlin
data class ObjectSnapshot(
    val objectId: String,
    val title: String,
    val objectType: String,
    val domain: String,
    val status: String,
    val metadata: List<MetadataEntry>,
    val aiContext: AiObjectContext?,
    val pendingTaskCount: Int,
    val documentCount: Int,
    val relevanceScore: Float
)
```

## AiObjectContext

Structured AI analysis stored per object in the `ai_context` metadata field as JSON. Allows the AI to include prior reasoning without re-analysing the object from scratch each time.

```kotlin
data class AiObjectContext(
    val summary: String,
    val importantFacts: List<String>,
    val currentSituation: String,
    val suggestions: List<String>,
    val lastUpdated: Instant,
    val confidence: Float
)
```

Stored in the `metadata` table with `fieldId = "ai_context"`. Displayed in Object Detail as a distinct "AI Context" card (not in the regular Details list).

---

# Stage 3 — PromptBuilder

**Interface:** `domain/engine/PromptBuilder.kt`

```kotlin
interface PromptBuilder {
    fun build(context: RetrievalContext, userQuery: String): String
}
```

**Implementation:** `data/engine/PromptBuilderImpl.kt`

Pure formatting — no I/O, no coroutine, no database access. Takes a `RetrievalContext` and formats it into a structured system prompt using `buildString { }`.

The prompt includes:
- Profile name and total object count
- Lightweight index of all objects (for broad awareness)
- Full `ObjectSnapshot` detail for the top 5 relevant objects
- Per-object `AiObjectContext` if available
- Pending tasks and upcoming reminders
- Explicit instructions on the 5 supported `AiProposal` action types with JSON examples

---

# Stage 4 — LLM Completion

`AiProvider.complete(systemPrompt, userMessage, history)` is called with the formatted prompt, the user's message, and prior conversation turns.

The LLM produces a natural language response and optionally a structured JSON action block representing an `AiProposal`.

---

# Stage 5 — Response Parsing

`parseAiResponse()` extracts an `AiProposal` from the LLM response if one is present.

## AiProposal

`AiProposal` is a sealed class replacing the legacy `ProposedAction`. It represents every mutation the AI may suggest.

```kotlin
sealed class AiProposal {
    data class MetadataUpdate(
        val proposalId: String, val summary: String,
        val objectId: String, val objectTitle: String,
        val objectType: String, val fields: List<ProposedField>
    ) : AiProposal()

    data class GoalProposal(
        val proposalId: String, val summary: String,
        val title: String, val description: String,
        val deadline: LocalDate?, val estimatedWeeks: Int?,
        val suggestedTasks: List<String>, val linkedObjectId: String?
    ) : AiProposal()

    data class TaskCompletion(
        val proposalId: String, val summary: String,
        val taskId: String, val taskTitle: String, val goalId: String?
    ) : AiProposal()

    data class TaskCreation(
        val proposalId: String, val summary: String,
        val title: String, val description: String,
        val dueDate: LocalDate?, val goalId: String?, val objectId: String?
    ) : AiProposal()

    data class ObjectCreation(
        val proposalId: String, val summary: String,
        val objectType: String, val domain: String,
        val title: String, val initialNotes: String
    ) : AiProposal()
}
```

---

# Stage 6 — User Verification

Proposal cards are rendered in the AI Workspace:

| Proposal Type | Card Component |
|--------------|----------------|
| `MetadataUpdate` | `ActionProposalCard` |
| `GoalProposal` | `GoalProposalCard` |
| `TaskCompletion` | `TaskCompletionCard` |
| `TaskCreation` | `TaskCreationCard` |
| `ObjectCreation` | `ObjectCreationCard` |

All cards are defined in `designsystem/`. The user must explicitly approve or dismiss each proposal. AI suggestions never auto-apply to the Life State.

---

# Stage 7 — Proposal Execution

`executeProposal()` routes approved proposals to the appropriate mutation path:

| Proposal Type | Executed Via |
|--------------|-------------|
| `MetadataUpdate` | `MetadataRepository.upsertMetadata()` |
| `GoalProposal` | `PlanningEngine.createGoal()` |
| `TaskCompletion` | `PlanningEngine.completeTask()` |
| `TaskCreation` | `PlanningEngine.createTask()` |
| `ObjectCreation` | `ObjectRepository.createObject()` |

`PlanningEngine` is the single mutation path for all Planner operations. No ViewModel writes directly to `GoalRepository` or `TaskRepository` for mutations.

---

# Privacy

The retrieval engine only retrieves information belonging to the currently active profile. Cross-profile queries are not supported in Version 1.

---

# Error Handling

- If retrieval returns no scored objects, the prompt includes only the lightweight object index and pending tasks.
- If the LLM response contains no parseable `AiProposal`, only the natural language response is displayed.
- If no AI provider is configured, the `OfflineAiProvider` returns a setup prompt.

---

# Future Compatibility

The pipeline is designed for forward compatibility:

| Future Capability | Extension Point |
|------------------|----------------|
| Semantic / embedding search | Replace `RetrievalEngineImpl` scoring |
| On-device AI | Implement `AiProvider` backed by on-device model |
| Multiple AI providers | Already abstracted behind `AiProvider` |
| Tool calling | Extend `AiProposal` sealed class |
| Multi-modal | Add image context to `ObjectSnapshot` |

---

# Summary

| Stage | Responsibility | Location |
|-------|---------------|----------|
| Entry | `HomeViewModel.sendMessage()` | `features/home` |
| Scoring | `RetrievalEngineImpl` | `data/engine` |
| Snapshot | `ObjectReasonerImpl` | `data/engine` |
| Formatting | `PromptBuilderImpl` | `data/engine` |
| Inference | `AiProvider` | `data/ai/providers` |
| Parsing | `parseAiResponse()` | `features/home` |
| Verification | Proposal cards | `designsystem` |
| Execution | `executeProposal()` | `features/home` |
| Mutation | `PlanningEngine` / repositories | `data` |
