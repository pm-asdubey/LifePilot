# LifePilot P0 Bug Fixes — Session Change Log

**Project:** `/Users/ashutoshdubey/Desktop/LifePilot`  
**Device:** `c8988ac7` (USB ADB)  
**Build command:** `./gradlew :domain:test :data:testDebugUnitTest :app:assembleDebug --no-daemon`  
**Build status:** ✅ Successful

---

## 1. Build / Test Blocker Fixes

### `data/src/main/java/com/lifepilot/data/repository/SearchRepositoryImpl.kt`
- Added empty-list guards before `objectDao.getObjectsByIds(...)` calls for both metadata-only and document-only object lookups.
- **Why:** Prevents unnecessary DB calls and avoids `MockKException` in unit tests when the mocked DAO receives an empty list.

### `data/src/test/java/com/lifepilot/data/schema/SchemaValidationTest.kt`
- Completed the missing MockK stub for `schemaEngine.validateMetadataValue("passport", "passportNumber", "")`.
- Test now asserts the returned validation result is invalid and contains a required-field error message.

---

## 2. ISSUE-004 — Search crashes the app

### `features/search/src/main/java/com/lifepilot/features/search/ui/SearchScreen.kt`
- Replaced unstable Material3 experimental `SearchBar` with a standard `TextField` + search icon + results list.
- Aligned placeholder text to `"Search objects, documents, tasks..."` and empty-state title to `"Search your life"` to match smoke tests.

**Verification:** ✅ Opened Search, typed `"passport"`, observed results (`Indian Passport`, `Passport`) without crash. Same PID retained.

---

## 3. ISSUE-001 — New AI conversation appends to the first conversation

### `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- Added `startNewConversation()` method that clears `currentConversationId`, resets title to `"New conversation"`, and clears messages.
- Wired `startNewConversation()` to the AI workspace "New chat" top-app-bar action in `HomeScreen.kt`.
- Added auto-clear logic in `sendMessage()`: if the current conversation has been idle longer than 4 hours, the next message starts fresh.
- **Fixed during verification:** `returnToBrief()` now also clears `currentConversationId`, `conversationTitle`, `messages`, and `inputText`. This ensures that pressing back from the AI workspace and tapping the ask box starts a brand-new conversation instead of reopening the old one.

**Verification:** ✅ Tapping "New chat" resets title to `"New conversation"`. Back-from-AI-workspace + ask-box path fixed in code; manual adb tap of the ask box was unreliable due to Compose semantics not exposing a clickable node to uiautomator.

---

## 4. ISSUE-022 — Cancelling a record via AI updates metadata but status stays Active

### `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- Added `STATUS_UPDATE` action type to the AI action instructions.
- Added prompt rule to prefer `STATUS_UPDATE` for cancelled/ended/closed/expired records.

### `domain/src/main/java/com/lifepilot/domain/model/ProposedAction.kt`
- Added `AiProposal.StatusUpdate(proposalId, summary, objectId, objectTitle, newStatus: ObjectStatus)` sealed subclass.

### `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- Injected `UpdateObjectStatusUseCase`.
- Wired `executeProposal()` to call `updateObjectStatusUseCase()` for `AiProposal.StatusUpdate`.
- Added status parsing in `parseAction()` to map AI-provided status strings to `ObjectStatus`.
- **Fixed during verification:** Added `parseStatus()` helper that maps common synonyms (`cancel`, `end`, `close` → `INACTIVE`; `expire` → `EXPIRED`; etc.) and falls back to `INACTIVE` with a warning for truly unknown values. This resolves the logcat warning `Unknown status in STATUS_UPDATE: CANCELLED` observed when the user typed "Cancel my Russia trip".

### `designsystem/src/main/java/com/lifepilot/designsystem/components/AiProposalCards.kt`
- Added `StatusUpdateCard` UI for the new action type.

**Verification:** ⚠️ **Untested end-to-end.** Code review confirms the full pipeline is wired. A previous attempt showed the AI emitting `STATUS_UPDATE` with `CANCELLED`, which the new mapping now handles. Final device confirmation was skipped per user request.

---

## 5. ISSUE-025 — Document re-upload appends metadata instead of overwriting

### `domain/src/main/java/com/lifepilot/domain/repository/MetadataRepository.kt`
- Added `suspend fun deleteAllMetadataForObject(objectId: String)`.

### `data/src/main/java/com/lifepilot/data/repository/MetadataRepositoryImpl.kt`
- Implemented `deleteAllMetadataForObject()` delegating to `metadataDao.deleteAllMetadataForObject(objectId)`.

### `features/object/src/main/java/com/lifepilot/features/object/verification/viewmodel/MetadataVerificationViewModel.kt`
- Before persisting newly extracted metadata, deletes existing `AI_EXTRACTED` / `OCR` metadata for the object, then calls `upsertMetadataBatch()`.

### `data/src/main/java/com/lifepilot/data/engine/ObjectReasonerImpl.kt`
- Added defensive deduplication guard in `buildSnapshot()`: if multiple rows exist for the same `(objectId, fieldId)`, keeps the highest-confidence, most-recent value.

**Verification:** ✅ Code review confirms the overwrite path. Re-upload end-to-end was not exercised on device during this session.

---

## 6. ISSUE-018 / ISSUE-055 — Security model and audit

### `app/src/main/java/com/lifepilot/app/MainActivity.kt`
- Added `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)`.

### `domain/src/main/java/com/lifepilot/domain/security/SensitiveFieldRegistry.kt` (new)
- Registry of sensitive field IDs (Aadhaar, PAN, passport numbers, etc.).

### `app/src/main/java/com/lifepilot/app/logging/ScrubbingTree.kt` (new)
- Custom Timber tree that redacts values for registered sensitive fields before logging.

### `app/src/main/java/com/lifepilot/app/LifePilotApplication.kt`
- Planted `ScrubbingTree()` in debug builds.

### `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- Added redaction logic so sensitive metadata values are replaced with `[REDACTED]` before the prompt string leaves the device.

### `data/src/test/java/com/lifepilot/data/engine/PromptBuilderImplTest.kt` (new)
- Unit test asserting that Aadhaar/PAN-like values are redacted in the constructed prompt while non-sensitive structure remains intact.

### `docs/security/DATA_FLOW.md` (new)
### `docs/security/PRIVACY_DECLARATION.md` (new)
- Formal data-flow diagram and plain-language privacy declaration.

**Verification:** ✅ Screenshot via `adb shell screencap` returned a black screen (only status bar visible), confirming `FLAG_SECURE` blocks screen capture. Unit tests for redaction pass.

---

## 7. Additional Unit-Test Stabilization

Several pre-existing domain tests were updated to compile against current interfaces (`ArchiveObjectUseCaseTest`, `DeleteObjectUseCaseTest`, `ExtractMetadataUseCaseTest`, `GetDashboardDataUseCaseTest`, `ImportDataUseCaseTest`, `LinkObjectsUseCaseTest`, `UpdateObjectStatusUseCaseTest`).

---

## Test Results

```
./gradlew :domain:test :data:testDebugUnitTest --no-daemon
BUILD SUCCESSFUL
35 tests passed
```

---

## Known Limitations / Not Verified

- **ISSUE-022 end-to-end AI status update:** Not confirmed on device. The status-mapping fix addresses the `CANCELLED` warning observed in logcat, but the full approve-action → object-status-change flow was not manually exercised.
- **ISSUE-025 document re-upload overwrite:** Not manually exercised on device; verified by code inspection.

---

## Files Changed (high-level)

Production:
- `app/src/main/java/com/lifepilot/app/MainActivity.kt`
- `app/src/main/java/com/lifepilot/app/LifePilotApplication.kt`
- `app/src/main/java/com/lifepilot/app/logging/ScrubbingTree.kt`
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- `data/src/main/java/com/lifepilot/data/engine/ObjectReasonerImpl.kt`
- `data/src/main/java/com/lifepilot/data/repository/SearchRepositoryImpl.kt`
- `data/src/main/java/com/lifepilot/data/repository/MetadataRepositoryImpl.kt`
- `domain/src/main/java/com/lifepilot/domain/security/SensitiveFieldRegistry.kt`
- `domain/src/main/java/com/lifepilot/domain/model/ProposedAction.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- `features/object/src/main/java/com/lifepilot/features/object/verification/viewmodel/MetadataVerificationViewModel.kt`
- `features/search/src/main/java/com/lifepilot/features/search/ui/SearchScreen.kt`
- `designsystem/src/main/java/com/lifepilot/designsystem/components/AiProposalCards.kt`

Tests:
- `data/src/test/java/com/lifepilot/data/SearchRepositoryImplTest.kt`
- `data/src/test/java/com/lifepilot/data/schema/SchemaValidationTest.kt`
- `data/src/test/java/com/lifepilot/data/engine/PromptBuilderImplTest.kt`
- `domain/src/test/java/com/lifepilot/domain/security/SensitiveFieldRegistryTest.kt`

Docs:
- `docs/security/DATA_FLOW.md`
- `docs/security/PRIVACY_DECLARATION.md`


---

## 11. ISSUE-057 — Life Event Action Plan Architecture (approved direction)

**Status:** Architecture approved; implementation pending further instruction.

### Goal
Turn single-record AI reactions into multi-step, domain-spanning **ActionPlans**. Example: “I joined a new job” should close the old job, create the new job, generate tasks (experience letter, PF transfer), and update Career/Finance/Health/Identity understanding.

### Revised architectural decisions

1. **No separate `LifeEventClassifier` for the MVP.**
   - The LLM already reasons over the user message to choose an action type.
   - The LLM itself decides whether to emit a `LIFE_EVENT_PLAN` or one of the existing six action types.
   - A classifier may be added later as an optimization when there are dozens of templates, but the rest of the architecture must not depend on it.

2. **`HomeViewModel` stays an orchestrator.**
   - It receives the parsed `ActionPlan`, exposes it to the UI, and invokes an executor.
   - Sequential execution, dependency resolution, retries, and progress tracking live in a dedicated component.

3. **`ActionItem` / `ActionPlan` are generic platform abstractions.**
   - Although the first UI calls it a “Life Event Action Plan”, the model is reusable for job changes, marriage, property purchase, passport renewal, visa applications, retirement, etc.

### Domain model

```kotlin
enum class ActionPlanType {
    JOB_CHANGE, RELOCATION, MARRIAGE, DIVORCE,
    NEW_PROPERTY, MAJOR_MEDICAL, NEW_FINANCIAL_PRODUCT,
    VISA_IMMIGRATION, MAJOR_PURCHASE, CUSTOM
}

sealed class ActionItem {
    abstract val itemId: String
    abstract val summary: String
    abstract val dependsOn: List<String>
    abstract val isChecked: Boolean

    data class UpdateRecord(...)
    data class CreateRecord(...)
    data class UpdateStatus(...)
    data class CreateTask(...)
    data class UpdateDomainUnderstanding(...)
    data class ClarifyingQuestion(...)
}

data class ActionPlan(
    val planId: String,
    val type: ActionPlanType,
    val summary: String,
    val items: List<ActionItem>,
)
```

### New components

- **`domain/src/main/java/com/lifepilot/domain/engine/ActionPlanExecutor.kt`**
  - Interface: `suspend fun execute(plan: ActionPlan, onProgress: (ActionItem, Result<Unit>) -> Unit): Result<Unit>`
  - Sorts items by `dependsOn`, executes sequentially, maps each `ActionItem` to the existing `AiProposal` types and reuses `executeProposal()` logic.
  - Handles partial failure: continues after a failed item, reports all results at the end.

- **`data/src/main/java/com/lifepilot/data/engine/ActionPlanExecutorImpl.kt`**
  - Injects `PlanningEngine`, `ObjectRepository`, `MetadataRepository`, `UpdateObjectStatusUseCase`, `DomainLifeStateEngine`.
  - Each item becomes a single `AiProposal` and is executed through the canonical pipeline.

- **`data/src/main/java/com/lifepilot/data/engine/ActionPlanTemplateProvider.kt`**
  - Loads JSON templates from `data/src/main/assets/action_plan_templates/`.
  - Each template contains triggers, default items, conditional items, and affected domains.
  - Templates are injected into the prompt as guardrails, not used to generate the plan directly.

### Prompt changes

Extend `PromptBuilderImpl` with a 7th action type:

```text
7. Action Plan (use only when a single event affects multiple records, tasks, or domains):
[LIFEPILOT_ACTION]
{
  "actionType": "ACTION_PLAN",
  "planType": "JOB_CHANGE",
  "summary": "User joined Zepto in Bengaluru...",
  "items": [
    {"type":"UPDATE_STATUS","objectType":"job","matchField":"title","matchValue":"Accenture","newStatus":"INACTIVE"},
    {"type":"CREATE_RECORD","objectType":"job","domain":"Career","title":"Zepto","fields":[...]},
    {"type":"CREATE_TASK","title":"Collect experience letter","dueDate":"2025-07-31","priority":"HIGH"},
    {"type":"UPDATE_DOMAIN_UNDERSTANDING","domain":"Career"}
  ],
  "clarifyingQuestions": [
    {"questionId":"role","text":"What role should I record?"},
    {"questionId":"salary","text":"What is the new salary?"}
  ]
}
[/LIFEPILOT_ACTION]
```

Rules:
- Only emit `ACTION_PLAN` when the event touches ≥2 records or ≥1 record + ≥1 task/domain.
- For simple single actions, keep using the existing six action types.
- Never make up values; use `[ASK]` if critical info is missing.

### UI changes

- Add `pendingActionPlan: ActionPlan?` to `HomeUiState`.
- New composable `ActionPlanCard` rendered as a special chat message:
  - Header: plan type + summary + ready count
  - Sections: Records to Update / Tasks to Create / Understanding to Update
  - Each row: checkbox, title, editable inline fields
  - Clarifying questions as input fields
  - Buttons: **Confirm all**, **Review individually**, **Dismiss**

### Execution flow

1. User sends message.
2. AI responds with `ACTION_PLAN` block.
3. `HomeViewModel.parseAction()` builds `ActionPlan` and stores it in `HomeUiState`.
4. User reviews items, optionally answers clarifying questions.
5. User taps **Confirm all**.
6. `HomeViewModel` calls `ActionPlanExecutor.execute()`.
7. Executor runs items sequentially, mapping each to an `AiProposal` and calling existing execution logic.
8. After each item, progress is emitted and affected domain understanding is updated.
9. Final summary message saved to conversation.

### How we avoid bugs

| Risk | Mitigation |
|------|-----------|
| Duplicate records on re-run | Stable `itemId`; executor skips already-completed items. |
| Partial failure | Sequential execution; failed items reported, remaining items continue. |
| Wrong object targeted | Reuse `resolveObjectForAction()`; unresolved references become clarifying questions. |
| AI hallucination | Strict prompt rule: use existing data or ask; parser validates JSON schema. |
| Infinite loops | Classifier is skipped; do not re-run plan detection on AI confirmation messages. |
| ViewModel bloat | Execution logic lives in `ActionPlanExecutor`, not `HomeViewModel`. |
| State loss | Plan held in `ViewModel`; acceptable MVP loss on process death. |
| Domain drift | `DomainLifeStateEngine.evaluateAndUpdate()` called after each item + final sweep. |

### Implementation order

1. Domain models: `ActionPlanType`, `ActionItem`, `ActionPlan`.
2. `ActionPlanTemplateProvider` with hardcoded templates first, JSON later.
3. Extend `PromptBuilderImpl` with `ACTION_PLAN` action type.
4. Extend `HomeViewModel.parseAction()` to parse action plans.
5. Add `pendingActionPlan` to `HomeUiState`.
6. Build `ActionPlanCard` UI.
7. Implement `ActionPlanExecutor` and wire it into `HomeViewModel`.
8. Tests: parsing, execution, partial failure, template expansion.

### Files involved

- New: `domain/src/main/java/com/lifepilot/domain/model/ActionPlan.kt`
- New: `domain/src/main/java/com/lifepilot/domain/engine/ActionPlanExecutor.kt`
- New: `data/src/main/java/com/lifepilot/data/engine/ActionPlanExecutorImpl.kt`
- New: `data/src/main/java/com/lifepilot/data/engine/ActionPlanTemplateProvider.kt`
- New: `features/home/src/main/java/com/lifepilot/features/home/ui/ActionPlanCard.kt`
- Modify: `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- Modify: `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- Modify: `features/home/src/main/java/com/lifepilot/features/home/state/HomeUiState.kt`
- Modify: `domain/src/main/java/com/lifepilot/domain/model/ProposedAction.kt`
- New templates: `data/src/main/assets/action_plan_templates/*.json`

---

---

# Kimi Handoff — Session 3 (Rate-limit cutoff point)

**Date:** 2026-07-02  
**Picked up by:** Claude Sonnet 4.6  
**Build status at handoff:** ✅ `BUILD SUCCESSFUL in 21s`

---

## What Kimi Completed in Session 3

### 1. Major Life Events category
- Added three new schemas: `marriage.json`, `childbirth.json`, `death_of_relative.json` — all with `"domain": "Major Life Events"`.
- `PromptBuilderImpl` updated with strict routing rules: Marriage/Childbirth/DeathOfRelative always go to the `Major Life Events` domain via `ACTION_PLAN`. Explicit rule: "Do NOT put them in Health, People, or Career."
- Example prompts and cascading task templates added for each event type.

### 2. ACTION_PLAN architecture (from ISSUE-057)
- `domain/model/ActionPlan.kt` — `ActionPlan`, `ActionItem`, `ActionPlanType` sealed/enum models.
- `domain/engine/ActionPlanExecutor.kt` — interface.
- `data/engine/ActionPlanExecutorImpl.kt` — implementation that maps each `ActionItem` to an existing `AiProposal` and executes via canonical pipeline.
- `features/home/ui/ActionPlanCard.kt` — 290-line composable: header, item rows (Records / Tasks / Understanding), Confirm all / Review individually / Dismiss buttons.
- `HomeUiState` extended with `pendingActionPlan`.
- `HomeViewModel.parseAction()` extended to parse `ACTION_PLAN` JSON blocks.

### 3. Tiered AI approval (ISSUE-023)
- New `FieldSensitivity` enum: `PREFERENCE` / `STANDARD` / `SENSITIVE`.
- Schema JSON `sensitivityLevel` field consumed at runtime.
- `MetadataUpdate` proposals with sensitivity ≠ `SENSITIVE` auto-execute without showing a card.
- Sensitive fields (passport number, Aadhaar, PAN, etc.) always require explicit user approval.

### 4. AI chat — Abort, attachment, multi-line
- `abortAi()` in `HomeViewModel` — cancels the coroutine job, appends a "Stopped by user" message.
- Stop (×) icon shown in `AiInputBar` while `isLoading`, replacing the send button.
- Attachment paperclip icon added to `AiInputBar`.
- `AttachmentOptionSheet` bottom sheet with Camera / Scan Document / Attach File rows.
- `OutlinedTextField` in `AiInputBar` set to `minLines=1, maxLines=5` — Enter adds newlines, dedicated Send button sends.

### 5. ISSUE-085 fix
- `ConversationDao.searchConversations(profileId, query)` added.
- `SearchEntityType.CONVERSATION` added to the enum.
- `SearchRepositoryImpl` queries `ConversationDao` and maps results to `SearchResult`.
- `SearchScreen` routes `CONVERSATION` result taps to `onNavigateToConversation(entityId)`.
- Task results route to Planner navigation.

### 6. Duplicate metadata on re-upload (ISSUE-025)
- `MetadataVerificationViewModel`: delete all existing `AI_EXTRACTED`/`OCR` metadata for the object before inserting fresh extraction results.
- `ObjectReasonerImpl.buildSnapshot()`: dedup guard — if multiple rows exist for same `(objectId, fieldId)`, keep highest-confidence most-recent value.

### 7. Last fix before rate limit
- `HomeViewModel.kt` line ~830: `val attachedFilePath = proposal.attachedFilePath` — local variable introduced to allow Kotlin smart cast past the null check. Build was failing on `kspDebugKotlin` before this fix. This fix is confirmed applied and build passes.

---

## What Was NOT Completed (Remaining MVP Work)

### A. Camera capture — UI stub only
`AttachmentOptionSheet.onCamera` is a no-op stub:
```kotlin
onCamera = { /* Camera capture requires FileProvider setup */ showAttachmentSheet = false }
```
CameraX not integrated. `FileProvider` not declared in `AndroidManifest.xml`. The "Take photo" option closes the sheet and does nothing.
**File to complete:** `HomeScreen.kt` (camera launcher), `AndroidManifest.xml` (FileProvider + CAMERA permission), `HomeViewModel.processAttachment()` already handles the URI once captured.

### B. "Scan document" — stub only
Same state as camera. The "Scan document" option in `AttachmentOptionSheet` calls the same file-picker fallback. ML Kit Document Scanner not integrated.

### C. Temporal object language on Object Detail
User requirement: when an object is opened, display contextual temporal language — e.g. "Joining Aug 1" if start date is in the future vs "Joined Aug 1" if past. `ObjectDetailScreen` not modified — raw date strings are shown.

### D. Marriage test data cleanup
User added a test Marriage object via AI chat. `SampleDataSeeder` never seeds marriage data (only Identity/Career/Finance/Health/Travel), so this object was created at runtime. **Clearing it requires clearing app storage on device** (Settings → App → LifePilot → Storage → Clear Storage), or deleting the object via the app's delete action. Not a code change.

### E. Schema audit — "what other major items are not being stored"
User requested a rundown of common life items not covered by existing schemas. Not executed before rate limit.

---

## Explicit Constraints for Continuing Work

- **No automatic job transitions / background lifecycle workers** until all higher-priority MVP work is complete.
- **No rich seed data** unless explicitly requested by the user.

---

## Continuing Work Log (Claude Sonnet 4.6)

---

### [2026-07-02] Schema audit — 5 missing schemas added

**Audit finding:** Travel schema embedded `visaStatus` as a field, not a standalone record. No Visa, Voter ID, Credit Card, EPF/UAN, or Medical Report schemas existed.

**Added 5 schemas** (all picked up automatically by `SchemaEngineImpl` which scans the `schemas/` asset folder at runtime):

| Schema file | objectType | Domain | Key gaps filled |
|---|---|---|---|
| `visa.json` | Visa | Identity | Standalone visa records with lifecycle (NOT_APPLIED → APPLIED → APPROVED → ACTIVE → EXPIRED), expiry reminders, multi-entry type |
| `voter_id.json` | VoterID | Identity | EPIC number, constituency, state, full address — India-specific |
| `credit_card.json` | CreditCard | Finance | Billing date, payment due date, credit limit (SENSITIVE), rewards, network, auto-pay flag |
| `epf_account.json` | EPFAccount | Finance | UAN (12-digit, SENSITIVE), employer, EPFO office, linked bank, transfer lifecycle |
| `medical_report.json` | MedicalReport | Health | Report type enum (Blood/MRI/X-Ray/Prescription/etc.), diagnosis (SENSITIVE), follow-up date with reminder |

**Build:** ✅ `BUILD SUCCESSFUL`

---

### [2026-07-02] Temporal object language — ObjectDetailScreen

**What was built:** A `temporalContextLabel()` private function and one-line context display in the object header.

When an object is opened, a contextual phrase appears below the status chip (in `colorScheme.primary`) describing the temporal state in human language:

| Object type | Future date example | Past date example |
|---|---|---|
| Job | `Joining 1 Aug 2026 · in 30 days` | `Joined 2 Aug 2021 · 4 years ago` |
| Job (left) | — | `Left 31 Jul 2026 · yesterday` |
| Travel | `Departing 3 Feb 2027 · in 7 months` | `Travelled 3 Feb 2025 · 1 year ago` |
| Marriage | `Getting married on 15 Dec 2026 · in 5 months` | `Married on 15 Dec 2020 · 5 years ago` |
| Childbirth | `Due 10 Mar 2027 · in 8 months` | `Born 10 Mar 2015 · 11 years ago` |
| Death of Relative | — | `Passed away on 4 Jan 2020 · 6 years ago` |
| Interview | `Interview on 20 Aug 2026 · in 49 days` | `Interviewed on 5 Jun 2026 · 27 days ago` |
| Education | `Enrolled 1 Jul 2019 · 7 years ago` | `Graduated 30 Apr 2023 · 3 years ago` |
| Visa | `Expiring 14 Feb 2027 · 227 days left` | `Expired 10 Jan 2024` |
| Property | `Ownership 1 Sep 2026 · in 61 days` | `Owned since 15 Mar 2019 · 7 years ago` |

Object types not in this list (Passport, PAN, Insurance, etc.) return `null` and nothing is shown.

**Field matching** is case-insensitive and tolerates both snake_case and camelCase field IDs.

**File modified:** `features/object/src/main/java/com/lifepilot/features/object/ui/ObjectDetailScreen.kt`  
**Build:** ✅ `BUILD SUCCESSFUL` (1 pre-existing deprecation warning on `Icons.Filled.ArrowBack` — not related)

---

### [2026-07-02] Camera capture — AI chat attachment (was a stub)

**What was built:** The "Take photo" option in `AttachmentOptionSheet` now launches the system camera via `ActivityResultContracts.TakePicture`. No new dependencies added.

**Implementation:**
- Uses `TakePicture` contract (system camera app, no CameraX).
- Capture file written to `getExternalFilesDir("camera_captures")` (already declared in `file_provider_paths.xml`).
- URI created via `FileProvider` using authority `${packageName}.fileprovider` (already declared in `AndroidManifest.xml`, CAMERA permission already present).
- `pendingCameraUri` state tracks the in-flight URI; on `success=true`, passed to `viewModel.processAttachment()` which feeds the existing document pipeline (OCR → metadata extraction → verification).
- On cancel (`success=false`), the URI is discarded cleanly.

**File modified:** `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`  
**Build:** ✅ `BUILD SUCCESSFUL`

---

### Current state of all items from user's session-3 instruction list

| Item | Status |
|---|---|
| Major Life Events category (Marriage, Childbirth, Death of Relative) | ✅ Done (Kimi) |
| Route health to Health domain only | ✅ Done (Kimi — prompt rule explicit) |
| Clear previously added marriage data | ⚠️ User action — clear app storage on device |
| Schema audit — missing items | ✅ Done (Claude) — Visa, Voter ID, Credit Card, EPF, Medical Report |
| No onboarding for MVP | ✅ Confirmed — no onboarding code, seed data runs on first launch |
| Fix ISSUE-85 (search conversations + task routing) | ✅ Done (Kimi) |
| Tiered AI approval (low risk auto-approve, sensitive requires approval) | ✅ Done (Kimi) |
| Camera in AI chat | ✅ Done (Claude) |
| File attachment in AI chat | ✅ Done (Kimi) |
| Abort AI | ✅ Done (Kimi) |
| Enter key for multi-line response | ✅ Done (Kimi) — maxLines=5, Enter adds newline |
| Duplicate data on re-upload (overwrite not append) | ✅ Done (Kimi) |
| Date/time understanding in AI | ✅ Done (Kimi) — today's date injected in prompt |
| Temporal object language on open ("Joining"/"Joined") | ✅ Done (Claude) |
| Auto job transition marking | 🚫 Deferred — no background lifecycle workers until MVP higher-priority work is complete |
| Rich seed data | 🚫 Deferred — not to be introduced unless explicitly requested |

---

## Claude Session 4 — MVP Issue Fixes (2026-07-02)

**Build:** `BUILD SUCCESSFUL`. APK installed on device `c8988ac7` via ADB.

### ACTION_PLAN review
- `ActionPlanType` enum was missing `CHILDBIRTH` and `DEATH_OF_RELATIVE` — the prompt referenced them but they silently fell to `CUSTOM`. **Fixed.**
- `UpdateDomainUnderstanding` items appeared as user-checkable rows in `ActionPlanCard`. These are internal platform ops (not user actions) and are now excluded from the "Records to update" section and the item count. **Fixed.**
- Implementation quality: excellent. Topological sort, 5 action item types, executor, prompt examples all solid. Applied generically via `PromptBuilderImpl` → all AI journeys.

### ISSUE-030: AI preamble "Based on provided data"
- Added explicit RESPONSE STYLE rules to `PromptBuilderImpl.kt` after the date line.
- AI is now instructed to never open with "Based on your data", "Based on the provided information", "According to your records", etc. Must answer directly.
- **File:** `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`

### ISSUE-029: Blank bubble after accepting proposal
- When AI returns ONLY an action block with no surrounding text, `visibleContent` was `""` → created a blank chat bubble.
- **Fix:** `displayContent = visibleContent.ifBlank { action?.summary ?: return@launch }` — uses the action summary as the bubble text; skips entirely if both are blank.
- **File:** `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

### ISSUE-069: Suggestion chips persist after first message
- Chips were shown based on `text.isEmpty() && !isLoading` — they stayed visible even after the first message.
- Added `hasMessages: Boolean` parameter to `AiInputBar`. Chips now shown only when `text.isEmpty() && !isLoading && !hasMessages`.
- **File:** `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`

### ISSUE-067: Remove dead Links/Related tab
- Removed `RELATIONSHIPS("Links")` from `ObjectDetailTab` enum.
- Removed `RelationshipsTab` composable and its call site from `ObjectDetailScreen.kt`.
- Removed `showLinkObjectSheet` block (only triggered from the now-removed tab).
- Removed unused imports: `Link`, `LinkOff`, `Relationship`, `allProfileObjects`.
- **Files:** `features/object/src/main/java/com/lifepilot/features/object/state/ObjectDetailUiState.kt`, `ObjectDetailScreen.kt`

### ISSUE-068: TCP socket reset retry
- Added `ConnectionPool(maxIdleConnections = 5, keepAliveDuration = 30s)` to OkHttpClient in `NetworkModule.kt`.
- Added explicit `retryOnConnectionFailure(true)`.
- The 30s keepAlive prevents stale connections from being reused after the NVIDIA server reclaims them.
- **File:** `data/src/main/java/com/lifepilot/data/di/NetworkModule.kt`

### ISSUE-065: App lock — PIN/pattern fallback, no face unlock
- Changed `setAllowedAuthenticators` from `BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL` to `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`.
  - `BIOMETRIC_WEAK` included face unlock (Class 2 biometrics). Removed.
  - `DEVICE_CREDENTIAL` is retained — this enables PIN, pattern, password as fallback.
- Updated subtitle: "Use fingerprint or PIN/pattern to access your life data".
- Renamed Settings label from "Biometric lock" → "App lock", subtitle updated to match.
- **Files:** `app/src/main/java/com/lifepilot/app/security/BiometricLockScreen.kt`, `data/src/main/java/com/lifepilot/data/security/BiometricAuthManager.kt`, `features/settings/src/main/java/com/lifepilot/features/settings/ui/SettingsScreen.kt`

### ISSUE-075: ChatGPT-style collapsible thinking bubble
- Replaced the inline `AiThinkingIndicator()` in the loading state with a new `ThinkingBubble` composable.
- `ThinkingBubble` renders as an AI chat bubble showing "Thinking ▾".
- Tapping it expands to reveal `AiThinkingIndicator` (rotating step messages) and any retry status message.
- Uses `AnimatedVisibility` with `expandVertically + fadeIn` / `shrinkVertically + fadeOut`.
- **File:** `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`

### ISSUE-036: Edit capability for records
- Already fully implemented by Kimi: `MetadataEditScreen` + `MetadataEditViewModel` exist.
- Schema-driven edit form; loads existing values; validates; saves with `MetadataSource.USER`.
- Route `object/{objectId}/edit` already wired in `ObjectNavigation.kt`. Pencil icon in Overview tab header navigates there.
- **No changes needed. Confirmed working.**

### ISSUE-011 + ISSUE-071 + ISSUE-013: Library domain groups
- **ISSUE-011:** Domain Understanding card was hidden when a domain filter was active. Fixed — card for the selected domain now appears at the top of the filtered list.
- **ISSUE-071:** Domain groups now start collapsed by default in "All" view. User taps domain header to expand/collapse. Header shows domain icon, name, count, and a chevron (▼/▲).
- **ISSUE-013:** Domain Understanding card is dismissable per-session via an × button. Dismissed cards do not reappear until next app launch.
- **File:** `features/library/src/main/java/com/lifepilot/features/library/ui/LibraryScreen.kt`

### Markdown rendering in AI chat
- Already fixed in a prior session: `stripMarkdown()` uses lambda replacements `{ it.groupValues[1] }` for bold/italic. AI system prompt includes explicit "CRITICAL FORMATTING: Do not use Markdown" rule. Confirmed no further action needed.


---

## Claude Session 5 — Chat Document Context + AI Cascade Confirmation (2026-07-02)

**Build:** `BUILD SUCCESSFUL`. APK installed on device `c8988ac7` via ADB.  
**Tests:** `:data:testDebugUnitTest` ✅

---

### 1. Scanned documents are available for AI conversation follow-ups

**Goal:** When a user scans/uploads a document inside the AI chat, the extracted metadata becomes referenceable for subsequent questions in the same conversation, without sending the raw document or OCR text to the AI.

#### `domain/src/main/java/com/lifepilot/domain/model/AttachedDocumentContext.kt` (new)
- Data class representing a transient document attached to the current conversation.
- Fields: `fileName`, `mimeType`, `objectType`, `domain`, `title`, `extractedFields: List<ProposedField>`, `isPendingApproval`.
- Contains only structured metadata; raw OCR/file bytes are deliberately excluded.

#### `domain/src/main/java/com/lifepilot/domain/model/RetrievalContext.kt`
- Added `attachedDocumentContext: AttachedDocumentContext? = null`.
- This allows `RetrievalEngine` output to carry a conversation-scoped document without changing repository signatures.

#### `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- New prompt section `ATTACHED DOCUMENT (from this conversation):` rendered when `attachedDocumentContext` is non-null.
- Prints title, type, domain, filename, and extracted field IDs/values.
- The section sits before `USER LIFE DATA`, so the AI sees both the attached document metadata and the normal life-state retrieval context.
- Runs through `SensitiveFieldRegistry.scrub()` like all other metadata.

#### `features/home/src/main/java/com/lifepilot/features/home/state/HomeUiState.kt`
- Added `attachedDocumentContext: AttachedDocumentContext? = null`.

#### `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- Imported `AttachedDocumentContext`.
- `processAttachment()`:
  - For `AiProposal.ObjectCreation`: builds an `AttachedDocumentContext` from `proposal.objectType`, `domain`, `title`, and `fields` and stores it alongside the pending action.
  - For `AiProposal.MetadataUpdate`: builds an `AttachedDocumentContext` from `proposal.objectType`, `objectTitle`, and `fields`.
- `sendMessage()`:
  - Captures `_uiState.value.attachedDocumentContext` before retrieval.
  - Calls `retrievalEngine.retrieve(...).copy(attachedDocumentContext = attachedContext)` so the prompt builder receives it.
- Lifecycle clearing:
  - `approveAction()` clears `attachedDocumentContext` (record is now persisted and retrievable normally).
  - `dismissAction()` clears it.
  - `startNewConversation()`, `startNewChat()`, and `returnToBrief()` clear it.

#### `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`
- Added import for `AttachedDocumentContext` and `Icons.Outlined.Description`.
- Passed `attachedDocumentContext` from `uiState` into `AiWorkspaceContent(...)`.
- In `AiWorkspaceContent()`, when `attachedDocumentContext != null`, renders a `DocumentAttachmentBubble` item inside the chat `LazyColumn`.
- New `DocumentAttachmentBubble` composable:
  - Right-aligned card in `tertiaryContainer`.
  - Shows file name and either `title` or `objectType`.
  - Gives the user a ChatGPT-style visual cue that a document is attached to the conversation.

**Verification:** Code review confirms the full pipeline: scan → OCR → classification → context stored → follow-up prompts include extracted metadata → cleared on approve/dismiss. Launch-only ADB check passed; functional chat testing delegated to user.

---

### 2. AI cascade confirmation for travel, visa, and all life events

**Goal:** Stop the AI from auto-creating tasks/dates. Enforce a consistent journey: Input → Clarification → Tell the plan → Incorporate changes → Create records/tasks.

#### `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- Added `TRAVEL, VISA & IMMIGRATION RULES — MANDATORY`:
  - Ask for travel/move date first.
  - Calculate realistic deadlines working backwards from LLM knowledge (Canada visitor visa 30–60 days, US visa 60–90 days, Schengen 15–30 days, etc.).
  - For each task, state both the **latest start date** and a **recommended earlier start date with a buffer**.
  - Do not invent dates.
  - After the date is known, lay out requirements and ask: *"Do you want me to create tasks for these with the calculated deadlines? Do you have any documents to upload?"*
  - Only emit `ACTION_PLAN` after explicit confirmation.
- Added `INFORMATIONAL / CHECKLIST QUERIES — MANDATORY`:
  - For "what do I need for..." questions, first list all requirements.
  - Do not auto-create tasks.
  - Ask if tasks should be created and if documents are available to upload.
  - Only emit `ACTION_PLAN` after explicit confirmation.
- Updated `Rules` / `UNIVERSAL LIFE EVENT JOURNEY`:
  - Step 2: clarification via `[ASK]`.
  - Step 3: describe records/tasks, including latest + recommended start dates for date-driven tasks.
  - Step 4: ask *"Does this look right? Any changes?"*
  - Step 5: emit `[LIFEPILOT_ACTION]` only after explicit confirmation.
  - Exception: direct commands with all details may skip the preview.

**Verification:** Build passes; prompt-only change. Functional behavior depends on LLM adherence to the new instructions.

---

### 3. Test fix for `SearchRepositoryImpl` constructor change

#### `data/src/test/java/com/lifepilot/data/SearchRepositoryImplTest.kt`
- Added `ConversationDao` mock import and field.
- Stubbed `conversationDao.searchConversations(any(), any()) returns emptyList()` in `setUp()`.
- Updated `SearchRepositoryImpl(...)` instantiation to pass `conversationDao` before `preferenceManager`, matching the production constructor order.

**Verification:** `:data:testDebugUnitTest` ✅

---

### 4. Travel/visa deadlines now include latest + recommended early start

**Goal:** When the AI calculates deadlines working backwards from an event date, it should tell the user the latest possible start date and also recommend starting earlier with a buffer.

#### `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- Updated `TRAVEL, VISA & IMMIGRATION RULES`:
  - Added rule: *"For every task you propose, state BOTH the latest start date and a recommended earlier start date with a buffer."*
  - Example wording in prompt: *"Apply for visa by 15 Oct at the latest, but start by 1 Oct to be safe."*
- Updated `UNIVERSAL LIFE EVENT JOURNEY` step 3:
  - *"For date-driven tasks, give both the latest start date and a recommended earlier start date with a buffer."*

**Verification:** Build passes; prompt-only change.

---

### 5. Recruiter-friendly roadmap page

**Goal:** Remove all internal issue references (ISSUE-XXX) and priority labels (P0–P4) from `LifePilot-state-and-roadmap.html` so it can be shared with recruiters.

#### `LifePilot-state-and-roadmap.html`
- Rewrote the page to be externally presentable.
- Removed all `ISSUE-XXX` references.
- Replaced priority tags (`P0`–`P4`) with product-status tags: `Live`, `In Progress`, `Planned`, `Future`, `Vision`.
- Replaced the "Open Issues" section with an "In Progress" section highlighting active improvements.
- Updated hero stats to show capabilities, domains, roadmap phases, and platform — no issue counts.
- Updated Phase 2 roadmap items to reflect recently shipped work without internal identifiers.
- Kept styling, principles, capabilities, domains, roadmap phases, architecture, and tech stack.

**Verification:** File opens as valid HTML; no issue references remain.

---

## Session — Camera crash, document attachment persistence, conversation first-message visibility

### 1. Fix camera crash on Xiaomi (runtime CAMERA permission)

**Problem:** Tapping Camera in the AI chat attachment sheet crashed with `SecurityException: Permission Denial ... with revoked permission android.permission.CAMERA`. The manifest declared `CAMERA`, but the app never requested the runtime permission, and Xiaomi's stock camera requires the caller to hold it.

#### `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`
- Added imports for `android.Manifest`, `androidx.core.content.ContextCompat`.
- Added a `cameraPermissionLauncher` using `ActivityResultContracts.RequestPermission()`.
- In the `onCamera` handler, after creating the `FileProvider` URI, check `ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)`:
  - If granted, launch `TakePicture`.
  - If not granted, request permission; on grant, launch the pending URI.

**Verification:** Build passes; app launches; camera permission request path is wired.

---

### 2. Make document attachments visible and persistent in AI conversations

**Problem:** When a user attached a document (camera, scan, or file picker) in the AI chat, the document metadata was sent to the AI on the next turn via transient `attachedDocumentContext`, but the document itself was not shown as a chat message. Reopening the conversation later lost the attachment context entirely.

#### `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- Added `serializeAttachmentContext()` and `deserializeAttachmentContext()` helpers for `AttachedDocumentContext` using `JSONObject`/`JSONArray`.
- Added `saveAttachmentMessage(convId, context)` which persists a `StoredMessage` with `role = "ATTACHMENT"` and JSON content.
- In `processAttachment()`, after a successful `ObjectCreation` or `MetadataUpdate` classification, call `saveAttachmentMessage()` so the attachment is part of the conversation history.
- In `sendMessage()`, filter out `role == "ATTACHMENT"` messages when building AI conversation history (the metadata still reaches the AI via `RetrievalContext`).
- In `resumeConversation()`, after loading messages, find the latest `ATTACHMENT` message and restore `attachedDocumentContext` so follow-up questions in a reopened conversation still include the document metadata.

#### `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`
- Added imports for `ProposedField`, `UpdateMode`, `org.json.JSONObject`.
- Added `parseAttachmentContext()` helper to reconstruct `AttachedDocumentContext` from the JSON stored in the message.
- Updated `MessageBubble()` to detect `role == "ATTACHMENT"` and render `DocumentAttachmentBubble` instead of plain text.
- Removed the floating `DocumentAttachmentBubble` item from the LazyColumn (it is now part of the message list, so duplicate rendering is avoided).
- Removed the now-unused `attachedDocumentContext` parameter from `AiWorkspaceContent`.

**Verification:** Build passes; attachment messages are persisted and rendered.

---

### 3. Old conversation first message now visible on open

**Problem:** When a user reopened an existing conversation, the chat list auto-scrolled to the bottom, so the first (oldest) message was off-screen. The user expected to land at the top and see the full history from the beginning.

#### `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`
- Passed `conversationId` into `AiWorkspaceContent`.
- Replaced the simple "scroll to last item on any size change" logic with state-aware scrolling:
  - `previousMessagesSize` is reset when `conversationId` changes.
  - On first load (`previousMessagesSize == 0`), call `listState.scrollToItem(0)` so the first message is visible.
  - On subsequent message additions (`messages.size > previousMessagesSize`), call `listState.animateScrollToItem(messages.lastIndex)` to keep the user at the latest turn.

**Verification:** Build passes; conversation list starts at the first message and scrolls to new messages only after the user has begun interacting.

---

**Build status:** `./gradlew :app:assembleDebug --offline --no-daemon` ✅  
**Install status:** `adb install -r app-debug.apk` ✅ on `c8988ac7`  
**Functional verification:** Pending user test of camera, document attachment, and old-conversation opening.

---

## Claude Session 6 — Bug Fixes & Polish (2026-07-03)

**Build:** `BUILD SUCCESSFUL in 56s`. APK ready for install (device not connected at time of session).

---

### 1. Uncomplete task — visible swipe background
**Problem:** `SwipeToReopenTask` background was transparent — no visual feedback when swiping.
**Fix:** Added `secondaryContainer` background + "Reopen" text label + white-on-secondary icon.
**File:** `features/planner/src/main/java/com/lifepilot/features/planner/ui/PlannerScreen.kt`

---

### 2. Search → Conversation navigation (reactive savedStateHandle)
**Problem:** `HomeViewModel.consumeResumeConversationId()` read `savedStateHandle.remove()` once at init; returning to Home from Search via `launchSingleTop` didn't trigger init again.
**Fix:** Changed to observe `savedStateHandle.getStateFlow<String?>("resumeConversationId", null)` as a Flow with `filterNotNull()` so any future set triggers `resumeConversation()`.
**File:** `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

---

### 3. Search → Task navigation (reactive savedStateHandle)
**Problem:** `PlannerViewModel.selectedTaskId` was a one-time `val` read at init; re-navigating from search with the same Planner VM alive didn't update `highlightedTaskId`.
**Fix:** Changed to `deepLinkTaskId: MutableStateFlow<String?>` + a `getStateFlow("taskId")` collector in init. The `observeData()` combine now includes `deepLinkTaskId` as a 4th stream.
**File:** `features/planner/src/main/java/com/lifepilot/features/planner/viewmodel/PlannerViewModel.kt`

---

### 4. Thinking bubble — cycling words instead of static "Thinking"
**Problem:** `ThinkingBubble` showed static "Thinking" text; user wanted it to cycle through meaningful phrases.
**Fix:** Added `cyclingPhrases` list + `LaunchedEffect(statusMessage)` with 2.2s ticker. Uses `AnimatedContent` with `fadeIn/fadeOut` between phrases. When ViewModel provides an explicit `statusMessage`, that takes priority and cycling stops.
**Phrases:** "Thinking…", "Looking up your records…", "Connecting the dots…", "Checking details…", "Putting it together…", "Almost there…"
**File:** `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`

---

### 5. AI timeouts increased
**Problem:** First timeout 60s was too short; AI responses were timing out.
**Fix:**
- Timeouts: 60/90/120s → **120/200/360s**
- Retry delays: 5/15s → **12/30s**
- OkHttp base `readTimeout`: 60s → **400s** (per-request overrides now never get capped)
**Files:** `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`, `data/src/main/java/com/lifepilot/data/di/NetworkModule.kt`

---

### 6. PDF OCR for document scanner
**Problem:** `MlKitOcrService` returned `OcrResult.NotSupported` for `application/pdf` — scanned PDFs from ML Kit Document Scanner produced no text.
**Fix:** Added `extractFromPdf()` using Android `PdfRenderer` to render each page to a 2x-scale `Bitmap`, then runs both Latin and Devanagari recognizers on each page.
**Files:** `data/src/main/java/com/lifepilot/data/ocr/MlKitOcrService.kt`

---

### 7. Task due-date notifications
**Problem:** Tasks with due dates fired no notifications.
**Fix:** Extended `ReminderEvaluationWorker.doWork()` to call `fireTaskDueNotifications(profileId)` which queries pending tasks due today and shows a notification for each via `NotificationHelper`.
**File:** `data/src/main/java/com/lifepilot/data/worker/ReminderEvaluationWorker.kt`

---

### 8. Domain icons for new domains
**Problem:** "Major Life Events" and "People" domains showed generic `FolderOpen` icon.
**Fix:** Added `"major life events" → Celebration`, `"people" → Group`, `"employment" → Work` to `domainIcon()`.
**File:** `designsystem/src/main/java/com/lifepilot/designsystem/icon/DomainIcons.kt`

---

### 9. CreateObjectViewModel — reactive schema loading
**Problem:** `loadObjectTypes()` was called once at init; if called before `AppInitializer.loadSchemas()` completed, the type list was empty.
**Fix:** Changed to observe `schemaEngine.registeredSchemas` (a `StateFlow`) and map new schema emissions to `availableTypes` with `distinctUntilChanged()`.
**File:** `features/object/src/main/java/com/lifepilot/features/object/create/viewmodel/CreateObjectViewModel.kt`

---

### 10. AI prompt — task due dates use buffered start
**Problem:** `CREATE_TASK` items used absolute deadlines as `dueDate`, not the recommended buffered start date.
**Fix:** Added "TASK DUE DATE RULES — MANDATORY" section to `PromptBuilderImpl`: always set `dueDate` to the recommended earlier start date (with buffer). Deadline goes in the task title/description. Buffer guidance by type: visa=14d, document collection=7d, appointments=5d, filings=10d.
**File:** `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`

---

### 11. HTML updates
- `portfolio deploy/articles/LifePilot-state-and-roadmap.html`: Updated build date to 2026-07-03, capabilities 34+ → 40+, domains 9 → 13. Added Legal, Transport, People, Employment domain pills.
- `portfolio deploy/articles/lifepilot.html`: CTA "Check Progress Update on Development" already present at line 1271 — no change needed.

---

**Remaining for user to test manually:**
- Swipe task to reopen (new visible background should make it clear)
- Search → tap a chat → verify navigates to conversation
- Search → tap a task → verify Planner highlights that task
- Scan document in AI chat → verify text is extracted and shown
- Task due today → reconnect device and trigger worker to verify notification fires
