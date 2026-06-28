# ADR-008 — Architecture Stabilization Pass

**Status:** Accepted

**Date:** 2026-06-28

---

## Context

As the LifePilot codebase approached feature completeness for Version 1.0, several architectural weaknesses had accumulated:

1. **Dual AI entry points.** AI interaction existed both in a standalone `features/ai` module (`AiChatViewModel`, `AiChatScreen`) and partially in `HomeViewModel`. This created two context-building code paths, duplicated conversation state management, and split the AI surface area across modules.

2. **Unstructured context building.** The system prompt for the AI was assembled inside `AiChatViewModel` as an ad-hoc string construction — loading up to 30 objects, fetching 5 metadata fields each, and inlining tasks and reminders. This logic was tangled with UI state management and untestable.

3. **AI responses were unstructured.** The AI could propose any text response. There was no typed representation of what the AI was suggesting, making it impossible to safely route proposals to the correct repository.

4. **No single mutation path for goals and tasks.** Feature ViewModels were writing directly to `GoalRepository` and `TaskRepository`, bypassing any coordination layer and making it easy for future code to introduce inconsistent state changes.

5. **Metadata had no verifiability.** AI-extracted metadata had no indicator of whether a human had reviewed and confirmed the value. There was no way to distinguish user-entered facts from AI-proposed values.

---

## Decisions

### 1 — Single AI Entry Point

The `features/ai` module (`AiChatViewModel`, `AiChatScreen`) was removed from `app/build.gradle.kts` and is no longer compiled into the application.

All AI interaction now routes through `HomeViewModel.sendMessage()` inside `features/home`.

The Home screen operates in two modes controlled by `HomeMode`:
- `DAILY_BRIEF` — shows attention items, active goals, recent conversations
- `AI_WORKSPACE` — shows conversation thread and proposal cards

The first message transitions to `AI_WORKSPACE`. `startNewChat()` returns to `DAILY_BRIEF`.

**Rationale:** A single entry point eliminates duplicate context-building, eliminates split conversation state, and makes the AI pipeline testable end-to-end through a single ViewModel.

---

### 2 — RetrievalEngine Extraction

Object scoring and selection logic was extracted from `AiChatViewModel` into a domain interface `RetrievalEngine` with implementation `RetrievalEngineImpl`.

```kotlin
interface RetrievalEngine {
    suspend fun retrieve(profileId: String, userQuery: String): RetrievalContext
}
```

The implementation uses keyword scoring (title=3pt, type=2pt, domain=1.5pt, metadata=0.5pt) and selects a maximum of 5 objects per request.

**Rationale:** The scoring algorithm is now independently testable, swappable without touching UI code, and clearly separated from prompt formatting.

---

### 3 — PromptBuilder Extraction

System prompt construction was extracted from `AiChatViewModel` into a domain interface `PromptBuilder` with a pure implementation `PromptBuilderImpl` that contains no I/O.

```kotlin
interface PromptBuilder {
    fun build(context: RetrievalContext, userQuery: String): String
}
```

**Rationale:** A pure function with no I/O is trivially testable. Prompt structure can be iterated without touching any Android code. The interface boundary also makes it easy to replace the formatting strategy in future.

---

### 4 — ObjectReasoner Introduction

Per-object snapshot assembly (fetching metadata, task count, document count, parsing `AiObjectContext`) was extracted into `ObjectReasoner`:

```kotlin
interface ObjectReasoner {
    suspend fun buildSnapshot(profileId: String, objectId: String): ObjectSnapshot?
}
```

`RetrievalEngine` calls `ObjectReasoner.buildSnapshot()` for each of the top-scored objects.

**Rationale:** Object snapshot assembly is a cohesive unit of work that was previously scattered across multiple `ViewModel` methods. Extracting it makes it composable and testable.

---

### 5 — Structured AiProposal (replaces ProposedAction)

The legacy `ProposedAction` sealed class was replaced with `AiProposal`, which models all five mutation types the AI may suggest:

- `MetadataUpdate` — propose changes to object metadata fields
- `GoalProposal` — suggest creating a new goal with tasks
- `TaskCompletion` — suggest marking an existing task complete
- `TaskCreation` — suggest creating a new task
- `ObjectCreation` — suggest creating a new object

Each variant carries a `proposalId` (for deduplication), a `summary` (shown in the proposal card), and the minimum fields required to execute the proposal if approved.

Proposal cards in `designsystem/` (`GoalProposalCard`, `TaskCompletionCard`, `TaskCreationCard`, `ObjectCreationCard`, `ActionProposalCard`) render each type.

**Rationale:** Typed proposals eliminate the need for string parsing when executing AI suggestions. Each variant can be routed to the correct repository without pattern matching on text. User-facing cards are consistent and schema-driven.

---

### 6 — PlanningEngine as Single Mutation Path

A `PlanningEngine` domain interface was introduced as the single mutation path for all goal and task state changes:

```kotlin
interface PlanningEngine {
    suspend fun createGoal(...): Goal
    suspend fun completeGoal(goalId: String)
    suspend fun archiveGoal(goalId: String)
    suspend fun createTask(...): Task
    suspend fun completeTask(taskId: String)
    suspend fun cancelTask(taskId: String)
}
```

No ViewModel or feature module writes directly to `GoalRepository` or `TaskRepository` for mutations. All writes go through `PlanningEngine`.

**Rationale:** A single mutation path ensures consistent side effects (timeline entries, task generation, reminder evaluation) for every state change. It mirrors the `LifeStateEngine` pattern already established for object and document mutations.

---

### 7 — AiObjectContext and Structured Per-Object AI State

A new `AiObjectContext` model was introduced to store structured AI analysis per object. It is persisted as JSON in the `metadata` table under the reserved `fieldId = "ai_context"`.

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

In Object Detail, `ai_context` is displayed as a distinct `secondaryContainer` card labelled "AI Context". It is never included in the schema-driven Details form.

**Rationale:** Structured per-object AI state allows `ObjectReasoner` to include prior AI analysis in every snapshot without re-generating it each time. It also surfaces the AI's reasoning to the user in a consistent, identifiable location.

---

### 8 — Metadata Provenance

A `verificationStatus` column (`TEXT NOT NULL DEFAULT 'UNVERIFIED'`) was added to the `metadata` table in MIGRATION_2_3.

A `VerificationStatus` enum was introduced in the domain layer: `UNVERIFIED`, `VERIFIED`, `REJECTED`.

Every metadata entry in the Object Detail screen displays a `ProvenanceBadge` showing the source and verification status.

`MetadataRepository` gained `verifyMetadata(metadataId)` and `rejectMetadata(metadataId)` methods, and `upsertMetadata` accepts a `verificationStatus` parameter.

**Rationale:** Users need to know whether a displayed value was entered by them, extracted by OCR, proposed by AI, or confirmed by a human. Without provenance, the Life State cannot be trusted. Provenance is the implementation of the "User Verification" product principle.

---

## Consequences

**Positive:**

- The AI pipeline is now fully testable at every stage (scoring, formatting, parsing, execution) without an Android device.
- There is one place to look when debugging any AI behaviour.
- Metadata values are now trustworthy — the UI shows exactly where each value came from and whether it was verified.
- Goal and task mutations are consistent and auditable.

**Negative / Trade-offs:**

- `features/ai` code is gone. Any developer expecting a standalone AI screen will need to look at `features/home`.
- DB version 3 requires two runtime migrations for existing installs (MIGRATION_1_2, MIGRATION_2_3).

---

## Alternatives Considered

- **Keep `features/ai` and route through it.** Rejected because it would require two ViewModels to share `RetrievalEngine`, creating coupling without a clean ownership boundary.
- **Store `AiObjectContext` in a separate table.** Rejected because it would require a new DAO and migration. Using the existing `metadata` table with a reserved `fieldId` is consistent with the existing vertical metadata design and requires no structural change.
- **Use a `ConversationRepository` for all proposal routing.** Rejected because proposals are fundamentally different from conversation messages. Routing through `PlanningEngine` keeps mutation semantics at the domain layer.
