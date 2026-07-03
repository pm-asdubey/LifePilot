# Architecture Decision Records — LifePilot

## ADR-001: Projects as a First-Class Concept

**Date**: 2026-07-03  
**Status**: Accepted

### Context

Users frequently have multi-domain life initiatives — a Japan trip, a house purchase, a job change — that touch multiple Objects (e.g. Passport, Visa), Tasks (book flight, apply for leave), Goals (save for trip), and Documents (itinerary PDF, insurance). Without a grouping mechanism, these related records are scattered across domains with no single view.

### Decision

Introduce **Projects** as a first-class entity in LifePilot.

A Project is a life initiative that clubs Objects, Tasks, Goals, and Documents under one umbrella. Projects do NOT replace Domain Life States (the AI's per-domain understanding) — they are complementary. Domain Life States represent the AI's understanding of a domain; Projects are user-visible initiative containers.

### Implementation

**Database**  
- New `projects` table (Migration 5):  
  `project_id`, `profile_id`, `title`, `description`, `domain`, `status` (ACTIVE/COMPLETED/ARCHIVED), `created_at`, `updated_at`  
- Nullable `project_id` FK column added to `objects`, `tasks`, `goals` tables via `ALTER TABLE ... ADD COLUMN`.  
- All existing rows default to NULL (no project), ensuring zero-downtime migration.

**Domain layer**  
- `Project.kt` — data class with `ProjectStatus` enum.  
- `ProjectRepository.kt` — interface exposing CRUD, observe flows, link operations, and observe linked items.  
- `AiProposal.ProjectCreation` added to the sealed proposal hierarchy.  
- `RetrievalContext.activeProjects` added (default empty list for backward compatibility).

**Data layer**  
- `ProjectEntity.kt`, `ProjectDao.kt`, `ProjectRepositoryImpl.kt`.  
- `RetrievalEngineImpl` loads active projects and includes them in `RetrievalContext`.  
- `PromptBuilderImpl` surfaces active projects in the AI system prompt and adds action type 8 (`PROJECT_CREATION`) to the prompt.

**AI integration**  
- `HomeViewModel.parseAction` handles `PROJECT_CREATION` JSON blocks.  
- `HomeViewModel.executeProposal` creates the project and links any object IDs returned by the AI.

**UI**  
- Library screen gains a **Projects** tab alongside the existing Records tab.  
- `ProjectDetailScreen` shows linked Objects, Tasks, and Goals with tap-through navigation.  
- `LibraryNavigation` adds a `project/{projectId}` route.

### Design Principles Preserved

- **Domain Life States are unchanged.** Projects do not affect the AI's domain understanding architecture.  
- **Offline-first.** All project data lives in Room; no network required.  
- **User owns their data.** Projects are soft containers — deleting a project does not delete the linked records.  
- **AI proposes, user approves.** `ProjectCreation` follows the same proposal → approval → execution pipeline as all other proposals.  
- **Clean Architecture.** UI → ViewModel → Repository → DAO — no layers skipped.

### Consequences

- Migration 5 is additive (no columns dropped, no NOT NULL without defaults). Safe for existing users.  
- `project_id` columns are nullable on all linked tables; existing queries are unaffected.  
- A future sync layer can treat projects as first-class sync units.  
- The `Project` domain model intentionally does NOT embed linked items — they are queried reactively via the repository to avoid N+1 loading.

---

## ADR-002: Projects Placement, UX Flow, and Data Storage Policies

**Date**: 2026-07-03  
**Status**: Accepted — supersedes the UI section of ADR-001

### What Changed from ADR-001

ADR-001 incorrectly placed Projects inside the Library screen. This decision corrects that and documents the full product intent derived from the mockup (`LifePilot-projects-mockup.html`).

---

### Where Projects Live

**Planner screen** owns Projects. The Planner has two tabs: **Tasks** and **Projects**.  
Projects replace Goals as the primary "what am I working toward" concept in the UX.  
**Library** remains exclusively the source of truth for Objects (Records). No Projects tab in Library.

The Goals table is NOT deleted from the database — data is preserved. Goals are simply no longer surfaced as a primary UX concept. They may be repurposed as milestone targets inside Projects in a future iteration.

---

### Project Workspace (per-project screen)

Opening a project navigates to a workspace with these tabs (in order):

| Tab | Contents |
|---|---|
| Overview | Stat row (open tasks, documents, days left), Life State chips (from domain), AI Insight card, Next tasks preview |
| Tasks | Open tasks (with checkboxes) + Completed tasks (faded) |
| Documents | Linked Records section (objects) + Documents section (raw files) |
| AI | Scoped chat — AI retrieval narrowed to this project's records, tasks, documents, and domain life state only |

**Timeline tab is deferred** — not included in MVP.

---

### AI Interaction Model for Projects

The AI does NOT propose a project immediately when the user mentions an initiative. The flow is:

1. **Discuss first.** The AI asks clarifying questions and discusses the scope of the initiative with the user over one or more turns.
2. **Plan together.** Once the AI understands the initiative, it proposes a concrete plan in conversation (tasks, linked records, target date).
3. **User confirms the plan.** User says yes, or refines.
4. **Batch approval UI.** Only then does the AI emit a proposal block containing all of:
   - `PROJECT_CREATION` — the project itself
   - `OBJECT_CREATION` entries — for any new records needed (only for sensitive ones; routine ones can be auto-created)
   - `TASK_CREATION` entries — tasks scoped to the project
   - Domain life state update — AI's updated understanding of the relevant domain
5. User approves the batch → everything is created in one transaction.

This preserves the "AI proposes, user approves" invariant without creating a fragmented experience of individual proposal cards.

---

### Emoji Assignment

| Input method | Emoji behaviour |
|---|---|
| AI-proposed project | Auto-assigned from domain→emoji map. Stored in the `PROJECT_CREATION` JSON. User can change it after creation. |
| User-created project (manual, via Planner FAB) | No pre-selection. User must pick from the emoji grid in the Create Project sheet. |

**Domain → Emoji defaults:**

| Domain | Emoji |
|---|---|
| Travel | ✈️ |
| Career | 💼 |
| Property | 🏠 |
| Education | 🎓 |
| Health | 🏥 |
| Finance | 💰 |
| Legal | ⚖️ |
| General / unknown | 🎯 |

---

### Data Storage Policies

**Stored in the database:**
- `projects` table: `project_id`, `profile_id`, `title`, `description`, `domain`, `emoji`, `target_date` (nullable Long epoch ms), `status` (ACTIVE/COMPLETED/ARCHIVED), `is_ai_proposed` (Boolean — true until user approves), `created_at`, `updated_at`
- Links: `project_id` FK (nullable) on `objects`, `tasks`. `project_id` FK (nullable) on `documents` — added in Migration 6.
- `is_ai_proposed = true` records are held in the proposal flow (not persisted) until the user approves. Upon approval all linked records are written atomically.

**Computed at runtime, never stored:**
- Progress % = `completedTasks / totalTasks`. Always derived from task state.
- Status chip (On track / Behind / Blocked) = derived: any overdue task → Behind; target date ≤ 7 days away with open urgent tasks → Blocked; otherwise On track.
- Life State chips on Overview = pulled live from `DomainLifeState` for the project's domain — not duplicated on the project record.

**Scoped AI context for project AI tab:**
- `RetrievalContext` is built with only the project's linked objects, tasks, documents, and the single relevant domain life state.
- The global object index is still passed for action-parsing resolution but the AI prompt only surfaces project-scoped records.

---

### Three Canonical User Journeys

#### Journey 1 — Conversation → Project (e.g. Japan trip)

1. User: "I'm planning a Japan trip in August."
2. AI retrieves Travel domain life state. Finds passport (valid). Discusses: asks about dates, who's travelling, what visa is needed.
3. Over 1–2 turns, AI and user agree on scope.
4. AI proposes: PROJECT_CREATION `{emoji: "✈️", title: "Japan Trip 2026", targetDate: "2026-08-12"}` + tasks ["Apply for Japan visa", "Buy travel insurance", "Book hotel", "Buy JR Pass"].
5. User approves → project + tasks created atomically.
6. Travel domain life state updated: "Active project: Japan Trip 2026 targeting Aug 2026. Visa pending."
7. Later: user scans flight PDF → AI asks "Link to Japan Trip 2026?" → document linked to project.
8. Project Overview shows: Life State chips (✓ Passport valid, ⚠ Travel insurance, ✗ JR Pass) + AI Insight card.

#### Journey 2 — Document → Object → Project (e.g. job offer)

1. User attaches offer letter PDF in Home chat.
2. OCR runs during send. AI reads OCR text + user's typed message together.
3. AI responds: "This is a job offer from Acme Corp for Senior PM, starting Sep 1. Should I save this as a record?"
4. User: "Yes."
5. AI proposes: OBJECT_CREATION `{type: "Job", title: "Senior PM at Acme"}` with extracted fields.
6. User approves → Job object created, offer letter stored under it.
7. AI continues in the same thread: "This looks like a major life change. Want me to set up a project to track your transition?" Discusses what tasks are needed.
8. User agrees → AI proposes PROJECT_CREATION `{emoji: "💼", title: "Senior PM at Acme — Transition"}` + tasks + links Job object.
9. Career domain life state updated: "Transitioning to Senior PM at Acme. Start date Sep 1. Notice period ongoing."

#### Journey 3 — Manual project creation → AI assists inside workspace

1. User taps Planner → Projects → FAB → Create Project sheet.
2. User picks emoji manually (no pre-selection), types "Buy First House", sets target date. Creates.
3. Empty project. User opens it → Tasks tab empty → taps "Ask AI" (opens project AI tab).
4. Scoped AI: sees project title, Finance/Property domain life state, no linked records yet. Suggests tasks.
5. User adds tasks. Goes back to Home → asks "what documents do I need for a home loan?" → AI sees active project "Buy First House" in global retrieval context → answers specifically.
6. User uploads property/loan documents → AI asks "Link to Buy First House?" → documents accumulate.
7. Finance + Property domain life states updated as loan is sanctioned.

---

### Consequences

- Migration 6 required: add `emoji TEXT`, `target_date INTEGER`, `is_ai_proposed INTEGER` to `projects` table; add `project_id` FK to `documents` table.
- The "Scan Document" / "Camera" / "Photos" / "Files" attachment flow is also redesigned in ADR-003.
- Goals are removed from Planner UX but data is preserved. No migration needed for removal.

---

## ADR-003: ACTION_PLAN Execution — Project + Task + Document Combined Flow

**Date**: 2026-07-03  
**Status**: Accepted

### Context

After ADR-002 defined the product intent for Projects, the execution layer needed to wire everything together. When the AI proposes an action plan for a complex life event (tax filing, job change, trip, marriage), the system must atomically create the Project, link all Tasks to it, and link all Records/Documents to it — so that the Project Workspace immediately shows a complete, coherent view without any manual linking step from the user.

---

### Decision

#### 1. ACTION_PLAN now includes a `CREATE_PROJECT` item type

Every `ACTION_PLAN` block that has 2 or more tasks MUST include a `CREATE_PROJECT` item as `itemId: "0"`. All `CREATE_TASK` and `CREATE_RECORD` items reference it via `"projectItemId": "0"`.

Example structure emitted by AI:
```json
{
  "actionType": "ACTION_PLAN",
  "planType": "CUSTOM",
  "summary": "Tax filing for FY 2025-26",
  "items": [
    {"type": "CREATE_PROJECT", "itemId": "0", "title": "Tax Filing 2025-26", "emoji": "📋", "domain": "Finance", "dependsOn": []},
    {"type": "CREATE_TASK",   "itemId": "1", "title": "Gather Form 16",           "dueDate": "2026-07-10", "priority": "HIGH", "projectItemId": "0", "dependsOn": []},
    {"type": "CREATE_TASK",   "itemId": "2", "title": "Gather capital gains info", "dueDate": "2026-07-15", "priority": "HIGH", "projectItemId": "0", "dependsOn": []},
    {"type": "CREATE_RECORD", "itemId": "3", "title": "Tax Return 2025-26", "objectType": "tax", "domain": "Finance", "projectItemId": "0", "dependsOn": []},
    {"type": "UPDATE_DOMAIN_UNDERSTANDING", "itemId": "4", "domain": "Finance", "dependsOn": ["0"]}
  ]
}
```

#### 2. Execution order is topologically sorted

`ActionPlanExecutorImpl` runs items in dependency order. `CREATE_PROJECT` (no deps) always runs first, producing a real `projectId` before any tasks or records are created. A `createdProjectIds` map keyed by `itemId` carries the created `projectId` through the execution loop.

#### 3. Tasks are linked to the project at two levels

When a `CREATE_TASK` item has `projectItemId`:
1. `planningEngine.createTask(... projectId = resolvedProjectId ...)` — writes `project_id` into the `tasks` row directly.
2. `projectRepository.linkTask(projectId, taskId)` — redundant `UPDATE tasks SET project_id = ?` via `ProjectDao`, ensuring `observeTasksForProject` DAO query always returns the task.

Both are done so that even if one path has a timing issue, the other ensures the link.

#### 4. Records stay in Library; they are only referenced by Projects

When a `CREATE_RECORD` item has `projectItemId`:
1. `objectRepository.createObject(...)` — creates the LifeObject in the `objects` table. This is the **source of truth**. The record appears in Library.
2. `projectRepository.linkObject(projectId, objectId)` — sets `project_id` on the object row. This makes the record appear in the Project Workspace Documents tab.

Deleting a project does NOT delete linked records. Records outlive projects.

#### 5. Document linking policy (summary)

| Where stored | Where visible |
|---|---|
| `objects` table in Room | Library → Records (always) |
| `project_id` FK on the object row | Project Workspace → Documents tab (when linked) |
| Physical file on disk | `/files/` directory, never moved or duplicated |

Tapping a record in the Project Workspace Documents tab navigates to the full Library record detail screen — not a project-specific copy.

---

### 3-Turn AI Conversation Flow for Complex Events

All complex life events follow this exact 3-turn structure enforced in `PromptBuilderImpl`:

| Turn | AI behaviour | Action block? |
|---|---|---|
| **TURN 1 — CLARIFY** | React warmly. Ask ALL factual questions (dates, amounts, income types, documents) in ONE `[ASK]` block. Also ask if relevant documents are available to scan/upload. | None (unless a document is pending — then OBJECT_CREATION fires here too) |
| **TURN 2 — PROPOSE** | Include 1-2 sentences of advisory guidance (e.g. "For your income profile you'll need ITR-2"). Then list every proposed task with title, due date, and priority as plain text. End with: "Would you like to adjust anything before I create these?" | **None** — the user MUST see the plan before any task is created |
| **TURN 3 — COMMIT** | User confirms → emit `ACTION_PLAN` immediately. No narration. Just the action block. If user requests changes → revise task list as plain text, loop back to TURN 2. | `[LIFEPILOT_ACTION]` ACTION_PLAN |

**Critical invariant:** A "yes please" or "yes" given while answering the `[ASK]` clarifying questions in TURN 1 is NOT approval to create tasks. The user has not seen the specific tasks and dates yet. The AI MUST still complete TURN 2 before committing.

**`[ASK]` block rules:**
- Contains ONLY factual questions (dates, names, amounts, income types).
- NEVER contains permission questions ("shall I create tasks?", "would you like a plan?").
- Permission to create is only sought via TURN 2's closing question after the full plan is shown.

---

### Document Golden Rule (enforced in PromptBuilderImpl)

If the context includes a document with `isPendingApproval = true`:
- A `[LIFEPILOT_ACTION] OBJECT_CREATION` block MUST appear in the **same response** where the document is first seen.
- This fires at **any turn** — TURN 1, TURN 2, or TURN 3.
- It combines freely with `[ASK]` blocks, task proposals, and `ACTION_PLAN` blocks in the same response.
- The document save step is never deferred to a later turn, regardless of what query the user asked.

**Document + planning in same response (TURN 1 example):**
```
[LIFEPILOT_ACTION] { "actionType": "OBJECT_CREATION", "objectType": "PanCard", ... } [/LIFEPILOT_ACTION]

Your PAN card is saved to Library. A few questions to set up your tax filing plan: [ASK]1. Which financial year? 2. Do you have stock market or rental income? 3. Do you have Form 16 or TDS certificates to upload?[/ASK]
```

---

### Project Workspace Data Flow

```
ActionPlanExecutorImpl (on user approval)
    CREATE_PROJECT  →  projectRepository.createProject()  →  projectId stored
    CREATE_RECORD   →  objectRepository.createObject()    →  saved to Library
                    →  projectRepository.linkObject()      →  linked to project
    CREATE_TASK     →  planningEngine.createTask(projectId=...)  →  saved with FK
                    →  projectRepository.linkTask()        →  double-linked

ProjectWorkspaceViewModel (reactive, always up to date)
    observeObjectsForProject(projectId)  →  Documents tab
    observeTasksForProject(projectId)    →  Tasks tab + Overview next-tasks
    getProject(projectId)                →  Header (emoji, title, progress%)

ProjectWorkspaceScreen tabs:
    Overview   → Stats (open tasks · documents · days left) + Next 3 tasks
    Tasks      → Open · N (with due dates, priority) / Completed · N (faded)
    Documents  → Linked Records · N  (tap → Library record detail)
    AI         → Scoped context banner (project-scoped chat, future)
```

---

### Consequences

- `ActionItem` sealed class gained `CreateProject` and `projectItemId: String?` on `CreateTask` and `CreateRecord`. All existing `when(item)` exhaustive expressions updated.
- `PlanningEngine.createTask` gained `projectId: String? = null` with a default — zero breaking changes to existing callers.
- `ActionPlanExecutorImpl` now injects `ProjectRepository` (Hilt singleton, no circular dependency).
- The prompt's ACTION_PLAN example was updated to always show `CREATE_PROJECT` as item `"0"` and `"projectItemId": "0"` on all tasks and records. The AI is instructed this is mandatory for any plan with ≥ 2 tasks.
- Progress % and status chip remain computed at runtime from task state — never stored on the project row.
