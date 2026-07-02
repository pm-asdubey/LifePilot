# LifePilot — Running Issue Log

> Each issue documents observed behaviour, verified root cause, files involved, and the planned fix approach so no analysis work needs to be repeated when implementation begins.
> Fixes must be applied generically — no one-off patches. If a pattern is broken, fix the pattern everywhere.
>
> **IMPORTANT: Do not begin implementing any issue unless the user explicitly asks for it by issue number or description. This log is for planning and reference only.**

**Last updated:** 2026-07-02 | **Total open:** 46 | **Resolved:** 27 (marked ✓ below) | **Partially resolved:** 1

---

## Priority Legend
- **P0 — Critical:** App crash, data integrity failure, or security exposure. Fix before any other work.
- **P1 — High:** Core value proposition broken. The app's main purpose is impaired.
- **P2 — Medium:** Missing feature the user expects. Product feels incomplete without it.
- **P3 — Low:** AI quality and UX polish improvements.
- **P4 — Roadmap:** Deliberate future work, not expected in the current build.

---

# P0 — Critical

---

## ISSUE-004 — Search crashes the app
**Reported:** 2026-06-29 | **Priority:** P0
**Status: RESOLVED ✓** — Fixed by Kimi. Replaced unstable `Material3 SearchBar` with a standard `TextField` + results list. Verified on device without crash.

**Observed behaviour:**
Opening Search or typing in the search field crashes the app.

**Root cause (verified):**
Confirmed from device logcat (PID 26993). `ComposeRuntimeError: Start/end imbalance`. Material3 `SearchBar` is `@ExperimentalMaterial3Api` and has known recomposition instability when its `active` state transitions.

**Planned fix approach:**
Replace Material3 `SearchBar` with a standard `TextField` + search icon + results list. Removes the unstable experimental composable entirely. Apply the same replacement to any other screen using experimental Compose components with known instability.

**Files involved:**
- `features/search/src/main/java/com/lifepilot/features/search/ui/SearchScreen.kt`

---

## ISSUE-055 — No formal security audit; data boundaries between local processing and AI transmission undefined
**Reported:** 2026-06-29 | **Priority:** P0
**Status: RESOLVED ✓** — Fixed by Kimi. `DATA_FLOW.md` and `PRIVACY_DECLARATION.md` created in `docs/security/`. Sensitive field redaction added to `PromptBuilderImpl`. Automated redaction unit tests pass. `SensitiveFieldRegistry` created. Timber scrubbing tree planted.

**Observed behaviour:**
There is no documented or verified boundary between what LifePilot processes locally and what leaves the device. A user trusting the app with their passport number, Aadhaar, PAN, salary, health records, and travel plans has no way to verify what is or is not sent to an AI provider. No security audit has been conducted. No data flow diagram exists.

**Root cause (verified):**
Security architecture was designed incrementally (ISSUE-018 covers storage and transmission gaps) but no holistic audit has been performed. The precise data flow from user input → local processing → AI prompt construction → API transmission → response → storage has never been mapped end-to-end and verified against the intended privacy model.

**Required audit scope:**

*What must be verified as processed locally only (never leaves device):*
- Raw OCR text before field extraction
- Full document binary files (PDF, images)
- Biometric authentication
- Encryption key derivation and storage
- Room database contents
- File path resolution
- Rule engine evaluation (reminders, expiry)
- Deterministic Layer 1 query responses

*What is sent to cloud AI provider (must be audited and minimised):*
- Constructed prompt text — audit every field included by `PromptBuilderImpl`
- Sensitive field values — must be redacted before inclusion (ISSUE-019)
- Conversation history included in context window — verify no raw document text is included
- System prompt — verify no hardcoded personal data

*What is sent to internet search provider (Layer 3):*
- Search query string only — verify it contains no personal identifiers
- Zero personal field values in any search query, enforced at `InternetIntelligenceOrchestrator`

*What is stored in cloud (Drive backup):*
- ZIP archive contents — verify encryption before upload
- No metadata sent to Drive API beyond file name and size

**Planned fix approach:**
1. Produce a formal **Data Flow Diagram** (DFD) covering every data path in the application. Stored in `docs/security/DATA_FLOW.md`.
2. Annotate every call to `PromptBuilderImpl` with a comment listing which field categories are included and why.
3. Write an automated test that constructs a prompt with known sensitive values and asserts those values are masked before the string leaves the method.
4. Produce a **Privacy Declaration** document (`docs/security/PRIVACY_DECLARATION.md`) suitable for showing to users: plain-language description of what stays on device, what is sent where, and under what conditions.
5. Conduct a manual code review of the network layer — every outbound HTTP call must be listed with its payload contents.
6. This issue does not close until the DFD, privacy declaration, and automated redaction tests all exist and pass.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt` — primary audit target
- `data/src/main/java/com/lifepilot/data/ai/NvidiaAiProvider.kt` — outbound payload audit
- `data/src/main/java/com/lifepilot/data/ai/orchestration/InternetIntelligenceOrchestrator.kt` — search query audit
- New: `docs/security/DATA_FLOW.md`
- New: `docs/security/PRIVACY_DECLARATION.md`

---

## ISSUE-018 — No security model: sensitive data stored and transmitted without protection
**Reported:** 2026-06-29 | **Priority:** P0
**Status: RESOLVED ✓** — Fixed by Kimi (+ Claude for FLAG_SECURE removal). `FLAG_SECURE` added (later removed per user request to allow screenshots). `SensitiveFieldRegistry` created. `ScrubbingTree` Timber tree scrubs sensitive values from logs. `PromptBuilderImpl` redacts sensitive fields before AI transmission. Remaining gaps (SQLCipher DB encryption, biometric-gated export) deferred to later milestone.

**Observed behaviour:**
Users upload passport numbers, PAN, Aadhaar, bank details, health records. Six security gaps exist.

**Root cause (verified):**
1. Room DB unencrypted plaintext — no SQLCipher.
2. Full sensitive values sent verbatim to NVIDIA NIM API.
3. Export bundle is plaintext JSON.
4. No Timber scrubbing of sensitive field values.
5. No `FLAG_SECURE` — screenshots and recents capture sensitive screens.
6. Biometric lock is optional and buried in Settings.

**Planned fix approach:**
- Now (P0): `FLAG_SECURE` on `MainActivity` (one line). Create `SensitiveFieldRegistry` and scrub matching fields from all Timber logs.
- Next (P1): Encrypt Room DB with SQLCipher + Android Keystore. Redact sensitive fields before sending to AI (see ISSUE-019).
- Later (P2): Encrypt export bundle. Prompt biometric setup in onboarding.

**Files involved:**
- `app/src/main/java/com/lifepilot/app/MainActivity.kt`
- `data/src/main/java/com/lifepilot/data/database/LifePilotDatabase.kt`
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- New: `domain/src/main/java/com/lifepilot/domain/security/SensitiveFieldRegistry.kt`

---

## ISSUE-022 — Cancelling a record via AI updates metadata but status stays Active
**Reported:** 2026-06-29 | **Priority:** P0
**Status: RESOLVED ✓** — Fixed by Kimi. `STATUS_UPDATE` action type added to `PromptBuilderImpl`. `AiProposal.StatusUpdate` sealed subclass added. `StatusUpdateCard` UI added. `HomeViewModel.executeProposal()` wired to `UpdateObjectStatusUseCase`. Status synonym mapping (`cancel/end/close → INACTIVE`, `expire → EXPIRED`) added. End-to-end confirmed via code review; not manually tested on device.

**Observed behaviour:**
"My US trip is cancelled" → metadata note added but object still shows Active in My Records.

**Root cause (verified):**
`executeProposal()` for `MetadataUpdate` only calls `metadataRepository.upsertMetadata()`. Never calls `objectRepository.updateObjectStatus()` (which already exists). No `STATUS_UPDATE` action type exists in the prompt.

**Planned fix approach:**
1. Add `STATUS_UPDATE` action type to `PromptBuilderImpl`.
2. Add `AiProposal.StatusUpdate(objectId, objectTitle, newStatus: ObjectStatus, summary)` to the sealed class.
3. Wire `parseAction()` to map status string → `ObjectStatus`.
4. Wire `executeProposal()` to call `objectRepository.updateObjectStatus()`.
5. Prompt instruction: cancelled/ended/expired/closed events → prefer `STATUS_UPDATE`.
6. Generic application: same gap causes job endings, expired insurance, and closed accounts to also fail status updates. One fix covers all.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- `domain/src/main/java/com/lifepilot/domain/model/ProposedAction.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

---

## ISSUE-001 — Every new conversation appends to the first conversation
**Reported:** 2026-06-29 | **Priority:** P0
**Status: RESOLVED ✓** — Fixed by Kimi. `startNewConversation()` added to `HomeViewModel` — clears `currentConversationId`, resets title, clears messages. "New chat" button wired in `HomeScreen`. `returnToBrief()` also clears conversation state. Auto-clear after 4-hour idle gap added.

**Observed behaviour:**
Starting a new AI conversation lands inside the existing first conversation instead of creating a fresh one.

**Root cause (verified):**
`sendMessage()` passes persisted `currentConversationId` to `getOrCreateConversation()`. If non-null it always returns the existing conversation. No "new conversation" action clears this ID.

**Planned fix approach:**
Add `startNewConversation()` to `HomeViewModel` — sets `currentConversationId = null`. Surface as a "New" button in the AI workspace. Also auto-clear when switching from Daily Brief to AI workspace if the last conversation is older than a configurable threshold (e.g. 4 hours).

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- `data/src/main/java/com/lifepilot/data/repository/ConversationRepositoryImpl.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`

---

## ISSUE-025 — Document re-upload appends metadata instead of overwriting; duplicates corrupt AI context
**Reported:** 2026-06-29 | **Priority:** P0
**Status: RESOLVED ✓** — Fixed by Kimi. `MetadataRepository.deleteAllMetadataForObject()` added. `MetadataVerificationViewModel` deletes existing `AI_EXTRACTED`/`OCR` fields before inserting fresh extraction. `ObjectReasonerImpl.buildSnapshot()` deduplication guard added — keeps highest-confidence most-recent value if duplicate `(objectId, fieldId)` rows exist. Confirmed via code review.

**Observed behaviour:**
Uploading a document that already has an existing record creates a second set of metadata entries rather than replacing the old ones. Once two conflicting entries exist for the same field (e.g. two `expiry_date` values on a passport), the AI starts returning contradictory or empty answers and eventually stops reasoning over that object entirely.

**Root cause (verified):**
`MetadataRepositoryImpl.upsertMetadata()` uses Room's `OnConflictStrategy.REPLACE` on the `(objectId, fieldId)` composite key — but the upload pipeline calls `insertMetadata()` (INSERT only) when processing a freshly extracted document, bypassing upsert. The result is duplicate rows for the same `(objectId, fieldId)` pair. `RetrievalEngineImpl` and `ObjectReasoner` then build the context snapshot from all matching rows, concatenating duplicates into incoherent values.

Additionally, uploaded files are stored in their original format (JPEG, PNG, PDF) with no normalisation. Multiple uploads of the same physical document land as separate file entries with no deduplication check.

**Planned fix approach:**
1. Audit all call sites in the document processing pipeline. Replace every `insertMetadata()` call with `upsertMetadata()` — no exceptions.
2. Before inserting any extracted metadata, call `metadataRepository.deleteAllForObject(objectId)` to wipe stale fields, then insert fresh. Apply this to both manual uploads and OCR-triggered extraction.
3. Convert all uploaded images to PDF at the point of ingestion (`PdfRenderer` / `iTextPDF`) before storing. One canonical file per document — overwrite the existing file if the object already has one of the same type.
4. Add a `documentHash` field to the document record. On upload, compute SHA-256 of the raw bytes. If hash matches an existing document on the same object, skip re-processing entirely (exact duplicate). If hash differs, proceed with overwrite path.
5. In `ObjectReasoner.buildSnapshot()`, add a deduplication guard: if multiple rows exist for the same `(objectId, fieldId)`, take the one with the highest `confidence` and most recent `updatedAt`. This prevents any existing bad data from breaking AI context.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/repository/MetadataRepositoryImpl.kt`
- `data/src/main/java/com/lifepilot/data/pipeline/DocumentProcessingPipeline.kt`
- `data/src/main/java/com/lifepilot/data/engine/ObjectReasoner.kt`
- `domain/src/main/java/com/lifepilot/domain/usecase/UploadDocumentUseCase.kt`

---

# P1 — High

---

## ISSUE-015 — AI creates a travel record instead of answering document readiness question
**Reported:** 2026-06-29 | **Priority:** P1

**Observed behaviour:**
"I am going to the USA, are all my documents in order?" — AI created a Travel object. Did not answer which documents are valid, expiring, missing (US visa), or need to be applied for/uploaded.

**Root cause (verified):**
1. Keyword scorer gives Passport (domain=Identity) a score of 0 for tokens ["going", "usa"] — AI never sees the passport.
2. No cross-domain affinity: travel queries do not pull Identity domain objects.
3. Prompt has no instruction for document readiness queries.

**Planned fix approach:**
1. **Domain-affinity rules** in `RetrievalEngineImpl`: travel intent → force-include all Identity domain objects. Configurable affinity table, not hardcoded.
2. **Computed `expiryStatus`** in `ObjectReasoner.buildSnapshot()`: `"Valid — 604 days remaining"` / `"Expiring soon"` / `"Expired"` — not raw date strings.
3. **Prompt readiness block**: enumerate Identity documents with expiry status, flag missing visas for destination, propose `TASK_CREATION` for each gap.
4. Generic application: same affinity pattern applies to Health queries pulling medical records, Finance queries pulling insurance.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/engine/RetrievalEngineImpl.kt`
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- `data/src/main/java/com/lifepilot/data/engine/ObjectReasoner.kt`

---

## ISSUE-010 — Health symptom routed to object record; no proactive next steps generated
**Reported:** 2026-06-29 | **Priority:** P1

**Observed behaviour:**
"I am experiencing pain in my right leg" added a note to "Annual Health Summary". Did not update Health domain life state. Did not suggest a doctor, create a task with location/schedule context, or remember scheduling preferences.

**Root cause (verified):**
1. No `DOMAIN_LIFE_STATE_UPDATE` action type — AI can only update object metadata or create objects. Symptoms have no natural object target.
2. Prompt gives no instruction to use location and profession when generating health tasks.
3. No mechanism to persist inferred preferences (e.g. "prefers weekend appointments") into domain life state.

**Planned fix approach:**
1. Add `DOMAIN_LIFE_STATE_UPDATE` action type to `PromptBuilderImpl` — targets a domain name, updates `currentSituation`, `recentChanges`, `openQuestions`, `recommendations` directly. No object match required.
2. Add `AiProposal.DomainLifeStateUpdate` to the sealed class.
3. Wire approval → `domainRepository.upsertDomainLifeState()`.
4. Prompt: health symptoms → always emit `DOMAIN_LIFE_STATE_UPDATE` for Health AND a `TASK_CREATION` for doctor visit using location/profession from context.
5. Add `userPreferences: List<String>` to `DomainLifeState`. AI can store inferred habits via `DOMAIN_LIFE_STATE_UPDATE`.
6. Generic application: `DOMAIN_LIFE_STATE_UPDATE` is the correct action for any observation that updates domain understanding without being tied to a specific object (moods, habits, general status reports).

**Files involved:**
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- `domain/src/main/java/com/lifepilot/domain/model/ProposedAction.kt`
- `domain/src/main/java/com/lifepilot/domain/model/DomainLifeState.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

---

## ISSUE-007 — AI creates objects without gathering details or proposing tasks for gaps
**Reported:** 2026-06-29 | **Priority:** P1

**Observed behaviour:**
"I have a trip to Dubai in 2028" → object created immediately. No clarifying questions. No Goal + Tasks for incomplete items.

**Root cause (verified):**
`AiProposal.ObjectCreation` fires on detection with no clarification round-trip. `GoalProposal` supports `suggestedTasks` but is never triggered from object creation. Prompt does not instruct information gathering before creation.

**Planned fix approach:**
1. Prompt rule: when a new life event has fewer than N known details, emit `[ASK]` first — not `OBJECT_CREATION`.
2. Only emit `OBJECT_CREATION` after the user responds OR explicitly says "just add it".
3. After creation, if user indicated incomplete items, follow with `GOAL_PROPOSAL` whose `suggestedTasks` cover each gap.
4. Generic application: same pattern for new job, new property, new insurance policy.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- `domain/src/main/java/com/lifepilot/domain/model/ProposedAction.kt`

---

## ISSUE-011 — Current Understanding card hidden when a domain tab is selected in Library
**Reported:** 2026-06-29 | **Priority:** P1
**Status: RESOLVED ✓** — Fixed by Claude (Session 4). `DomainUnderstandingCard` now emits at the top of the filtered list when a domain tab is active, using `domainLifeStateMap[selectedDomainFilter]`. No longer hidden behind the `selectedDomainFilter == null` guard.

**Observed behaviour:**
Tapping a domain tab (e.g. Career) hides the Current Understanding card for that domain. Card only shows in the All view.

**Root cause (verified):**
`DomainUnderstandingCard` rendered inside `if (selectedDomainFilter == null)` block. When a tab is selected, guard fails and card is never emitted.

**Planned fix approach:**
Emit the card as the first `item` in the LazyColumn when a domain filter is active, using `domainLifeStateMap[selectedDomainFilter]`. Keep per-domain cards under sticky headers in the all-domains view. Single block move.

**Files involved:**
- `features/library/src/main/java/com/lifepilot/features/library/ui/LibraryScreen.kt` — lines 313–353

---

## ISSUE-002 — Completing a task does not update the domain life state
**Reported:** 2026-06-29 | **Priority:** P1

**Observed behaviour:**
Completing a task marks it done but the domain's Current Understanding is unchanged.

**Root cause (verified):**
`PlannerViewModel.completeTask()` calls `planningEngine.completeTask()` only. No downstream call to `DomainLifeStateEngine`. Task completion is a dead end in the Life State Engine pipeline.

**Planned fix approach:**
After `planningEngine.completeTask()` succeeds, call `domainLifeStateEngine.evaluateAndUpdate()` for the relevant domain. Generic application: same gap applies to archiving objects, completing goals, dismissing reminders. Wire all post-action events into the same `evaluateAndUpdate` call.

**Files involved:**
- `features/planner/src/main/java/com/lifepilot/features/planner/viewmodel/PlannerViewModel.kt`
- `domain/src/main/java/com/lifepilot/domain/engine/DomainLifeStateEngine.kt`

---

## ISSUE-021 — AI timeout with no retry and no user feedback
**Reported:** 2026-06-29 | **Priority:** P1

**Observed behaviour:**
Slow or timed-out AI calls spin for 60s with no message, then show a generic error. No retry.

**Root cause (verified):**
OkHttp: `connectTimeout(30s)` / `readTimeout(60s)`, single attempt. `NvidiaAiProvider` has one `try/catch` → `Result.failure`. No retry loop, no status messages.

**Planned fix approach:**
1. Retry loop: up to 3 attempts, delays 0s → 5s → 15s, timeouts 60s → 90s → 120s.
2. After each retryable failure, update `HomeUiState.aiStatusMessage`:
   - Attempt 2: "Taking longer than usual, retrying…"
   - Attempt 3: "Still working on it…"
   - All failed: error message + manual Retry button.
3. Distinguish retryable (timeout, 503, no network) from permanent (401, 400) — never retry permanent failures.

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- `data/src/main/java/com/lifepilot/data/ai/NvidiaAiProvider.kt`
- `data/src/main/java/com/lifepilot/data/di/NetworkModule.kt`
- `features/home/src/main/java/com/lifepilot/features/home/state/HomeUiState.kt`

---

## ISSUE-026 — No camera capture or file attachment in AI chat or Library
**Reported:** 2026-06-29 | **Priority:** P1
**Status: PARTIALLY RESOLVED** — File attachment: done by Kimi (`AttachmentOptionSheet` + paperclip button in `AiInputBar`). Camera capture: done by Claude (Session 4) via `TakePicture` contract + `FileProvider`. ML Kit document scanner ("Scan document") still a stub. Library "Add new" flow not yet connected.

**Observed behaviour:**
There is no way to take a photo of a physical document or attach an existing file (PDF, image) from within the AI chat or from Library and have it automatically OCR'd, classified, and saved. The user must leave the app, find the file separately, then attempt to import — with no guarantee of classification or extraction.

**Root cause (verified):**
No camera integration exists anywhere in the codebase. `UploadDocumentUseCase` accepts a `Uri` but the UI only sources this from the file picker (`ActivityResultContracts.GetContent`). No `CameraX` capture flow, no ML Kit document scanner, and no in-chat media attachment path exist. The file picker is also not surfaced from the AI chat input bar at all.

**Planned fix approach:**
1. Add an attachment button (paperclip icon) to the AI chat input bar alongside the send button.
2. Tapping it opens a bottom sheet with three options: **"Take photo"**, **"Scan document"**, **"Attach file"**.
3. "Take photo": `CameraX` image capture → crop/confirm screen → PDF conversion → `DocumentProcessingPipeline`.
4. "Scan document": ML Kit Document Scanner (`GmsDocumentScannerOptions`) with auto edge detection and perspective correction → PDF → same pipeline.
5. "Attach file": `ActivityResultContracts.GetContent` supporting PDF, JPG, PNG, HEIC → same pipeline.
6. `DocumentProcessingPipeline`: OCR (ML Kit Text Recognition) → `ClassificationEngine` infers object type and domain → emits `AiProposal.ObjectCreation` with pre-populated fields for user verification.
7. Verification step: show the scanned image / attached file alongside extracted fields. User confirms, corrects, or supplements each field before saving.
8. Same attachment entry point available in Library "Add new" flow (see ISSUE-027).
9. Apply generically: pipeline handles all document types through the same classification path — not per-type hardcoding.

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt` — add attachment button to `AiInputBar`
- New: `features/home/src/main/java/com/lifepilot/features/home/ui/DocumentCaptureSheet.kt`
- `data/src/main/java/com/lifepilot/data/pipeline/DocumentProcessingPipeline.kt`
- `data/src/main/java/com/lifepilot/data/ocr/OcrEngine.kt`
- New: `data/src/main/java/com/lifepilot/data/classification/ClassificationEngine.kt`
- `app/src/main/AndroidManifest.xml` — camera permission, READ_EXTERNAL_STORAGE

---

## ISSUE-027 — "Add new" in Library forces predefined types; custom document types not possible
**Reported:** 2026-06-29 | **Priority:** P1

**Observed behaviour:**
Tapping "Add new" in Library presents a fixed list of hardcoded object types. There is no way to add a US Visa, a custom insurance document, a property deed, or any document type not on the preset list. This directly blocks real-world usage.

**Root cause (verified):**
`AddObjectSheet` (or equivalent) renders a hardcoded list of type strings rather than querying `SchemaEngine.getAllObjectTypes()`. Additionally, `SchemaEngine` itself only knows about types defined at build time in the bundled schema JSON — there is no user-defined type path.

**Planned fix approach:**
1. Remove all hardcoded type lists from the UI. The Add New flow must source types exclusively from `SchemaEngine.getAllObjectTypes()`.
2. Add a "Custom / Not listed" option at the bottom of every type picker. Selecting it allows the user to type a free-form object type name. This creates an ad-hoc schema entry with no predefined fields — the user adds metadata fields manually, or the AI extracts them from an uploaded document.
3. Add a domain picker (Identity, Career, Finance, Health, Travel, Other) as the first step, before the type picker. This narrows the list and provides context for classification.
4. After selecting type + domain, the flow offers: "Scan document" (→ ISSUE-026 camera path) or "Enter details manually" (→ blank metadata form).
5. Schema engine should persist user-created types to a `user_schemas` table so they survive app restarts and appear consistently in type pickers throughout the app.
6. Apply generically: every type picker in the app (object creation, AI proposal cards, search filters) must use `SchemaEngine` — never a hardcoded list.

**Files involved:**
- `features/library/src/main/java/com/lifepilot/features/library/ui/LibraryScreen.kt` — Add New entry point
- New: `features/library/src/main/java/com/lifepilot/features/library/ui/AddObjectFlow.kt`
- `domain/src/main/java/com/lifepilot/domain/engine/SchemaEngine.kt`
- `data/src/main/java/com/lifepilot/data/schema/SchemaEngineImpl.kt`
- New: `data/src/main/java/com/lifepilot/data/database/entity/UserSchemaEntity.kt`

---

## ISSUE-036 — No edit capability for objects and metadata after creation
**Reported:** 2026-06-29 | **Priority:** P1
**Status: RESOLVED ✓** — Confirmed by Claude (Session 4). `MetadataEditScreen` + `MetadataEditViewModel` exist. Schema-driven edit form loads existing values, validates, and saves with `MetadataSource.USER`. Route `object/{objectId}/edit` wired in `ObjectNavigation.kt`. Pencil icon in Overview tab navigates to edit.

**Observed behaviour:**
Once a record is created — whether via AI proposal or manual entry — there is no way to edit the object title, description, domain, type, or any metadata field. If an AI-extracted value is wrong and the user accepted it, or if a detail changes over time (new phone number, updated address, different job title), the record is permanently stuck with the original value unless deleted and recreated.

**Root cause (verified):**
`ObjectDetailScreen` renders metadata as a read-only list. No edit mode, no inline edit action, and no `EditObjectUseCase` or `UpdateMetadataUseCase` call path exists from the UI. `ObjectRepository.updateObject()` likely exists at the data layer but is never wired to a UI surface.

**Planned fix approach:**
1. Add an edit icon (pencil) to the `ObjectDetailScreen` top bar. Tapping it enters edit mode for the entire screen.
2. In edit mode: object title and description become editable `TextField`s. Domain/type become a picker (sourced from `SchemaEngine`). Each metadata row gains an inline edit field.
3. Add a "Add field" button at the bottom of the metadata list to let users manually add new fields not extracted by AI.
4. On save: call `objectRepository.updateObject()` for title/description changes and `metadataRepository.upsertMetadata()` for each changed or added field. Set `verificationStatus = VERIFIED` and `source = USER` on any manually edited field.
5. Long-press on a metadata row in read mode opens a context menu: Edit / Delete field.
6. Apply generically: edit must work for all object types, all domains, and both AI-created and manually-created records.

**Files involved:**
- `features/object/src/main/java/com/lifepilot/features/object/ui/ObjectDetailScreen.kt`
- `features/object/src/main/java/com/lifepilot/features/object/viewmodel/ObjectDetailViewModel.kt`
- `domain/src/main/java/com/lifepilot/domain/usecase/UpdateObjectUseCase.kt`
- `domain/src/main/java/com/lifepilot/domain/repository/ObjectRepository.kt`
- `domain/src/main/java/com/lifepilot/domain/repository/MetadataRepository.kt`

---

## ISSUE-044 — AI proposal cards have no inline editing; user must approve or reject as-is
**Reported:** 2026-06-29 | **Priority:** P1

**Observed behaviour:**
When the AI proposes an action — creating an object, adding a task, updating a status, proposing a goal — the card shows the proposed values with Approve and Reject buttons only. If a field value is wrong (wrong date, misspelled title, incorrect domain, wrong priority), the user must reject the entire proposal, re-prompt the AI, and hope the next proposal is better. There is no way to correct a single field before accepting.

**Root cause (verified):**
All proposal card composables (`ObjectCreationCard`, `TaskCreationCard`, `GoalProposalCard`, `MetadataUpdateCard`, `StatusUpdateCard`) render proposed values as static text. Only `TaskCreationCard` has a partial exception for due date (ISSUE-012, not yet implemented). No card has an edit-before-approve pattern.

**Planned fix approach:**
1. Every proposal card that presents a named field must render that field as an editable input, not static text. Pre-filled with the AI's suggestion — the user only touches it if something is wrong.
2. `TaskCreationCard`: title (TextField), due date (date chip → DatePickerDialog), priority (segmented picker: Low / Medium / High), notes (optional TextField).
3. `ObjectCreationCard`: title (TextField), domain (dropdown sourced from SchemaEngine), object type (dropdown), description (TextField).
4. `GoalProposalCard`: title (TextField), deadline (date chip), description (TextField). Suggested tasks within the card: each task title editable, each due date editable.
5. `MetadataUpdateCard`: each proposed field value is a TextField pre-filled with extracted value. User corrects any wrong value before approving.
6. `StatusUpdateCard`: status dropdown pre-selected with AI's suggestion, user can change before approving.
7. `onApprove` for every card must collect the current (potentially user-edited) values from local state and pass them through — not the original AI-proposed values.
8. Apply generically: any future proposal card type must follow the same editable-before-approve pattern. This is the design contract for all proposal cards.

**Files involved:**
- `designsystem/src/main/java/com/lifepilot/designsystem/components/AiProposalCards.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt` — `approveAction()` must accept edited values

---

## ISSUE-050 — Understanding stored per-Object instead of per-Domain; wrong canonical level
**Reported:** 2026-06-29 | **Priority:** P1

**Observed behaviour:**
AI-derived understanding is currently stored on individual objects — `passport.context`, `resume.context`, `hdfc_account.context`. When a user asks a domain-level question ("How is my career going?", "Am I financially prepared for this trip?"), the AI assembles a fragmented picture by concatenating object-level snippets rather than consulting a single authoritative domain understanding. There is also no way for the system to reason about the domain as a whole — gaps, risks, momentum, open questions — because no such structure exists.

**Root cause (verified):**
`DomainLifeState` exists as a data model and is seeded with sample data, but it is treated as a summary display field rather than the canonical source of domain understanding. Object-level context fields exist in parallel, creating dual sources of truth. The AI prompt includes both object snapshots and domain life states, but with no clear hierarchy — the object-level data effectively drowns out the domain-level signal.

**Desired architecture:**
```
Objects → Metadata → Evidence
              ↓
        Domain Life State  (one per domain: Identity, Career, Finance, Health, Travel)
              ↓
        AI Reasoning
```

Each `DomainLifeState` becomes the single canonical understanding for that domain. Objects are evidence. They feed up into domain understanding — they do not hold understanding themselves. The AI always reasons from domain life states, using objects only as supporting citations.

**Planned fix approach:**
1. Remove any `context` or `aiSummary` fields from the `LifePilotObject` model. Objects hold structured metadata only — no prose understanding.
2. `DomainLifeState` gains richer fields: `currentSituation`, `momentum`, `openQuestions`, `knownRisks`, `nextActions`, `lastReasonedAt`, `confidenceScore`, `contributingObjectIds: List<String>`.
3. `ObjectReasoner.buildSnapshot()` stops generating prose summaries per object. It builds structured evidence packets (field name + value + verificationStatus) that feed into domain state computation.
4. `PromptBuilderImpl` provides the AI with domain life states as the primary context. Object snapshots are appended only as citations — labelled "Supporting evidence for [domain]".
5. AI responses must cite which domain life state and which object evidence they drew from, so the user can verify.
6. Cross-references ISSUE-010 (health symptoms), ISSUE-002 (task completion), ISSUE-051 (maintenance pipeline).

**Files involved:**
- `domain/src/main/java/com/lifepilot/domain/model/DomainLifeState.kt`
- `domain/src/main/java/com/lifepilot/domain/model/LifePilotObject.kt`
- `data/src/main/java/com/lifepilot/data/engine/ObjectReasoner.kt`
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`

---

## ISSUE-051 — Domain Life State maintenance pipeline not wired; life events don't update domain understanding
**Reported:** 2026-06-29 | **Priority:** P1

**Observed behaviour:**
When a task is completed, a document scanned, a conversation concluded, an object status updated, or a goal achieved, the domain life state for the affected domain does not change. The current understanding of "Career" does not update when a job interview task is completed. "Health" does not update when a symptom conversation ends. The life state goes stale as soon as the user starts actually using the app.

**Root cause (verified):**
`LifeStateEngineImpl` has no pipeline that connects life events to domain understanding updates. The following triggers exist but have no downstream domain evaluation: OCR completion, `sendMessage()` conversation turns, `completeTask()`, `archiveObject()`, `updateObjectStatus()`, reminder dismissal. This is the same gap as ISSUE-002 (task completion) but generalised across every event type.

**Planned fix approach:**
Wire every meaningful life event into a `DomainLifeStateEvaluator` within `LifeStateEngineImpl`. Trigger conditions and their domain targets:

| Trigger | Domain(s) to re-evaluate |
|---|---|
| Document scanned + OCR complete | Domain of the classified object |
| Metadata verification accepted | Domain of the updated object |
| Object status changed | Domain of the object |
| Conversation turn completed (AI response received) | All domains mentioned in the conversation |
| Task completed | Domain linked to the task's parent object or goal |
| Goal achieved | Domain linked to the goal |
| Reminder fired or dismissed | Domain of the linked object |
| New object created | Domain of the new object |

Re-evaluation does not always call the AI. For structured changes (status update, new metadata), the evaluator computes a deterministic delta and patches `DomainLifeState` directly. For unstructured changes (conversation, OCR of a novel document), it queues an AI-assisted re-evaluation that runs asynchronously and updates the domain life state when complete.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/engine/LifeStateEngineImpl.kt`
- New: `domain/src/main/java/com/lifepilot/domain/engine/DomainLifeStateEvaluator.kt`
- `features/planner/src/main/java/com/lifepilot/features/planner/viewmodel/PlannerViewModel.kt`
- `data/src/main/java/com/lifepilot/data/pipeline/DocumentProcessingPipeline.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

---

## ISSUE-052 — AI persists life events without a follow-up interview; critical context lost
**Reported:** 2026-06-29 | **Priority:** P1

**Observed behaviour:**
"I have an interview" → AI creates a Career object immediately. It does not ask which company, which role, how the user feels about it, what the next step is, or whether an offer came. The persisted record is a shell. The domain life state update is correspondingly shallow. The AI had one shot at capturing a significant life event and produced a record that could have been typed in a notes app.

**Root cause (verified):**
ISSUE-007 covers the general case of AI creating objects without clarifying. This issue is the deeper, structured version: LifePilot needs a dedicated **follow-up interview** pattern for significant life events — not just one clarifying question, but a short, contextually intelligent sequence that extracts the full picture before anything is persisted. No such pattern exists. The prompt has no instruction to identify event types that warrant an interview, and no `InterviewState` exists in the conversation model.

**Planned fix approach:**
1. Define a set of **high-value event types** that always trigger an interview: new job/interview, new medical diagnosis or symptom, new property or major purchase, new relationship milestone, new travel plan, major financial event (loan, investment, income change), legal event (visa, court, contract).
2. Add interview templates per event type to `PromptBuilderImpl`. Each template defines 3–5 follow-up questions, sequenced logically, stopping early if the user indicates they don't know or want to skip.
3. Interview questions are asked conversationally — one at a time, not as a form. The AI waits for each answer before asking the next.
4. After the interview is complete (or skipped), the AI synthesises all answers into a single `ObjectCreation` + `DomainLifeStateUpdate` proposal for the user to verify before anything is persisted.
5. Add `interviewState: InterviewState?` to `HomeUiState` — tracks which event type is being interviewed, which questions have been asked, and what answers have been collected. This state is cleared after the proposal is accepted or rejected.
6. Closely related to ISSUE-007 (clarification before creation) and ISSUE-009 (no placeholders). Those issues should be considered prerequisites. This issue is the structured, multi-turn extension of both.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt` — interview templates per event type
- New: `domain/src/main/java/com/lifepilot/domain/model/InterviewState.kt`
- `features/home/src/main/java/com/lifepilot/features/home/state/HomeUiState.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

---

## ISSUE-057 — Life Event Action Plan: cascading updates not understood, not presented, not executed sequentially
**Reported:** 2026-06-29 | **Priority:** P1
**Status: RESOLVED ✓** — Architecture and implementation done by Kimi (Sessions 2–3). `ActionPlan`, `ActionItem`, `ActionPlanType` domain models created. `ActionPlanExecutor` interface + `ActionPlanExecutorImpl` implemented. `ActionPlanCard` composable (290 lines) built. `HomeUiState.pendingActionPlan` added. `HomeViewModel.parseAction()` parses `ACTION_PLAN` JSON blocks. `PromptBuilderImpl` extended with 7th action type. `ActionPlanTemplateProvider` with JSON templates. `CHILDBIRTH` and `DEATH_OF_RELATIVE` enum values added (Claude Session 4). `UpdateDomainUnderstanding` items excluded from user-visible rows (Claude Session 4).

**This is the most important product upgrade on the roadmap.**

**Observed behaviour:**
"I joined a new job" → AI creates one Career record and stops. It does not close the old job. It does not create tasks for collecting experience letters, PF transfer, or relieving documents. It does not update the Finance domain for the salary change. It does not ask whether the city is changing. It does not generate moving tasks. It does not update Health domain for the new employer's insurance. It does not update Identity domain if the address is changing. It creates one record and considers its job done.

This is the gap between a records manager and a life operating system.

**Root cause (verified):**
The AI prompt is designed for single-action responses — one proposal per turn. There is no concept of a **cascading event** that touches multiple domains, multiple objects, and generates multiple tasks simultaneously. `HomeViewModel` handles one `AiProposal` at a time. The UI has no pattern for presenting a multi-step action plan. The AI has no instruction to reason about second-order implications of a life event.

**Desired interaction pattern:**

```
User: I joined a new job at Zepto in Bengaluru starting August 1st.

LifePilot: That's a significant change. Here's everything I can update — 
           review and confirm what applies:

┌─────────────────────────────────────────────────────────┐
│  LIFE EVENT ACTION PLAN                    4 of 7 ready │
├─────────────────────────────────────────────────────────┤
│  ✓  Close current job at Accenture                      │
│     Left date: 31 July 2025                             │
│                                                         │
│  ✓  Add task: Collect experience letter                 │
│     Due: 31 July · High priority                        │
│                                                         │
│  ✓  Add task: Initiate PF transfer (EPFO)               │
│     Due: 15 August · High priority                      │
│                                                         │
│  ✓  Create new job record: Zepto                        │
│     Role: [confirm] · Salary: [confirm] · From: Aug 1   │
│                                                         │
│  ?  Are you relocating to a different city?             │
│     Currently: Bengaluru → Zepto is also Bengaluru      │
│     → No relocation tasks added                         │
│                                                         │
│  ✓  Update Career understanding                         │
│  ✓  Update Finance understanding (salary change)        │
├─────────────────────────────────────────────────────────┤
│  [ Confirm all ]          [ Review individually ]       │
└─────────────────────────────────────────────────────────┘

Before I apply these — what role and salary should I record 
for the Zepto position?
```

The AI continues the conversation naturally after presenting the plan. The user can confirm all at once, or step through items individually to edit. The plan executes sequentially, with each completed step shown with a checkmark.

**Planned fix approach:**

**1. Life Event Classifier**
Add a `LifeEventClassifier` that detects high-impact event types from user messages: job change, relocation, marriage/divorce, new property, major medical event, new financial product, visa/immigration event, major purchase. Each event type has a registered **cascading update template**.

**2. Cascading Update Templates**
Each template defines the full set of potential updates for that event type:
- Which existing objects might need status/date updates (e.g. close old job)
- Which tasks are typically required (e.g. collect documents, transfer accounts)
- Which domains need re-evaluation (Career, Finance, Identity, Health)
- Which clarifying questions determine whether additional cascades apply (e.g. city change → relocation tasks)

Templates are configuration-driven (JSON in `SchemaEngine`), not hardcoded, so they can be extended without code changes.

**3. Life Event Action Plan UI**
New composable `LifeEventActionPlan` rendered as a special message type in the chat (not a floating modal):
- Each action item is a checkable row with editable fields inline
- Items grouped: Records to Update / Tasks to Create / Understanding to Update
- "?" items are clarifying questions that, when answered, may add or remove other items
- "Confirm all" applies every checked item in sequence
- "Review individually" steps through each item as a separate mini-proposal
- The AI continues typing below the plan card — it can answer questions and the plan remains visible and interactive above

**4. Sequential Execution**
`HomeViewModel` gains a `LifeEventActionPlan` state that holds the full list of `ActionItem` objects. Execution is sequential: each item creates the appropriate `AiProposal`, executes it, marks the item complete, then proceeds to the next. Each completion updates the relevant domain life state (ISSUE-051). The conversation remains responsive throughout — the user can continue chatting while items are being processed.

**5. Conversation continues**
After presenting the action plan, the AI asks the first clarifying question it needs (role? salary? relocation?) as a natural follow-up message. Answers update the action plan in real time — checking new items or filling in blank fields. The plan and the conversation are two parallel threads in the same UI.

**Relationship to other issues:**
This issue subsumes and extends ISSUE-007 (clarification before creation), ISSUE-052 (follow-up interview), ISSUE-022 (status not updating), ISSUE-002 (task completion → life state), and ISSUE-051 (maintenance pipeline). Those issues are prerequisites. This issue is the integrated, user-facing expression of all of them working together.

**Files involved:**
- New: `domain/src/main/java/com/lifepilot/domain/model/LifeEventActionPlan.kt`
- New: `domain/src/main/java/com/lifepilot/domain/engine/LifeEventClassifier.kt`
- New: `domain/src/main/java/com/lifepilot/domain/engine/CascadingUpdateEngine.kt`
- New: `features/home/src/main/java/com/lifepilot/features/home/ui/LifeEventActionPlanCard.kt`
- `features/home/src/main/java/com/lifepilot/features/home/state/HomeUiState.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- `data/src/main/java/com/lifepilot/data/schema/` — cascading update templates as JSON config

---

## ISSUE-085 — Search does not return conversations and task results do not redirect to Planner
**Reported:** 2026-07-02 | **Priority:** P1
**Status: RESOLVED ✓** — Fixed by Kimi (Session 3). `ConversationDao.searchConversations()` added. `SearchEntityType.CONVERSATION` added to enum. `SearchRepositoryImpl` queries conversations and maps to `SearchResult`. `SearchScreen` routes conversation taps to `onNavigateToConversation()` and task taps to Planner navigation.

**Observed behaviour:**
Search returns objects, documents, goals and tasks, but does not surface past AI conversations. Tapping a task search result does not open the task in the Planner; it either does nothing or behaves like an object result.

**Root cause (verified):**
1. `SearchRepositoryImpl.search()` queries `ObjectDao`, `MetadataDao`, `DocumentDao`, `GoalDao` and `TaskDao`, but never `ConversationDao`.
2. `SearchEntityType` has no `CONVERSATION` value, so the UI has no way to render conversation matches.
3. `SearchScreen` result click handler is wired only to object-detail navigation; task and conversation results have no dedicated routing.

**Planned fix approach:**
1. Add conversation search to `SearchRepositoryImpl`: query `ConversationDao` for title/content matches and map results to `SearchResult` with `SearchEntityType.CONVERSATION`.
2. Extend `SearchEntityType` with `CONVERSATION` and update `SearchScreen` to render conversation results with a chat-style icon and timestamp.
3. Tapping a **task** result navigates to the Planner / Tasks tab and scrolls to the selected task.
4. Tapping a **conversation** result resumes that conversation in the AI workspace (`HomeViewModel.resumeConversation`).
5. Tapping an **object** result continues to open object detail.
6. Generic application: the same result-type routing pattern should be used for any future searchable entity (e.g. reminders, notes).

**Files involved:**
- `data/src/main/java/com/lifepilot/data/repository/SearchRepositoryImpl.kt`
- `data/src/main/java/com/lifepilot/data/database/dao/ConversationDao.kt`
- `domain/src/main/java/com/lifepilot/domain/model/SearchResult.kt`
- `features/search/src/main/java/com/lifepilot/features/search/ui/SearchScreen.kt`
- `features/search/src/main/java/com/lifepilot/features/search/navigation/SearchNavigation.kt`
- `features/home/src/main/java/com/lifepilot/features/home/navigation/HomeNavigation.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- `features/planner/src/main/java/com/lifepilot/features/planner/navigation/PlannerNavigation.kt`

---

# P2 — Medium

---

## ISSUE-014 — No onboarding; Profile has no personal details; no completion indicator on Home
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
Fresh install goes straight to Home. No guidance. Profile only stores `displayName`. No completion ring on Home.

**Root cause (verified):**
`Profile.kt` has 6 fields — no personal details. Zero onboarding code exists. Home top bar shows plain name only.

**Planned fix approach:**
- Phase 1: Extend `Profile` with `fullName`, `dateOfBirth`, `profession`, `employer`, `city`, `country`, `phoneNumber`, `emergencyContactName`, `emergencyContactPhone`. Pass `profession`/`city`/`country` into `RetrievalContext`.
- Phase 2: Build `features/onboarding/` — Step 1 mandatory (name + location), Steps 2–N all skippable.
- Phase 3: Compute `completionPercentage`. Show completion ring in `DailyBriefTopBar`. Tap → first incomplete onboarding step.

**Files involved:**
- `domain/src/main/java/com/lifepilot/domain/model/Profile.kt`
- `data/src/main/java/com/lifepilot/data/engine/RetrievalEngineImpl.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`
- New: `features/onboarding/`

---

## ISSUE-017 — No first-launch "New user vs Restore from backup" screen
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
Every launch creates a new profile unconditionally. No way to restore from a previous backup on a new device.

**Root cause (verified):**
`AppInitializer.ensureDefaultProfile()` creates a profile on first launch with no "has seen welcome" flag. No `WelcomeScreen` exists.

**Planned fix approach:**
1. Add `hasCompletedWelcome: Flow<Boolean>` to `PreferenceManager`.
2. On first launch: `WelcomeScreen` — "Start fresh" → onboarding; "Restore" → file picker → `ImportDataUseCase` → Home.
3. Move profile creation from `AppInitializer` into onboarding Step 1.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/repository/PreferenceManager.kt`
- `app/src/main/java/com/lifepilot/app/initializer/AppInitializer.kt`
- `app/src/main/java/com/lifepilot/app/MainActivity.kt`
- New: `features/onboarding/WelcomeScreen.kt`

---

## ISSUE-016 — No automatic backup to Google Drive
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
Backup is manual share-only. No scheduling, no Drive upload, no restore from Drive on a new device.

**Root cause (verified):**
`ExportDataUseCase` builds JSON correctly but no `WorkManager` periodic task or Drive API integration exists. Export omits document binary files.

**Planned fix approach:**
1. `BackupWorker` — calls `ExportDataUseCase`, uploads to Drive via REST API.
2. `scheduleBackup(intervalHours)` in `WorkManagerScheduler`. Default: 24 hours.
3. "Auto-backup to Google Drive" toggle + last-backup timestamp in Settings.
4. Extend export to include document file references.
5. Implement `ImportDataUseCase` restore path.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/worker/WorkManagerScheduler.kt`
- `domain/src/main/java/com/lifepilot/domain/usecase/ExportDataUseCase.kt`
- `domain/src/main/java/com/lifepilot/domain/usecase/ImportDataUseCase.kt`
- `features/settings/src/main/java/com/lifepilot/features/settings/ui/SettingsScreen.kt`

---

## ISSUE-019 — Sensitive numbers displayed unredacted with no masking
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
Passport numbers, Aadhaar, PAN, account numbers shown in full everywhere — object detail, AI chat, Library.

**Root cause (verified):**
No redaction utility exists anywhere. Metadata values rendered verbatim from DB.

**Planned fix approach:**
1. `SensitiveFieldRegistry` — config-driven list of `fieldId` patterns requiring masking.
2. `RedactionUtil.mask(fieldId, value)` — standard masking per field type.
3. `ObjectDetailScreen`: masked by default, per-field "👁 Reveal" toggle with optional biometric re-auth.
4. AI response bubbles: regex-detect sensitive patterns, replace with masked form + "🔓 Show full" chip.
5. `PromptBuilderImpl`: redact sensitive fields before sending to AI API.

**Files involved:**
- New: `domain/src/main/java/com/lifepilot/domain/security/SensitiveFieldRegistry.kt`
- New: `domain/src/main/java/com/lifepilot/domain/security/RedactionUtil.kt`
- `features/object/src/main/java/com/lifepilot/features/object/ui/ObjectDetailScreen.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`

---

## ISSUE-003 — Goals and Tasks are separate; Tasks should nest inside Goals
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
Goals and Tasks are parallel with no parent-child relationship. No way to attach tasks to a goal.

**Root cause (verified):**
`Goal.kt` and `Task.kt` flat models, no `goalId` foreign key on `Task`. Planner renders both separately.

**Planned fix approach:**
1. Add optional `goalId: String?` to `Task` + DB migration.
2. Update `PlanningEngine.createTask()` to accept `goalId?`.
3. `GoalProposal` approval: create goal first, then create each suggested task linked to the new goal ID.
4. Planner UI: goals as expandable containers with child tasks; standalone tasks (no goalId) grouped below.
5. Remove any separate "Tasks" navigation item.

**Files involved:**
- `domain/src/main/java/com/lifepilot/domain/model/Task.kt`
- `data/src/main/java/com/lifepilot/data/database/entity/TaskEntity.kt`
- `domain/src/main/java/com/lifepilot/domain/engine/PlanningEngine.kt`
- `features/planner/src/main/java/com/lifepilot/features/planner/`

---

## ISSUE-013 — Domain Understanding card has no collapse; grows unbounded
**Reported:** 2026-06-29 | **Priority:** P2
**Status: RESOLVED ✓** — Fixed by Claude (Session 4). Domain Understanding card is now dismissable per-session via an × button. Does not reappear until next app launch.

**Observed behaviour:**
As AI accumulates knowledge, the understanding card text grows with no truncation, eventually dominating the Library screen.

**Root cause (verified):**
`DomainUnderstandingCard` renders `currentSituation` as plain `Text()` with no `maxLines` and no expand/collapse state.

**Planned fix approach:**
`var expanded by remember { mutableStateOf(false) }`. Collapsed: `maxLines = 3, overflow = Ellipsis` + "Read more →". Expanded: full text + "Show less". First recommendation visible only when collapsed (as a preview nudge).

**Files involved:**
- `features/library/src/main/java/com/lifepilot/features/library/ui/LibraryScreen.kt` — `DomainUnderstandingCard()`, line 404

---

## ISSUE-012 — Task creation card has no editable due date before approving
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
AI-suggested due date on task card is read-only. User cannot change it before approving.

**Root cause (verified):**
`TaskCreationCard` takes `dueDate: String?` as display-only. `onApprove: () -> Unit` takes no parameters.

**Planned fix approach:**
1. Add `onDueDateChange: (LocalDate?) -> Unit` + local date state to `TaskCreationCard`.
2. Tappable chip opens `DatePickerDialog`.
3. `onApprove: (overrideDueDate: LocalDate?) -> Unit`.
4. `HomeViewModel.approveAction()` uses override date when provided.
5. Same pattern for `GoalProposalCard` deadline.

**Files involved:**
- `designsystem/src/main/java/com/lifepilot/designsystem/components/AiProposalCards.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

---

## ISSUE-006 — "Ask AI about this" discards object context; candidate for removal
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
Navigates to Home AI workspace with zero object context passed.

**Root cause (verified):**
`ObjectNavigation.kt` line 39: `{ _, _, _ -> navController.navigate("home") }` — all parameters discarded.

**Planned fix approach:**
Remove the button entirely. If deep-link context is needed later, implement via `HomeViewModel.setObjectContext(objectId)` threaded into the system prompt — not a dead navigation event.

**Files involved:**
- `features/object/src/main/java/com/lifepilot/features/object/navigation/ObjectNavigation.kt`
- `features/object/src/main/java/com/lifepilot/features/object/ui/ObjectDetailScreen.kt`

---

## ISSUE-023 — No AI permission settings; all proposals require manual approval
**Reported:** 2026-06-29 | **Priority:** P2
**Status: RESOLVED ✓** — Fixed by Kimi via ISSUE-066 implementation. Tiered AI approval: `PREFERENCE` sensitivity auto-executes silently, `STANDARD` shows compact chip, `SENSITIVE` always shows full approval card. `FieldSensitivity` enum in schema JSON drives the tier.

**Observed behaviour:**
No way to grant the AI standing permission for specific action types or permanently restrict others.

**Root cause (verified):**
`PreferenceManager` has no AI permission keys. `HomeViewModel` unconditionally sets `pendingAction` for every proposal.

**Planned fix approach:**
1. `AiPermission` enum: `AUTO_UPDATE_RECORDS`, `AUTO_ADD_TASKS`, `AUTO_COMPLETE_TASKS`, `AUTO_CREATE_GOALS`, `AUTO_UPDATE_STATUS`. Deletion always requires manual approval.
2. Store in `PreferenceManager` as `Set<String>` DataStore key.
3. "AI Permissions" section in Settings with per-permission toggles.
4. `HomeViewModel`: check auto-approve before setting `pendingAction`. If granted, call `executeProposal()` directly + post confirmation message.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/repository/PreferenceManager.kt`
- `features/settings/src/main/java/com/lifepilot/features/settings/ui/SettingsScreen.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

---

## ISSUE-032 — Notification taps open Home, not the relevant object or reminder
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
Tapping a reminder notification (e.g. "Passport renewal due") opens the app to the Home screen. There is no navigation to the relevant object. The user has to manually navigate to Library → Identity → Indian Passport to see the context.

**Root cause (verified):**
`NotificationHelper.buildNotification()` sets the `contentIntent` to a generic `MainActivity` launch `PendingIntent` with no extras. `WorkManagerScheduler`'s reminder evaluation worker does not pass `objectId` or `reminderId` into the notification payload. `MainActivity` has no deep-link handler.

**Planned fix approach:**
1. Pass `objectId` and `reminderId` as extras in the notification `PendingIntent`.
2. Add a deep-link handler in `MainActivity` — on launch, check intent extras. If `objectId` is present, navigate to `ObjectDetailScreen` after the app finishes initialisation.
3. Wire the same deep-link contract into the app's navigation graph via `NavDeepLink` so the path works whether the app is cold-started or resumed from background.
4. Apply generically: task due reminders → Planner at the relevant task. Morning brief notification → Home Daily Brief tab. Goal deadline reminder → the relevant goal in Planner.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/notification/NotificationHelper.kt`
- `data/src/main/java/com/lifepilot/data/worker/ReminderEvaluationWorker.kt`
- `app/src/main/java/com/lifepilot/app/MainActivity.kt`
- `app/src/main/java/com/lifepilot/app/navigation/AppNavigation.kt`

---

## ISSUE-033 — No in-app document viewer; attached files open in external apps
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
Documents attached to objects have no in-app viewer. Tapping a document either does nothing or launches an external PDF/image app. The user leaves LifePilot to view a document they stored in LifePilot. For an app that positions itself as the operating system for personal documents, this is a fundamental gap.

**Root cause (verified):**
`ObjectDetailScreen` document row click handler calls `context.startActivity(Intent.ACTION_VIEW)` with the file URI. No in-app viewer composable exists. There is no `DocumentViewerScreen` in the navigation graph.

**Planned fix approach:**
1. Add `DocumentViewerScreen` to the navigation graph, receiving a `documentId`.
2. For PDFs: use `PdfRenderer` (Android built-in, no library required) to render pages as `Bitmap` into a zoomable `Image` composable (`rememberTransformableState`). Support swipe between pages.
3. For images (JPG/PNG): use `Coil` (already a dependency) in a fullscreen zoomable view.
4. Top bar: document title, share button (exports via `FileProvider`), and a back button.
5. For sensitive documents (passport, Aadhaar, financial): apply biometric re-auth gate before the viewer opens, consistent with ISSUE-019 redaction work.
6. Documents remain stored in the app's internal files directory — `FileProvider` is used for any share action, never raw path exposure.

**Files involved:**
- New: `features/object/src/main/java/com/lifepilot/features/object/ui/DocumentViewerScreen.kt`
- `features/object/src/main/java/com/lifepilot/features/object/navigation/ObjectNavigation.kt`
- `features/object/src/main/java/com/lifepilot/features/object/ui/ObjectDetailScreen.kt`

---

## ISSUE-028 — Verified vs Unverified metadata distinction not visible in the UI
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
Metadata fields exist with `verificationStatus = VERIFIED` or `UNVERIFIED`, and a `confidence` score, but the UI renders all fields identically. There is no way to tell at a glance which values came from OCR extraction (unverified) vs were explicitly confirmed by the user (verified). This erodes trust in the data shown.

**Root cause (verified):**
`ObjectDetailScreen` renders metadata rows as plain key-value text with no visual differentiation by `verificationStatus`. The `confidence` and `verificationStatus` fields are fetched and mapped in the ViewModel but discarded before the UI layer.

**Planned fix approach:**
1. Unverified fields: render with a subtle amber dot or "?" indicator and a de-emphasised text colour. Tapping the row shows a brief tooltip: "Extracted automatically — tap to verify".
2. Verified fields: no indicator needed — clean rendering is itself the signal. Optionally a small checkmark on explicit user-confirmed fields.
3. Add a "Verify" action directly on each unverified row: tapping it opens an inline edit field pre-filled with the current value. On save, `verificationStatus` is updated to `VERIFIED` and `confidence` set to 1.0.
4. In the AI context snapshot (`ObjectReasoner.buildSnapshot()`), prefix unverified field values with `[unverified]` so the AI can express appropriate hedging ("your passport number on file is Z4921835, though this was extracted automatically and hasn't been confirmed").
5. Apply generically: the same verification UI applies to every metadata field across all object types.

**Files involved:**
- `features/object/src/main/java/com/lifepilot/features/object/ui/ObjectDetailScreen.kt`
- `data/src/main/java/com/lifepilot/data/engine/ObjectReasoner.kt`
- `designsystem/src/main/java/com/lifepilot/designsystem/components/MetadataRow.kt`

---

## ISSUE-029 — Blank message bubble appears after accepting a Record saved event
**Reported:** 2026-06-29 | **Priority:** P2
**Status: RESOLVED ✓** — Fixed by Claude (Session 4). When `visibleContent` is blank after stripping action blocks, `displayContent` falls back to `action?.summary` rather than creating an empty bubble. Skips the message entirely if both are blank.

**Observed behaviour:**
After the user taps "Accept" on a Record saved / Object created proposal card, a blank empty bubble appears between the original user question and the "✓ Record saved" confirmation message.

**Root cause (verified):**
`HomeViewModel.approveAction()` calls `sendMessage("")` or equivalent to trigger a follow-up AI turn after approval, passing an empty string as the user message. This empty message is persisted to the conversation and rendered as a blank bubble before the AI's confirmation response arrives. The confirmation message itself arrives separately and appears below the blank bubble.

**Planned fix approach:**
1. Do not call `sendMessage()` with an empty string after approving a proposal. Approval is not a user message — it is a system event.
2. After `executeProposal()` succeeds, directly append a system confirmation message to the conversation using a new `appendSystemMessage(text)` function on `ConversationRepository`. This does not go through the AI — it is a local, instant update.
3. The confirmation message should briefly describe what was saved: "Record saved: Indian Passport added to Identity." Style it differently from user messages and AI responses (e.g. smaller, centred, de-emphasised) so it is visually distinct.
4. Apply generically: the same pattern applies to all proposal approvals (task creation, goal creation, status update, domain life state update) — none of them should produce blank bubbles or require an unnecessary AI round-trip to confirm local state changes.

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt` — `approveAction()`
- `data/src/main/java/com/lifepilot/data/repository/ConversationRepositoryImpl.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt` — system message bubble style

---

## ISSUE-037 — Tasks and goals not editable after creation; no way to change title, notes, or due date
**Reported:** 2026-06-29 | **Priority:** P2
**Status: RESOLVED ✓** — Fixed by Claude (Session 5). `TaskDetailSheet` and `GoalDetailSheet` bottom sheets created. `PlanningEngine.updateTask()` + `TaskRepository.updateTask()` implemented. `PlannerViewModel` wired with `openEditTask/closeEditTask/saveEditedTask/openEditGoal/closeEditGoal/saveEditedGoal`. Tapping a task or goal opens the edit sheet. `rescheduleTask` no-op also fixed.

**Observed behaviour:**
Once a task or goal is created (via AI proposal or manually), there is no way to edit its title, notes, due date, or priority. The only action available is completion or deletion. If the AI suggested the wrong date or a task description needs updating, there is no path to correct it.

**Root cause (verified):**
`PlannerScreen` renders tasks and goals as read-only rows. No edit mode or swipe-to-edit action exists. `TaskCreationCard` allows a due-date override before creation (see ISSUE-012) but there is no equivalent for already-created tasks. `planningEngine.updateTask()` may not exist.

**Planned fix approach:**
1. Tap on any task row → opens a `TaskDetailSheet` (bottom sheet) showing: title (editable), notes (editable), due date (date picker), priority (picker: None / Low / Medium / High), linked goal (if any), linked object (if any).
2. Changes saved via `planningEngine.updateTask()`. If `updateTask()` does not exist, add it.
3. Tap on a goal → opens `GoalDetailSheet`: title (editable), description (editable), deadline (date picker), linked tasks list with add/remove capability.
4. Swipe-left on a task row reveals: Edit (opens sheet) / Complete / Delete.
5. Apply generically: editing must work for both AI-created and manually-created tasks and goals.

**Files involved:**
- `features/planner/src/main/java/com/lifepilot/features/planner/ui/PlannerScreen.kt`
- New: `features/planner/src/main/java/com/lifepilot/features/planner/ui/TaskDetailSheet.kt`
- New: `features/planner/src/main/java/com/lifepilot/features/planner/ui/GoalDetailSheet.kt`
- `features/planner/src/main/java/com/lifepilot/features/planner/viewmodel/PlannerViewModel.kt`
- `domain/src/main/java/com/lifepilot/domain/engine/PlanningEngine.kt`

---

## ISSUE-038 — Planner has no filter; tasks for different timeframes and priorities mixed together
**Reported:** 2026-06-29 | **Priority:** P2
**Status: RESOLVED ✓** — Already implemented. `FilterChip` row with Today / This Week / All / Completed filters exists in `PlannerScreen`. Filter logic in `PlannerViewModel.filterTasks()`. Confirmed by Claude (Session 4).

**Observed behaviour:**
The Planner (Goals + Tasks) shows all items in a single undifferentiated list. With any real usage — multiple goals, dozens of tasks — the list becomes unmanageable. There is no way to focus on what needs attention today, this week, or by priority.

**Root cause (verified):**
`PlannerScreen` collects all tasks from `planningEngine.observeTasks()` with no filter parameters. `PlannerUiState` has no `selectedFilter` or `selectedPriority` field. No `FilterChip` row exists in the UI.

**Planned fix approach:**
1. Add a sticky `FilterChip` row at the top of the Planner (below the section title, above the goal/task list).
2. **Date filter chips:** All · Today · This Week · This Month. "Today" = tasks with `dueDate == today`. "This Week" = due within 7 days. "This Month" = due within 30 days. "All" = no filter.
3. **Priority filter chips** (second row, shown when tasks have priority set): All · High · Medium · Low. These are additive — both date and priority filters apply simultaneously.
4. Add `selectedDateFilter: DateFilter` and `selectedPriorityFilter: PriorityFilter?` to `PlannerUiState`. Filter logic lives in the ViewModel, not the composable.
5. Persist last-used filter in `PreferenceManager` so it survives navigation.
6. Apply generically: date and priority filters apply to both standalone tasks and tasks nested inside goals.

**Files involved:**
- `features/planner/src/main/java/com/lifepilot/features/planner/ui/PlannerScreen.kt`
- `features/planner/src/main/java/com/lifepilot/features/planner/state/PlannerUiState.kt`
- `features/planner/src/main/java/com/lifepilot/features/planner/viewmodel/PlannerViewModel.kt`
- `data/src/main/java/com/lifepilot/data/repository/PreferenceManager.kt`

---

## ISSUE-039 — Tasks have no priority field; no visual weight between urgent and low-importance items
**Reported:** 2026-06-29 | **Priority:** P2
**Status: RESOLVED ✓** — Already implemented. `TaskPriority` enum (URGENT/HIGH/MEDIUM/LOW) exists on `Task` domain model and `TaskEntity`. Priority colour coding in `PlannerScreen` task rows. Priority `FilterChip` in `TaskDetailSheet`. Confirmed by Claude (Session 4).

**Observed behaviour:**
All tasks look identical regardless of urgency. "Renew passport before travel" and "organise laptop desktop" appear with the same visual weight. There is no priority field, no colour coding, and no way for the AI to propose a task with explicit priority.

**Root cause (verified):**
`Task` domain model has no `priority` field. `TaskEntity` has no `priority` column. `AiProposal.TaskCreation` has no priority attribute. The prompt does not instruct the AI to assess task urgency.

**Planned fix approach:**
1. Add `priority: TaskPriority` to the `Task` domain model. `TaskPriority` enum: `NONE`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. Add Room migration.
2. Update `AiProposal.TaskCreation` to include `priority: TaskPriority`. Update `parseAction()` to map AI-produced priority strings (e.g. "high", "urgent", "critical") to the enum.
3. Prompt instruction: when proposing a task, always include a priority assessment based on context (deadline proximity, linked object expiry, user safety, financial impact).
4. Visual treatment in `PlannerScreen` task rows:
   - `CRITICAL`: left border accent in red (`#EF4444`), bold title
   - `HIGH`: left border in amber (`#F59E0B`)
   - `MEDIUM`: left border in blue (`#6A5AE0`)
   - `LOW` / `NONE`: no border accent, normal weight
5. Priority badge shown inside `TaskDetailSheet` and `TaskCreationCard`.
6. Apply generically: priority must flow through the entire pipeline — AI proposal → verification card → task row → filter chip (see ISSUE-038) → morning brief ordering.

**Files involved:**
- `domain/src/main/java/com/lifepilot/domain/model/Task.kt`
- `data/src/main/java/com/lifepilot/data/database/entity/TaskEntity.kt`
- `domain/src/main/java/com/lifepilot/domain/model/ProposedAction.kt`
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- `features/planner/src/main/java/com/lifepilot/features/planner/ui/PlannerScreen.kt`
- `designsystem/src/main/java/com/lifepilot/designsystem/components/AiProposalCards.kt`

---

## ISSUE-040 — Export only outputs JSON; documents, search history, and tasks excluded
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
The export bundle contains only a JSON representation of objects and metadata. All attached documents (PDFs, scanned images) are left behind. Search history is not included. Tasks and goals are not included. An export that cannot fully restore the user's state on a new device is not a real backup.

**Root cause (verified):**
`ExportDataUseCase` serialises objects and metadata to JSON but has no file-copy step. `SearchRepository` (if it exists) has no export hook. `PlanningEngine` task/goal data is not included in the bundle. The import path (`ImportDataUseCase`) correspondingly cannot restore documents.

**Planned fix approach:**
1. Extend `ExportDataUseCase` to produce a ZIP archive rather than a flat JSON file.
2. ZIP structure:
   ```
   lifepilot_backup_YYYYMMDD.zip
   ├── data.json          (all objects, metadata, reminders, domain life states)
   ├── tasks.json         (all goals and tasks with priority, dates, completion status)
   ├── search_history.json (recent search queries and result summaries)
   ├── conversations.json  (AI conversation history)
   └── documents/
       └── <objectId>/
           └── <filename>.pdf
   ```
3. Document copy: iterate all `Document` records, copy each file from internal storage into the `documents/<objectId>/` folder within the ZIP.
4. Update `ImportDataUseCase` to unzip, restore documents to internal storage, and re-create all entities from JSON.
5. Update backup filename to use timestamp. Update Drive upload (ISSUE-016) to upload the ZIP, not the JSON.
6. Add a "What's included" summary in the export UI: "X objects, Y documents (Z MB), tasks, search history, conversations."

**Files involved:**
- `domain/src/main/java/com/lifepilot/domain/usecase/ExportDataUseCase.kt`
- `domain/src/main/java/com/lifepilot/domain/usecase/ImportDataUseCase.kt`
- `features/settings/src/main/java/com/lifepilot/features/settings/ui/SettingsScreen.kt`

---

## ISSUE-045 — No download-as-PDF or direct share for documents stored in the app
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
Documents stored in LifePilot — whether uploaded by the user or scanned via AI chat — have no download or share action. There is no way to export a specific document as a PDF, send it via WhatsApp/email, or save it to Files. The document is trapped inside the app.

**Root cause (verified):**
`ObjectDetailScreen` document row has no share or download action. Documents stored as non-PDF (JPEG, PNG, HEIC) have no conversion step. `FileProvider` is not configured in the manifest to expose internal storage files for sharing. No `DownloadDocumentUseCase` or share intent exists.

**Planned fix approach:**
1. Every document row in `ObjectDetailScreen` gets a share icon button (or long-press context menu: **View · Download as PDF · Share**).
2. **Download as PDF**: if the stored file is already a PDF, copy it to the user's Downloads folder via `MediaStore`. If it is an image, convert to PDF using `PdfDocument` (Android built-in) before writing.
3. **Share**: wrap the file in a `FileProvider` URI and launch `Intent.ACTION_SEND`. Android's share sheet handles the destination (WhatsApp, email, AirDrop via Nearby Share, etc.).
4. Same share/download actions available from within `DocumentViewerScreen` (ISSUE-033) via a top-bar icon.
5. For documents surfaced inside AI chat responses (when an AI answer references an attached document): add a small share chip below the relevant bubble.
6. All share actions go through `FileProvider` — never expose raw internal file paths.

**Files involved:**
- `features/object/src/main/java/com/lifepilot/features/object/ui/ObjectDetailScreen.kt`
- New: `domain/src/main/java/com/lifepilot/domain/usecase/ShareDocumentUseCase.kt`
- New: `domain/src/main/java/com/lifepilot/domain/usecase/DownloadDocumentAsPdfUseCase.kt`
- `app/src/main/AndroidManifest.xml` — FileProvider declaration
- `app/src/main/res/xml/file_provider_paths.xml`

---

## ISSUE-053 — App uses technical language throughout; non-technical users will not understand it
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
The app uses the word "Object" to refer to a passport, a job, or an insurance policy — a term that means nothing to a non-technical user. "Domain" is used where "Life Area" or "Category" belongs. "Metadata" appears in UI labels. "Verification Status" is exposed raw. "Life State" is technical jargon. A person who is not a software engineer should be able to use LifePilot without ever learning its internal vocabulary.

**Root cause (verified):**
Internal domain model names (`LifePilotObject`, `DomainLifeState`, `MetadataField`) leaked directly into UI strings throughout the codebase. No user-facing string layer or terminology mapping exists. The design system has no content guidelines.

**Planned fix approach:**
Full terminology audit and replacement. Every user-facing string must go through a plain-language filter:

| Technical term | Replace with |
|---|---|
| Object | Record |
| Domain | Life Area |
| Metadata | Details |
| Metadata field | Detail |
| Verification Status | Confirmed / Unconfirmed |
| Life State | Understanding |
| Domain Life State | [Life Area] Overview |
| Attention Required | Needs attention |
| OCR | (never shown to user) |
| Pipeline | (never shown to user) |
| Schema | (never shown to user) |

Every screen label, empty state, error message, AI response instruction, onboarding copy, and notification text must be audited against this table. Create a `strings.xml` content pass as a separate PR after the audit. Going forward, all new UI copy must be reviewed against these guidelines before merging.

**Files involved:**
- All `features/*/ui/` screen composables — string audit
- `app/src/main/res/values/strings.xml`
- New: `CONTENT_GUIDELINES.md` — plain-language rules for all future UI copy

---

## ISSUE-056 — AI chat missing standard messaging interactions: copy, select, regenerate, edit
**Reported:** 2026-06-29 | **Priority:** P2

**Observed behaviour:**
The AI chat screen has no way to copy a message, select text within a message, long-press for a context menu, select all, regenerate a response, edit a sent message and re-send, or give feedback on a response. The experience is passive compared to any mainstream chat product.

**Planned fix approach:**
Implement the following interactions in priority order:

*Long-press context menu (both user and AI messages):*
- **Copy** — copies full message text to clipboard. Brief "Copied" toast.
- **Copy selection** — enters text selection mode on that bubble; Android selection toolbar (Copy / Share / Select All) appears.
- **Share** — system share sheet with message text.
- **Add to record** — pre-fills an `AiProposal.MetadataUpdate` with the message content for the user to attach to an object.

*AI message action row (icon strip below each AI bubble, shown on tap):*
- **Regenerate** — re-sends the preceding user message, discards and replaces the current AI response, streams a new one.
- **Thumbs up / Thumbs down** — local feedback flag stored on the message. Never sent to any server automatically.

*User message actions (long-press):*
- **Edit and resend** — opens the message in the input field pre-filled. Submitting truncates all messages after that point (prompt: "This will remove messages after this point — continue?") and re-sends.

*Conversation-level actions (header overflow menu):*
- **Copy full conversation** — entire thread as plain text.
- **Select messages** — checkboxes appear on each bubble; action bar shows "Copy selected" and "Share selected".
- **Clear conversation** — with confirmation; consistent with ISSUE-001 fix.

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/ui/components/MessageBubble.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/AiChatScreen.kt`
- `features/home/src/main/java/com/lifepilot/features/home/state/HomeUiState.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

---

# P3 — Low

---

## ISSUE-008 — AI is reactive, not proactive or anticipatory
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
AI only answers the question asked. Does not surface implications or flag gaps unprompted.

**Root cause (verified):**
Prompt contains no instruction to reason forward from domain life states or reminders after answering.

**Planned fix approach:**
Add a "proactive reasoning" instruction block to `PromptBuilderImpl`: after answering, scan domain life states and upcoming reminders for imminently relevant items and surface them unprompted within a configurable urgency window.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- `data/src/main/java/com/lifepilot/data/engine/RetrievalEngineImpl.kt`

---

## ISSUE-009 — AI uses placeholders when information is missing instead of asking
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
When information is absent, AI uses generic assumptions or placeholders rather than asking the user with skip options.

**Root cause (verified):**
No information-gap detection in the prompt. No `SkipPreference` model exists.

**Planned fix approach:**
1. Prompt rule: when a gap is detected, emit `[ASK]` — never use placeholders.
2. `SkipPreference` model: `(profileId, fieldId, scope: SKIP_ONCE | SKIP_FOREVER)`.
3. Wire skip-forever preferences into `RetrievalContext`.
4. Surface three options in AI response: provide info / skip for now / skip forever.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`
- New: `domain/src/main/java/com/lifepilot/domain/model/SkipPreference.kt`

---

## ISSUE-020 — No two-stage retrieval; all objects loaded and sent on every query
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
Latency and token usage grow linearly with user data. All objects scored on every query.

**Root cause (verified):**
Single-pass scoring in `RetrievalEngineImpl`. No intent classification, no domain pre-filter.

**Planned fix approach:**
Stage 1: local keyword/regex intent classifier (no API call) → outputs domain set + query type. Stage 2: focused DB query for those domains only, then full LLM call with smaller context. Stage 1 can be upgraded to on-device model later without touching Stage 2.

**Files involved:**
- New: `domain/src/main/java/com/lifepilot/domain/engine/QueryIntentClassifier.kt`
- `data/src/main/java/com/lifepilot/data/engine/RetrievalEngineImpl.kt`

---

## ISSUE-005 — Search placeholder is too generic
**Reported:** 2026-06-29 | **Priority:** P3
**Status: RESOLVED ✓** — Fixed by Kimi. Placeholder changed to `"Search objects, documents, tasks..."` and empty-state title changed to `"Search your life"` in `SearchScreen.kt`.

**Observed behaviour:**
"Search for anything..." gives no indication of what is actually searchable.

**Root cause (verified):**
`SearchScreen.kt` line 64. Repository searches: object titles, metadata values, document names, goal titles, active task titles.

**Planned fix approach:**
Update placeholder to "Search objects, documents, metadata…". Add a subtitle listing searchable categories. Update whenever new entity types are added.

**Files involved:**
- `features/search/src/main/java/com/lifepilot/features/search/ui/SearchScreen.kt` — line 64

---

## ISSUE-034 — Timeline canonical entity not implemented; life events have no chronological record
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
The CLAUDE.md spec defines `Timeline` as a canonical entity. The Life State Engine pipeline (Document Upload → OCR → ... → Timeline Entry → Task Generation → ...) explicitly includes a Timeline step. No Timeline screen, entity, or entry creation exists in the codebase. Significant life events — new job, document uploaded, visa approved, task completed — are silently lost from history.

**Root cause (verified):**
`Timeline` appears in the canonical entity list in the spec but has no `TimelineEntity`, `TimelineRepository`, `TimelineDAO`, or `TimelineScreen`. The Life State Engine pipeline does not emit timeline entries at any point. The `LifeStateEngineImpl` has no reference to a timeline component.

**Planned fix approach:**
1. Add `TimelineEntry` domain model: `(entryId, profileId, objectId?, domain?, eventType: DOCUMENT_ADDED | STATUS_CHANGED | TASK_COMPLETED | REMINDER_FIRED | GOAL_ACHIEVED | AI_INSIGHT | USER_NOTE, title, summary, occurredAt, metadata: Map<String,String>)`.
2. `TimelineRepository` + Room entity + DAO.
3. Wire `TimelineRepository.addEntry()` into the Life State Engine at every meaningful pipeline step: document upload, metadata verification, object status change, task completion, goal achievement, reminder dismissal.
4. `TimelineScreen` accessible from bottom nav: reverse-chronological list, grouped by date, filterable by domain. Each entry tappable → relevant object or task.
5. Home Daily Brief "What changed?" section sources from the last N timeline entries, not from AI inference.
6. Timeline entries are included in export bundles and backup.

**Files involved:**
- New: `domain/src/main/java/com/lifepilot/domain/model/TimelineEntry.kt`
- New: `domain/src/main/java/com/lifepilot/domain/repository/TimelineRepository.kt`
- New: `data/src/main/java/com/lifepilot/data/database/entity/TimelineEntryEntity.kt`
- `data/src/main/java/com/lifepilot/data/engine/LifeStateEngineImpl.kt`
- New: `features/timeline/`

---

## ISSUE-030 — AI responses open with "Based on Provided data" — generic and unnatural
**Reported:** 2026-06-29 | **Priority:** P3
**Status: RESOLVED ✓** — Fixed by Claude (Session 4). Explicit RESPONSE STYLE instruction block added to `PromptBuilderImpl`: never open with "Based on your data"/"According to the information" variants. Must address user directly. Natural attribution phrasing examples provided.

**Observed behaviour:**
Almost every AI response begins with "Based on Provided data, ..." which is robotic, technically exposed (leaks the prompt mechanism), and inaccurate (LifePilot uses a retrieval system, not raw user-provided data).

**Root cause (verified):**
The system prompt in `PromptBuilderImpl` does not include any instruction on response tone or opening phrasing. The model defaults to a generic preamble it learned during training. No few-shot examples of the expected response style are included.

**Planned fix approach:**
Add a response style instruction block to the system prompt:
- Never open with "Based on provided data", "According to the information you've shared", or any variant that exposes the retrieval mechanism.
- Address the user directly and naturally, as a knowledgeable personal assistant that has access to their life records.
- If attribution is needed, use natural phrasing: "Your passport shows...", "Looking at your records...", "From what LifePilot has on file...", "I can see that your...".
- Include 2–3 few-shot response examples in the system prompt demonstrating the expected tone.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`

---

## ISSUE-054 — No UI polish pass; missing transitions, animations, and enterprise-grade finish
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
Screens appear and disappear without transitions. Lists load without any entrance animation. Cards are static. The loading skeleton is functional but mechanical. There is no shared element transition between Library and Record detail. Navigation feels abrupt. The overall experience is technically correct but not emotionally engaging — it does not feel like software someone would trust with their most important personal documents.

**Root cause (verified):**
No animation specification exists. Compose `AnimatedVisibility`, `animateContentSize`, `SharedTransitionLayout`, and `NavHost` transition overrides have not been applied systematically. The design system has no motion guidelines.

**Planned fix approach:**
Full animation and polish pass across all screens. Specific items:
1. **Navigation transitions**: `NavHost` custom enter/exit with `slideInHorizontally` + `fadeIn`. Back navigation: reverse direction. Modal sheets: `slideInVertically`.
2. **List entrance**: `LazyColumn` items use `AnimatedVisibility` with staggered `fadeIn` + `slideInVertically(initialOffsetY = { it / 4 })` on first load.
3. **Shared element transitions**: Library card → Record detail. The record title and domain chip animate position across the navigation boundary using `SharedTransitionLayout`.
4. **Card interactions**: `animateContentSize()` on expanding cards. Subtle `scale(0.97f)` press feedback on tappable cards via `interactionSource`.
5. **Skeleton shimmer**: replace static grey blocks with a shimmer animation (`infiniteTransition` + `linearGradient` sweep).
6. **AI typing indicator**: animated dots already exist — ensure consistent use across all AI loading states.
7. **Proposal card entrance**: cards slide up from below with `spring(dampingRatio = Spring.DampingRatioMediumBouncy)`.
8. **Success states**: record saved confirmation uses a brief checkmark `scale` animation before the system message appears.
9. **Error states**: shake animation on invalid input fields.
10. All animations must respect `LocalAccessibilityManager.current.isAnimationEnabled` — disabled when the user has reduced motion turned on in system settings.

**Files involved:**
- `designsystem/src/main/java/com/lifepilot/designsystem/` — motion guidelines + reusable animation composables
- All `features/*/ui/` screen composables
- `app/src/main/java/com/lifepilot/app/navigation/AppNavigation.kt` — transition overrides

---

## ISSUE-041 — Keyword-only search; no vector/semantic retrieval for conceptually related content
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
Search and AI retrieval are keyword-only. Searching "health cover" does not return the "Star Health Insurance" record. Asking "what protection do I have if I get sick abroad?" does not retrieve the insurance policy because no tokens overlap. Semantically related content is invisible to the retrieval engine.

**Root cause (verified):**
`RetrievalEngineImpl` uses a single-pass keyword scorer — token intersection between query and object field values. No embedding model, no vector index, no semantic similarity computation exists anywhere in the codebase.

**Planned fix approach:**
1. Add a local embedding model to produce vector representations of object snapshots at index time. Recommended: `all-MiniLM-L6-v2` ONNX (22 MB, runs on-device, no network required).
2. Store embeddings in a `vector_index` table: `(objectId, fieldHash, embedding BLOB)`. Recompute on object update.
3. At query time: embed the user query → cosine similarity against all stored vectors → merge top-K semantic results with existing keyword results (reciprocal rank fusion).
4. Two-stage retrieval (ISSUE-020) plugs in here: Stage 1 narrows by domain/intent, Stage 2 runs both keyword + vector within that domain.
5. Search UI: no change required — results improve transparently.
6. AI retrieval: `RetrievalEngineImpl` uses the same merged results for context building.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/engine/RetrievalEngineImpl.kt`
- New: `data/src/main/java/com/lifepilot/data/engine/VectorIndexEngine.kt`
- New: `data/src/main/java/com/lifepilot/data/database/entity/VectorIndexEntity.kt`
- New: `data/src/main/java/com/lifepilot/data/ml/EmbeddingModel.kt`

---

## ISSUE-047 — Export re-writes entire ZIP on every backup; no delta/incremental update
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
Every export or scheduled backup re-creates the entire ZIP archive from scratch — re-copying all documents, re-serialising all JSON, re-uploading to Drive. On a device with 50+ documents, this is slow, battery-intensive, and wastes Drive quota by uploading unchanged files repeatedly.

**Root cause (verified):**
`ExportDataUseCase` has no change-tracking mechanism. Every run is a full rebuild. No `lastExportedAt` timestamp is compared against `updatedAt` on entities. Drive upload replaces the entire file.

**Planned fix approach:**
1. Track a `lastExportedAt` timestamp in `PreferenceManager`.
2. On backup run, query only entities modified after `lastExportedAt`: changed objects, metadata, tasks, goals, reminders, conversations, and documents.
3. Maintain a **manifest file** inside the ZIP (`manifest.json`): maps each `objectId` and `documentId` to its `contentHash` and `lastModified`. On the next backup run, diff the current state against the manifest — only re-export entries where `contentHash` differs.
4. For Drive: store the ZIP with a stable filename (`lifepilot_backup.zip`). Use Drive's partial update API (PATCH with `Content-Range`) to replace only changed entries within the ZIP, or maintain a sidecar `manifest.json` in Drive and only re-upload changed document files individually.
5. On `ImportDataUseCase`, the manifest enables smart restore: skip files already present with matching hash, only import genuinely new or changed entries.
6. Surface delta stats in the backup UI: "Last backup: 3 min ago · 2 records changed · 1 document added."

**Files involved:**
- `domain/src/main/java/com/lifepilot/domain/usecase/ExportDataUseCase.kt`
- `data/src/main/java/com/lifepilot/data/repository/PreferenceManager.kt`
- `data/src/main/java/com/lifepilot/data/worker/BackupWorker.kt`

---

## ISSUE-058 — No voice input for chat; user must type all messages
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
The chat input field has no microphone button. All user messages must be typed. Describing a life event, recounting a doctor's visit, or dictating a task verbally is not possible. Voice is the fastest input method for complex, unstructured information — which is exactly the kind LifePilot needs.

**Planned fix approach:**
1. Add a microphone icon button to the right of the chat input field. Visible when the text field is empty; replaces the send button until text is entered.
2. Tapping the mic button starts Android's `SpeechRecognizer` (on-device, no network needed for recogniser itself). A pulsing waveform animation plays while listening. Tapping again stops recording.
3. Recognised text is inserted into the input field as editable draft text — the user sees what was captured before sending. They can edit before sending.
4. If recognition confidence is low, the field text is shown with a soft underline indicating uncertain segments. User can correct before sending.
5. Holding the mic button starts continuous dictation mode (useful for long descriptions). Releasing ends it.
6. Handle `RECORD_AUDIO` permission request gracefully with a clear rationale string.
7. `SpeechRecognizer` is used, not Google's `RecognizerIntent` (which opens a system dialog) — the experience stays fully within LifePilot's UI.

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/ui/AiChatScreen.kt`
- New: `features/home/src/main/java/com/lifepilot/features/home/ui/components/VoiceInputButton.kt`
- `app/src/main/AndroidManifest.xml` — `RECORD_AUDIO` permission

---

## ISSUE-059 — Chat conversations have no name; impossible to navigate conversation history
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
Conversations in the history list are unnamed or use a generic timestamp label. When a user has 20 conversations, they cannot tell which one was about their visa, which was about the job offer, and which was about the insurance renewal. There is no way to rename a conversation manually. Finding a past conversation requires opening each one.

**Planned fix approach:**
1. **Auto-name on first AI response**: after the first AI response in a new conversation is received, fire a lightweight background request (or use the same AI provider with a one-shot prompt: "Summarise this conversation in 5 words or fewer as a title") to generate a conversation name. Store the result as `conversation.title` in Room. The title appears in the conversation list and the chat header immediately.
2. If the AI provider is unavailable (offline), fall back to extracting the first 6 words of the user's opening message as a provisional title, flagged as `autoTitled = false`. Update it when connectivity returns.
3. **Manual rename**: long-press a conversation in the history list → "Rename" option in the context menu. Also accessible via the overflow menu inside an open conversation. Opens an inline text field pre-filled with the current title. Submit with Enter or a tick button.
4. **Renamed conversations** are never auto-retitled — user-set names are permanent until the user changes them again.
5. Conversation list shows: title (bold), last message preview (1 line, dimmed), relative timestamp (right-aligned). This is consistent with standard messaging app conventions.

**Files involved:**
- `domain/src/main/java/com/lifepilot/domain/model/AiConversation.kt` — add `title`, `autoTitled` fields
- `data/src/main/java/com/lifepilot/data/db/dao/ConversationDao.kt`
- `data/src/main/java/com/lifepilot/data/engine/LifeStateEngineImpl.kt` — trigger title generation after first response
- `features/home/src/main/java/com/lifepilot/features/home/ui/ConversationListScreen.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/AiChatScreen.kt`

---

## ISSUE-060 — No mascot or brand identity in the app; empty states and loading states are generic
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
Empty states, loading screens, and the app's identity moments (onboarding, morning brief, first launch) have no visual character. There is no mascot, no motion identity, nothing that makes LifePilot feel like a considered product with personality. It feels like scaffolding.

**Desired state:**
A **compass** as the LifePilot mascot and motion identity. The compass represents orientation, direction, and navigation through life — the exact metaphor the product is built on.

**Planned fix approach:**

*Compass animation asset:*
- Vector drawable compass (`AnimatedVectorDrawable` or Compose `Canvas`-drawn) — no raster assets, fully scalable.
- The compass has a clean, minimal design: circular bezel, cardinal points (N/S/E/W), a two-tone needle (red north, white south).
- The needle has a characteristic **settle animation**: on trigger, rotates quickly to a new heading, overshoots slightly, then settles back with a spring physics curve (`dampingRatio = 0.6`, `stiffness = 200`). This is the signature motion.

*Where the compass appears:*
- **Splash / loading screen**: compass needle spins freely, settles to north as the app finishes loading.
- **Empty AI chat**: compass sits centred in the empty state, needle settling gently. Caption: "Ask anything about your life."
- **Morning Brief card**: small compass icon rotates to a heading when the brief is generated — each day a different heading, visually suggesting a new direction.
- **Onboarding**: compass as the hero illustration on the welcome screen.
- **Search empty state**: compass with needle oscillating between directions. Caption: "Nothing found — your life is a blank map here."
- **App icon**: compass rose, consistent with the in-app asset.

*Technical approach:*
- Implement as a reusable `@Composable fun CompassAnimation(state: CompassState, modifier: Modifier)` in the design system.
- `CompassState` controls: `isLoading`, `targetHeadingDegrees`, `isSettled`. Animates automatically via `animateFloatAsState` with a spring spec.
- Never use a GIF or Lottie file — the animation is pure Compose `Canvas` so it scales, themes, and dark-modes correctly.

**Files involved:**
- New: `designsystem/src/main/java/com/lifepilot/designsystem/components/CompassAnimation.kt`
- New: `designsystem/src/main/java/com/lifepilot/designsystem/components/CompassState.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/AiChatScreen.kt` — empty state
- `features/onboarding/src/main/java/com/lifepilot/features/onboarding/ui/WelcomeScreen.kt`
- `app/src/main/res/` — updated app icon

---

## ISSUE-061 — Record schema is incomplete; large life areas have no object definitions
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
The existing schema covers a narrow set of object types. A user trying to track their mutual fund SIP, their gym membership, their child's school records, their freelance contracts, their vehicle, their will, or their professional certifications finds no matching record type. They are forced to use a generic type or not record it at all. The schema is the backbone of the Life State Engine — gaps in the schema are gaps in what LifePilot can understand about a user's life.

**Root cause (verified):**
The initial schema was built to demonstrate the architecture, not to cover the full breadth of a person's administrative life. No systematic research was done to enumerate what objects a person in their 20s–40s typically manages across all domains.

**Planned fix approach:**
Conduct a domain-by-domain audit of every life area and define the full object type set. Each new object type needs: `typeId`, display name, icon, metadata fields (with types, validation, and required flags), lifecycle states, linked domains, suggested tasks on creation, and reminder rules.

**Proposed expanded schema (to be researched and validated):**

*Identity:*
- Passport, Aadhaar, PAN, Voter ID, Driving Licence, Birth Certificate, Marriage Certificate, Degree Certificate, Professional Certification, Will/Testament

*Finance:*
- Bank Account, Credit Card, Debit Card, Fixed Deposit, Recurring Deposit, Mutual Fund / SIP, Stock/Equity Holding, PPF/NPS Account, Loan (Home/Car/Personal/Education), Insurance Policy (Health/Life/Vehicle/Term), Tax Filing, UPI/Payment App, Locker (Bank)

*Career:*
- Job, Internship, Freelance Contract, Offer Letter, Employment Agreement, Appraisal Record, Professional Certification, LinkedIn/Resume Version, Reference Contact

*Health:*
- Health Insurance, Doctor (GP/Specialist), Prescription, Vaccination Record, Lab Test Result, Hospital Visit, Chronic Condition, Allergy, Emergency Contact, Medical Device (spectacles prescription, hearing aid, etc.)

*Property:*
- Rental Agreement, Owned Property, Utility Account (electricity/gas/water/internet), Home Loan, Society/HOA Membership, Vehicle (car/bike), Vehicle Insurance, Vehicle RC/Registration, Parking Spot

*Travel:*
- Passport (cross-linked from Identity), Visa, Flight Booking, Train/Bus Booking, Hotel Booking, Travel Insurance, Forex Card, International SIM, Itinerary

*Education:*
- Degree, Course Enrollment, Certification, Scholarship, Student Loan, School Record (for children)

*Family:*
- Family Member Profile (cross-links to any object above belonging to that member)

*Legal:*
- Contract, NDA, Power of Attorney, Court Case, Notarised Document, Legal Notice

*Subscriptions and memberships:*
- Software Subscription, Streaming Service, Gym Membership, Club Membership, Newsletter/Magazine

**Research requirement before implementation:**
Before writing schema definitions, interview 5–10 people in the target demographic (25–40, urban, salaried) about what they actually struggle to track. Validate the list against real pain points. Prioritise by frequency of confusion, not completeness. Schema entries are never shown to users by their internal name — display names and icons must be warm and human (ISSUE-053).

**Files involved:**
- `data/src/main/java/com/lifepilot/data/schema/` — all object type JSON definitions
- `domain/src/main/java/com/lifepilot/domain/model/ObjectType.kt`
- `designsystem/` — new icons for each object type

---

# P4 — Roadmap

---

## ISSUE-035 — Morning Brief WorkManager job silently fails when offline or API key is missing
**Reported:** 2026-06-29 | **Priority:** P3

**Observed behaviour:**
`WorkManagerScheduler.scheduleMorningBrief()` is called at app initialisation. If the user has no API key configured or is offline, the worker either throws an uncaught exception or produces an empty/stale brief with no user-facing explanation. The user sees a notification with no content, or no notification at all.

**Root cause (verified):**
`MorningBriefWorker` (or equivalent) calls the cloud AI provider to generate the brief. It has no fallback path for offline or no-key states. No `Result.failure()` handling posts a graceful degradation message.

**Planned fix approach:**
1. Before invoking the AI, check: is a provider configured? Is the network available? If either is false, generate the brief deterministically from the Deterministic Engine (Layer 1 of the new architecture): list the top 3 upcoming reminders, any expiring documents in the next 30 days, and any overdue tasks. No AI required for this.
2. The AI-generated brief becomes an enhancement on top of the deterministic brief — not the sole source.
3. If the AI call fails (timeout, error), fall back to the deterministic brief rather than showing nothing.
4. The notification always fires at the scheduled time. Its content degrades gracefully: rich AI summary when available, structured list when not.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/worker/MorningBriefWorker.kt`
- `data/src/main/java/com/lifepilot/data/engine/LifeStateEngineImpl.kt` — expose a deterministic brief method

---

## ISSUE-062 — No AI voice interface; all interaction is text-only
**Reported:** 2026-06-29 | **Priority:** P4

**Desired state:**
A fully conversational voice mode where the user speaks to LifePilot and hears responses read back. "Hey LifePilot, when does my passport expire?" → spoken answer. "Add a task to renew it." → spoken confirmation. Hands-free, eyes-free operation — useful while driving, cooking, or exercising.

**Design:**

*Voice pipeline:*
```
Microphone
    ↓
On-device STT (SpeechRecognizer — same as ISSUE-058)
    ↓
LifeStateEngine / AiProvider (same pipeline as text chat)
    ↓
Response text
    ↓
On-device TTS (Android TextToSpeech API — no cloud dependency)
    ↓
Speaker
```

*Wake word (optional, later):* "Hey LifePilot" triggers listening without touching the screen. Implementation deferred — requires always-on microphone permission which raises significant privacy questions. First version requires a tap to start.

*Voice mode UI:*
- Dedicated voice mode button in the chat screen (or accessible via long-press of the mic from ISSUE-058).
- Entering voice mode expands the compass mascot (ISSUE-060) to fill the screen. The needle animates as the AI processes. The spoken response subtitle-scrolls below the compass as it is read aloud.
- The user can interrupt mid-response by speaking. TTS is stopped, STT restarts.

*TTS voice:*
- Android `TextToSpeech` with locale set to the user's system locale. No cloud TTS in V1.
- Future: swap to a higher-quality on-device neural TTS voice (e.g. Kokoro, Coqui XTTS) as quality of on-device models improves.

**Prerequisites:** ISSUE-058 (voice input), ISSUE-060 (compass animation).

**Files involved:**
- New: `features/home/src/main/java/com/lifepilot/features/home/ui/VoiceModeScreen.kt`
- New: `data/src/main/java/com/lifepilot/data/voice/VoiceOrchestrator.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

---

## ISSUE-042 — iOS app: no implementation, sync strategy undefined
**Reported:** 2026-06-29 | **Priority:** P4

**Desired state:**
LifePilot available on iOS (iPhone/iPad) with full feature parity and seamless sync with the Android app so a user can switch devices without data loss or duplication.

**Architectural considerations:**
1. **Shared logic**: Business logic (domain models, use cases, rules engine, schema engine) should be extracted to a Kotlin Multiplatform (KMP) module. This is the only realistic path to sharing logic between Android and iOS without rewriting it.
2. **UI**: iOS UI built natively in SwiftUI — not a WebView or cross-platform renderer. The design system tokens (colours, typography, spacing) translate to a `LifePilotDesignSystem.swift` equivalent.
3. **Sync strategy**: End-to-end encrypted sync via the user's own iCloud Drive or Google Drive. No LifePilot-owned server. Sync unit is the export ZIP (ISSUE-040). Conflict resolution: last-write-wins on metadata fields; document files are additive (never deleted by sync).
4. **Local storage**: iOS side uses SwiftData (or CoreData) with the same schema as Room. KMP domain models compile to Swift via `kotlin-swift-export`.
5. **AI providers**: Same `AiProvider` interface. On-device model (ISSUE-024) uses Core ML on iOS instead of MediaPipe — same abstraction, different backend.
6. **Prerequisites**: ISSUE-040 (ZIP export), ISSUE-024 (on-device AI abstraction), KMP domain module extraction must all be complete before iOS development begins.

**Files involved:**
- New: `shared/` KMP module — domain models, use cases, schema engine, rule engine
- New: `ios/` Xcode project — SwiftUI screens, SwiftData persistence, Core ML inference
- `domain/` module — must be KMP-compatible (remove Android-specific imports)

---

## ISSUE-043 — Web app: no implementation, sync strategy undefined
**Reported:** 2026-06-29 | **Priority:** P4

**Desired state:**
LifePilot accessible from a browser — primarily for desktop use (reviewing records, managing tasks, reading AI summaries on a larger screen). Feature parity with mobile is not required for V1 web; read access + search + task management is sufficient.

**Architectural considerations:**
1. **Sync**: Same ZIP-based sync as ISSUE-042. Web app reads from the user's Google Drive or Dropbox — no LifePilot server. User authenticates with their cloud storage provider directly.
2. **Tech stack**: Next.js (TypeScript) with a local IndexedDB store (Dexie.js) that caches the imported bundle. Server-side rendering is not needed — this is a client-side app with Drive as the backend.
3. **AI on web**: Cloud providers only (no on-device model in browser for V1). Same API interface as Android, different HTTP client.
4. **Sensitive data**: The web app must apply the same redaction rules as mobile (ISSUE-019). No personal data stored in browser localStorage beyond the session cache.
5. **Scope for V1 web**: View objects + metadata, search, view/complete tasks, read AI conversation history, trigger a new AI query. Document upload deferred to V2 web.
6. **Prerequisites**: ISSUE-040 (ZIP export with documents), stable data schema. Web can begin once the export format is finalised and stable.

**Files involved:**
- New: `web/` Next.js project — TypeScript, Dexie.js, Drive OAuth
- `domain/` module — export models must be serialisable to JSON consumed by the web client

---

## ISSUE-046 — Desktop app: QR-code sign-in, Drive-sync session, incremental fetch
**Reported:** 2026-06-29 | **Priority:** P4

**Desired state:**
LifePilot available as a desktop application (macOS and Windows) with zero friction sign-in — no account, no password. The phone is the identity. The desktop session fetches only what it needs from Drive; it does not download the entire backup.

**Sign-in flow design:**
1. Desktop app launches → displays a randomly generated QR code (encodes a one-time session token + device fingerprint).
2. User opens LifePilot on their phone → Settings → "Connect Desktop" → scans the QR code.
3. Phone sends the session token + Drive access grant to a lightweight relay (self-hosted or a minimal stateless Cloudflare Worker — no LifePilot server stores data).
4. Desktop receives the Drive credentials via the relay → relay deletes the session token immediately.
5. Desktop authenticates with Drive directly from this point. Relay is only used for the one-time handshake. No LifePilot server ever holds user credentials or data.

**Data fetching strategy:**
1. On first connect: download `manifest.json` from Drive (see ISSUE-047). Parse the manifest to enumerate all objects, documents, and their hashes.
2. Download `data.json`, `tasks.json`, `conversations.json` immediately (small, always needed).
3. Download documents **on demand** — only when the user opens a specific object. Cache locally with an LRU eviction policy (configurable max cache size, default 500 MB).
4. Background sync: poll Drive manifest every 15 minutes. Download only entries whose `contentHash` changed since last fetch.

**Tech stack:**
- Electron (TypeScript) or Tauri (Rust + WebView) — Tauri preferred for smaller binary and better performance.
- Local cache: SQLite via `better-sqlite3` (Electron) or `rusqlite` (Tauri). Same schema as the Android Room DB.
- UI: React + TypeScript. Shares design tokens (colours, typography) with the web app (ISSUE-043).

**Prerequisites:** ISSUE-040 (ZIP + manifest), ISSUE-047 (delta export), stable schema.

**Files involved:**
- New: `desktop/` Tauri/Electron project
- Minimal relay: Cloudflare Worker (stateless, single JS file, no persistence)

---

## ISSUE-048 — No payment infrastructure or subscription model defined
**Reported:** 2026-06-29 | **Priority:** P4

**Desired state:**
LifePilot eventually charges for the service — likely for cloud AI usage, cross-device sync, or a premium tier. The payment infrastructure needs to be designed before any of these features ship publicly.

**Considerations (not yet decided):**
1. **Model options**: free tier (on-device AI only, single device, no backup) vs paid tier (cloud AI credits, multi-device sync, automated backup). Freemium is the likely approach.
2. **Payment provider**: RevenueCat for Android/iOS in-app purchase (handles App Store and Play Store billing). Stripe for web and desktop.
3. **Entitlement system**: a lightweight entitlement check (local, no server call for basic features) that reads from a signed entitlement token stored in `EncryptedSharedPreferences`. Premium features gate-check this token.
4. **AI credit model**: cloud AI calls consume credits. On-device AI is always free. Credits replenish monthly with subscription. Overage: pay-per-use or throttle to on-device only.
5. **Privacy**: no usage telemetry beyond anonymous credit consumption counts. No content of AI queries ever sent to a billing server.

**Prerequisites:** stable feature set, public beta completion, pricing research.

**Files involved:**
- New: `domain/.../usecase/CheckEntitlementUseCase.kt`
- New: `data/.../billing/EntitlementRepository.kt`
- `features/settings/` — subscription management screen

---

## ISSUE-049 — No referral or access code system for free trial grants
**Reported:** 2026-06-29 | **Priority:** P4

**Desired state:**
A randomly generated alphanumeric code (e.g. `LIFEPILOT-X7K2-Q9MR`) that the developer or a beta user can share with anyone. Entering the code in the app grants free access to the premium tier for a configurable duration (e.g. 30 days, 90 days, lifetime beta).

**Design:**
1. **Code generation**: codes generated offline — a code encodes `(tier, expiryDuration, createdAt, salt)` and is HMAC-signed with a private key embedded at build time. No server call required to generate or validate a code.
2. **Code validation**: on the device, re-compute the HMAC of the code payload and compare against the embedded signature. If valid, write a signed entitlement token to `EncryptedSharedPreferences` with the expiry date.
3. **Revocation**: a periodically fetched revocation list (small JSON file on a CDN, no auth required) lists invalidated code prefixes. App checks this list on each launch when online. If offline, the last known list is used.
4. **Code entry UI**: Settings → "Have a code?" → text input → validate → show confirmation with tier and expiry date.
5. **No account required**: entitlement is device-local. If the user reinstalls or switches devices, they re-enter the code.

**Prerequisites:** ISSUE-048 (entitlement system) must exist before codes can grant anything meaningful.

**Files involved:**
- New: `domain/.../usecase/RedeemAccessCodeUseCase.kt`
- New: `data/.../billing/AccessCodeValidator.kt`
- `features/settings/` — code entry UI

---

## ISSUE-031 — Family member profiles and documents not well supported
**Reported:** 2026-06-29 | **Priority:** P4

**Desired state:**
Users should be able to track documents, reminders, and life events for family members (spouse, parents, children) within the same app. Example: spouse's passport expiry, parent's health insurance renewal, child's school records.

**Root cause (verified):**
The `Profile` model exists but is designed as a single-user model. There is no concept of a secondary profile, a relationship type, or a document ownership link to a person other than the primary user. The `Relationship` canonical entity exists in the schema spec but has no implementation.

**Planned fix approach:**
1. Implement the `Relationship` canonical entity: `(relationshipId, primaryProfileId, name, relation: SPOUSE | PARENT | CHILD | SIBLING | OTHER, dateOfBirth?, notes)`.
2. Add an optional `ownerRelationshipId` field to `LifePilotObject`. When set, the object belongs to a family member rather than the primary user.
3. In Library: add a "Family" filter tab. Objects with an `ownerRelationshipId` appear under that person's name.
4. Reminders for family documents generate on the same schedule — indistinguishable from primary user reminders in the notification and Home brief.
5. AI context: when `ownerRelationshipId` is set on a retrieved object, prepend ownership in the snapshot: "Spouse's Passport — ..." so the AI never conflates family documents with the primary user's.
6. Entry point: Settings > Family Members — add/edit/remove family members. Or from Library "Add new" → "This belongs to a family member".
7. Do not implement multi-user cloud sync. Family members are local relationships within the primary profile — not separate accounts.

**Files involved:**
- New: `domain/src/main/java/com/lifepilot/domain/model/Relationship.kt`
- `domain/src/main/java/com/lifepilot/domain/model/LifePilotObject.kt` — add `ownerRelationshipId`
- New: `data/src/main/java/com/lifepilot/data/database/entity/RelationshipEntity.kt`
- `data/src/main/java/com/lifepilot/data/engine/ObjectReasoner.kt`
- `features/library/src/main/java/com/lifepilot/features/library/ui/LibraryScreen.kt`
- `features/settings/src/main/java/com/lifepilot/features/settings/ui/SettingsScreen.kt`

---

## ISSUE-063 — Life Actions: Agentic Execution Layer for administrative workflows
**Reported:** 2026-06-29 | **Priority:** P4

**Vision:**
LifePilot today understands life and plans it. The next evolution is executing it. A **Life Action** is a high-confidence, user-approved administrative workflow that LifePilot can complete on the user's behalf using the verified information it already holds. The browser is an implementation detail. The product never exposes "automation" — the user simply says "help me complete this" and LifePilot determines whether it can, prepares everything it knows, asks only for what is missing, and executes with explicit approval at every irreversible step.

**Architecture evolution:**
```
Today:
Life Event → Understand → Update Life State → Create Plan

Future:
Life Event → Understand → Plan → Execute → Verify → Update Life State
```

Execution is a new capability layer that sits above the existing engine. It does not replace any existing layer — it consumes the outputs of `DomainLifeState`, `LifeEventActionPlan`, and the `Planner` to drive real-world actions.

**Example — Japan visa application:**
1. User: "I'm travelling to Japan in October."
2. `LifeEventClassifier` (ISSUE-057) recognises a Travel Event. Affected domains: Travel, Identity, Finance, Health.
3. `DomainLifeStateEvaluator` checks: passport validity, existing Japan visa, travel insurance, existing bookings.
4. Internet Intelligence Layer (ISSUE-024 Layer 3) searches for current Japan visa requirements for Indian passport holders.
5. `LifeEventActionPlan` (ISSUE-057) is generated: create Japan Trip goal, generate tasks (visa, insurance, flights, accommodation) with sensible deadlines derived from travel date.
6. One task: "Complete Japan tourist visa application." LifePilot marks this as **Life Action eligible**.
7. User taps "Execute this." `LifeActionEngine` opens the official Japan visa application portal in a managed WebView.
8. Every known field is pre-filled from verified metadata: name, passport number, date of birth, nationality, address, travel dates, purpose of visit, accommodation details.
9. Fields that cannot be pre-filled are surfaced one at a time as inline questions. No field is left blank without asking.
10. Before submission: full review screen showing every field populated. User corrects anything incorrect.
11. Explicit "Submit application" confirmation required — a deliberate, unambiguous tap.
12. After submission: receipt/reference number captured, stored as a Document on the Japan Trip object. Task marked complete. Travel domain life state updated.

**Example — Job application:**
User: "Apply for this Product Manager role at [URL]."
LifePilot already holds in `career.life`: resume version, employment history, education, phone, address, LinkedIn, portfolio URL, salary expectation, notice period, preferred locations, remote preference. It opens the application, populates every matching field, surfaces only fields it doesn't know (cover letter, specific screening questions), presents a review, submits after explicit approval, creates a Job Application record, and begins interview tracking automatically.

**Life Action eligibility criteria:**
A task is eligible for Life Action execution only when all of the following are true:
- It is a known workflow type with a defined execution template.
- The target website/form is an official source (not a third-party aggregator).
- At least 60% of required fields can be pre-filled from verified data.
- The action is reversible or the user has been explicitly warned it is not.
- The user has enabled Life Actions in Settings (opt-in, off by default).

**Execution mechanism on Android:**
Browser automation on Android does not mean Playwright or Puppeteer. The execution options in priority order:
1. **Android Accessibility Service** — reads and interacts with any visible UI element in any app or browser. Does not require root. This is the same mechanism used by password managers and TalkBack. Most powerful option; requires a sensitive permission that must be requested transparently with clear explanation.
2. **Managed WebView with JavaScript injection** — LifePilot opens a fullscreen `WebView`, injects field-fill scripts, hands control back to the user for review and submission. Simpler than Accessibility Service; only works for web forms, not native apps. V1 approach.
3. **URL parameter pre-fill** — some government portals accept query parameters. Trivial to implement; very limited coverage.
4. **Official APIs** — where the target service offers an official API (LinkedIn Easy Apply, some government portals). Always preferred over UI automation when available.

V1 uses the Managed WebView approach. Accessibility Service approach is investigated as V2 when coverage requirements grow.

**Relationship to existing architecture:**
- `DomainLifeState` (ISSUE-050) is the data source — the execution engine reads from it, never gathers independently.
- `LifeEventActionPlan` (ISSUE-057) is what surfaces executable tasks to the user.
- The Planner evolves to distinguish between manual tasks and Life Action tasks. Life Action tasks show an "Execute" button.
- After successful execution, `DomainLifeStateEvaluator` (ISSUE-051) is triggered to update the relevant domains.
- `AiProposal` gains a new subtype: `ActionProposal` — containing the action type, pre-fill field map, missing fields, and the target URL.

**Guiding principles (non-negotiable):**
- User remains in control at every step.
- No irreversible action without explicit, unambiguous confirmation.
- Use verified metadata only — never infer or guess a form field value.
- Ask only for genuinely missing information.
- The browser/automation layer is never visible to the user as a concept. The product is "completing a Life Action", not "running browser automation."
- If execution fails partway through, the engine records how far it got, what was submitted, and surfaces a recovery task.

**Positioning:**
LifePilot is an **AI Administrative Operating System**. Life Actions are the execution layer of that operating system. The product's four layers: Knowledge (Documents + Records), Life State (Domain Understanding), Planning (Goals + Tasks), Execution (Life Actions). This framing is what differentiates LifePilot permanently from a notes app, a task manager, or a generic AI assistant.

**New domain models needed:**
- `LifeAction` — action type, status, target URL, pre-fill map, missing fields, execution log, outcome
- `LifeActionTemplate` — configuration-driven definition of a workflow (fields, target, eligibility rules, confirmation requirements)
- `ExecutionLog` — timestamped record of every step taken during a Life Action execution

**Prerequisites:**
ISSUE-057 (Life Event Action Plan), ISSUE-050 (Domain Life State canonical), ISSUE-051 (maintenance pipeline), ISSUE-061 (expanded schema — actions need the right record types to draw from), stable verified metadata (ISSUE-028).

**Files involved:**
- New: `domain/src/main/java/com/lifepilot/domain/model/LifeAction.kt`
- New: `domain/src/main/java/com/lifepilot/domain/model/LifeActionTemplate.kt`
- New: `domain/src/main/java/com/lifepilot/domain/model/ExecutionLog.kt`
- New: `domain/src/main/java/com/lifepilot/domain/engine/LifeActionEngine.kt`
- New: `data/src/main/java/com/lifepilot/data/execution/WebViewExecutor.kt`
- New: `data/src/main/java/com/lifepilot/data/schema/actions/` — Life Action template JSON configs
- New: `features/planner/src/main/java/com/lifepilot/features/planner/ui/LifeActionExecutionScreen.kt`
- `features/planner/src/main/java/com/lifepilot/features/planner/ui/PlannerScreen.kt` — "Execute" button on eligible tasks
- `features/settings/` — Life Actions opt-in toggle and permission explanation

---

## ISSUE-024 — Hybrid Intelligence Architecture + Model-Agnostic Embedded SLM Layer
**Reported:** 2026-06-29 | **Priority:** P4

### Product Philosophy

LifePilot is not a chatbot. It is an AI Life Operating System.

Its intelligence combines Personal Memory, Structured Life Graph, Documents, Timeline, Calendar, Tasks, Goals, Habits, Financial Records, Contacts, Notes, Travel History, and Live Internet Knowledge.

**Internet access is not optional.** The AI must reason over both personal knowledge and live external information. However, personal information must never be sent to an internet search provider.

---

### Architecture: Four Layers

#### Layer 1 — Deterministic Engine
Answers anything that can be answered with rules rather than AI. Invoked first, before any model call.

Responsibilities: reminder calculations, expiry calculations, timeline queries, calendar queries, task filtering, habit statistics, search indexing, structured queries, rule engine, notification scheduling.

> Example: "What expires next month?" → structured DB query, no model involved.

#### Layer 2 — Personal Knowledge Layer
Encrypted local storage. The AI retrieves relevant context from here. This data is never sent to an internet search provider.

Contents: Life Graph, Documents, Calendar, Timeline, Goals, Tasks, Notes, Medical Records, Travel History, Contacts, Financial Information, Memories, Preferences.

#### Layer 3 — Internet Intelligence Layer
First-class internet search for any answer that depends on changing external information. Search providers are abstracted — the system must support replacing them without affecting the rest of the pipeline.

Required for: government rules, passport renewal, visa requirements, immigration, tax regulations, healthcare guidelines, insurance rules, travel advisories, weather, current news, market prices, flight status, public holidays, bank rules, UPI regulations, mutual fund rules, RBI circulars, any procedural information that changes over time.

**Supported providers (abstracted):** Brave Search, SerpAPI, Tavily, Google Search API, future providers.

#### Layer 4 — Reasoning Layer
Synthesises Personal Context + Internet Search Results + User Intent + Conversation History into a single response. Runs after Layers 1–3 have assembled the input.

---

### Model Abstraction Layer

The AI engine must never be coupled to a specific embedded model. Architecture:

```
AI Orchestrator
      ↓
EmbeddedModelInterface   (domain interface — no model specifics)
      ↓
┌─────────────────────────────────────┐
│ Gemma 2B Q4 (V1 default)           │
│ Gemma 3 / Gemma 4 (future)         │
│ Qwen 3 / Qwen 4 (future)           │
│ Microsoft Phi family (future)       │
│ Llama family (future)               │
│ Any GGUF-compatible model (future)  │
└─────────────────────────────────────┘
      ↓
InferenceBackendInterface  (domain interface — no runtime specifics)
      ↓
┌─────────────────────────────────────┐
│ MediaPipe LLM Inference (V1 default)│
│ ONNX Runtime (future)               │
│ llama.cpp via JNI (future)          │
│ Future Android ML runtimes          │
└─────────────────────────────────────┘
```

Business logic, orchestration, prompts, and UI must never reference a specific model or runtime. Model and runtime are selected through configuration only.

**V1 default:** Google AICore + Gemma 4 E2B (~1.8 GB, fits Snapdragon 778G+ / 6 GB RAM). Gemma 4 (April 2026) ships with native agentic capabilities, function calling, and structured JSON output — a meaningfully better fit for LifePilot's structured proposal pipeline than Gemma 2B Q4. MediaPipe LLM Inference API remains the fallback for devices where AICore is unavailable.

---

### Cloud Model Abstraction

Cloud providers are also abstracted behind a `CloudAiProvider` interface. Supported: NVIDIA NIM, Anthropic, OpenAI, Google, future providers. Swappable through configuration without touching business logic.

---

### Intelligence Orchestration — Decision Flow

```
User Query
    │
    ▼
1. Can the Deterministic Engine answer this?
   YES → Return structured result. Stop. No model invoked.
   NO  → Continue.
    │
    ▼
2. Is personal memory required?
   YES → Retrieve relevant Life Graph context from Layer 2.
    │
    ▼
3. Is live external knowledge required?
   YES → Run internet search via Layer 3. Retrieve top results.
   NO  → Skip.
    │
    ▼
4. Combine: Personal Context + Internet Results + Conversation History
   Pass into Embedded SLM (Layer 4).
    │
    ▼
5. Can the embedded SLM reasonably perform this task?
   YES → Return response.
   NO  → Escalate to configured Cloud Model.
    │
    ▼
Return response to user.
```

---

### Privacy Rules (non-negotiable)

- Personal data (passport numbers, financial details, health records, contact info) is **never** sent to internet search providers.
- Internet search queries contain only generic procedural questions constructed from intent, never from personal values.
- The boundary is enforced in the `InternetIntelligenceOrchestrator` before any search call is made.
- Example: "passport renewal procedure India 2025" is a safe search query. "Z4921835 passport holder renewal" is not.

---

### Query Classification Examples

| User Query | Deterministic | Personal | Internet | Reasoning |
|---|---|---|---|---|
| When does my passport expire? | ✓ | — | — | — |
| How do I renew my passport? | — | — | ✓ | ✓ |
| Should I renew my passport now? | — | ✓ expiry + travel | ✓ rules + fees + time | ✓ |
| Do I need vaccines for Brazil? | — | ✓ trip dates | ✓ WHO + advisories | ✓ |
| Summarise my LIC policy | — | ✓ document | — | ✓ |
| Can I carry my medication into Japan? | — | ✓ medication list | ✓ customs regulations | ✓ |
| What's happening with NVIDIA today? | — | — | ✓ | ✓ |
| Should I invest this month's savings? | — | ✓ cashflow + goals | ✓ market conditions | ✓ |

---

### Embedded Model Options (V1 evaluation)

| Model | Runtime | Size | Mid-range fit | Notes |
|---|---|---|---|---|
| Gemma 4 E2B | Google AICore | ~1.8 GB | ✓ SD 778G+ / 6 GB | **V1 default.** Agentic, function calling, structured JSON output. April 2026. |
| Gemma 2B Q4 | MediaPipe | ~1.5 GB | ✓ SD 778G+ / 6 GB | Fallback when AICore unavailable. Still viable. |
| Phi-3 Mini | llama.cpp JNI | ~0.7–2 GB | ✓ | Strong reasoning per parameter. Requires JNI bridge. |
| Qwen 1.5B | ONNX Runtime | ~1 GB | ✓ | Strong multilingual. Good for non-English users. |
| TinyLlama 1.1B | llama.cpp JNI | ~0.6 GB | ✓ Lower-end devices | Weaker reasoning, smallest footprint. |
| Mistral 7B Q4 | MLC-LLM | ~4 GB | ✗ Too heavy | High-end only. Future option. |

V1 ships with Gemma 4 E2B via Google AICore as the default, with Gemma 2B Q4 via MediaPipe as the fallback. The `EmbeddedModelInterface` means swapping to any row in the table above requires only a new implementation class — no orchestration, prompt, or UI changes. Gemma 4's native function calling is directly applicable to LifePilot's `AiProposal` structured output pipeline.

---

### Custom Model Training

Not recommended for V1.

Instead invest in: prompt engineering, Life Graph improvements, better retrieval, memory architecture, context optimisation.

After sufficient production usage and several thousand anonymised interaction examples, propose a roadmap for fine-tuning an embedded SLM specifically for LifePilot's reasoning style, summarisation quality, life-state planning, and structured output format. Custom training is a v2+ initiative.

---

### Trade-off Analysis

| Concern | Detail |
|---|---|
| Model download size | ~1.5 GB one-time. User warned during onboarding. Wi-Fi recommended. |
| First-inference latency | Gemma 4 E2B via AICore: ~1.5–3s on SD 778G (AICore pre-loads model at OS level, reducing cold-start significantly vs MediaPipe). Optimise with KV-cache warm-up. |
| Memory pressure | ~800 MB RAM at inference. App must release model from memory when backgrounded >10 min. |
| Internet search latency | Brave Search p95 ~300ms. Added to inference time only when needed. |
| Battery impact | Inference ~200–400mW. Per-query, not continuous. Acceptable. |
| Privacy | Personal data never leaves device. Search queries contain no personal values. Enforced at orchestration layer. |
| Model staleness | New model versions adopt via configuration + re-download. No code change required by design. |

---

### Performance Implications

- Deterministic Layer answers ~40% of queries with zero model invocation (expiry, task, calendar, reminder queries).
- Internet search adds ~300–500ms when required. Parallelise with personal context retrieval to hide latency.
- Embedded SLM cold-start: ~3–5s first inference. Warm subsequent queries: ~1–2s.
- Cloud escalation: ~3–8s round-trip. Only for queries the embedded SLM flags as beyond its capability.

---

### Future Roadmap — Embedded AI (3–5 years)

| Timeframe | Expected state | LifePilot response |
|---|---|---|
| 2026 | Gemma 4 E2B with AICore, agentic + function calling | Ship Gemma 4 E2B as V1 default. MediaPipe fallback for older devices. |
| 2026–2027 | 7B models fit flagship, 4B fit mid-range | Offer Gemma 4 4B / Qwen 3 4B as an opt-in upgrade for capable devices. |
| 2027 | 7B models viable on mid-range | Promote 7B to default on SD 8-series. On-device reasoning quality approaches cloud for most LifePilot queries. |
| 2028 | On-device multimodal (text + image) | Enable document image understanding locally — OCR + visual reasoning in one pass. |
| 2029–2030 | 13B+ on flagship, 7B universal | Cloud AI becomes the fallback, not the default. LifePilot fully sovereign on most devices. |
| V2+ | Sufficient production usage data | Fine-tune a LifePilot-specific SLM on anonymised interaction corpus for life admin reasoning, structured output, and domain life state summarisation. |

The `EmbeddedModelInterface` + `InferenceBackendInterface` abstraction ensures every row in this table is a configuration and implementation swap, not an architectural change.

---

### Files to Create / Modify

```
New domain interfaces:
  domain/.../engine/EmbeddedModelInterface.kt
  domain/.../engine/InferenceBackendInterface.kt
  domain/.../engine/CloudAiProvider.kt        (rename/refactor existing)
  domain/.../engine/SearchProvider.kt
  domain/.../engine/AiOrchestrator.kt

New data implementations:
  data/.../ai/embedded/MediaPipeInferenceBackend.kt
  data/.../ai/embedded/GemmaEmbeddedModel.kt    (V1 default, swappable)
  data/.../ai/search/BraveSearchProvider.kt
  data/.../ai/search/TavilySearchProvider.kt
  data/.../ai/orchestration/HybridAiOrchestrator.kt
  data/.../ai/orchestration/InternetIntelligenceOrchestrator.kt

Modify:
  data/.../ai/AiProviderFactory.kt              (wire new abstraction)
  data/.../engine/RetrievalEngineImpl.kt         (feed into orchestrator)
  data/.../engine/PromptBuilderImpl.kt           (internet context injection)
  features/settings/.../SettingsScreen.kt        (model download management)
  features/onboarding/                           (model download step)
```

---

### Related work / why this matters now
- ISSUE-057 (Life Event Action Plan) now surfaces multi-step plans to the user. Many plan items depend on live external rules: visa requirements, tax deadlines, PF transfer procedure, insurance portability, etc. Without Layer 3 internet intelligence and Layer 4 reasoning, the action plan cannot include accurate, current procedural guidance.
- ISSUE-079 (local knowledge base) should be implemented alongside Layer 3 so common procedural queries are cached and not re-fetched on every action plan.

---

# New Issues — Added 2026-06-30

---

# P1 — High (continued)

---

## ISSUE-066 — Tiered AI approval: sensitive records require user verification; soft preferences auto-applied
**Reported:** 2026-06-30 | **Priority:** P1
**Status: RESOLVED ✓** — Fixed by Kimi (Session 3). `FieldSensitivity` enum added (`PREFERENCE`/`STANDARD`/`SENSITIVE`). Schema JSON `sensitivityLevel` field consumed at runtime. `MetadataUpdate` proposals with sensitivity ≠ `SENSITIVE` auto-execute. Sensitive fields always require full approval card.

**Observed behaviour:**
Every AI proposal — whether it is extracting a passport number or noting that the user prefers Japanese cuisine — goes through the same approval flow. Blocking the user on "Approve / Reject" for "user watches action films" is friction with no benefit. Conversely, auto-applying a proposed passport number without user confirmation is a trust violation. There is no distinction between high-stakes and low-stakes information.

**Root cause (verified):**
`HomeViewModel.approveAction()` presents every `AiProposal` as `pendingAction` regardless of the sensitivity of the data it contains. `PromptBuilderImpl` and the schema engine have no concept of field sensitivity. All proposals surface identically in the UI.

**Planned fix approach:**
1. Add a `sensitivityLevel: FieldSensitivity` enum to the metadata schema definition for every field: `SENSITIVE` (passport number, account number, health data, Aadhaar, PAN, salary, address), `STANDARD` (job title, city, insurance provider, travel dates), `PREFERENCE` (cuisine, movie genre, commute preference, workout habits, dietary restrictions).
2. `AiProposal.MetadataUpdate` computes an aggregate `proposalSensitivity: FieldSensitivity` from the max of its included fields' sensitivity levels.
3. `HomeViewModel`: if `proposalSensitivity == PREFERENCE`, auto-execute without showing a proposal card — post a silent confirmation message in the chat instead: "Noted your preference for Italian food." No approval required, no card shown.
4. If `proposalSensitivity == STANDARD`, show a compact inline confirmation chip (not a full card) — the user can dismiss or undo within 5 seconds; if untouched it auto-executes.
5. If `proposalSensitivity == SENSITIVE`, always show the full approval card with field values displayed and requiring explicit "Save" tap.
6. `ObjectCreation` proposals are always SENSITIVE — creating a new record always requires approval.
7. Status updates (`STATUS_UPDATE`) are always SENSITIVE — changing a record's status requires confirmation.
8. Apply the sensitivity tier consistently throughout: proposal cards, Timber logging (SENSITIVE fields scrubbed), AI context (SENSITIVE fields redacted — ISSUE-019), and backup encryption priority.

**Files involved:**
- `data/src/main/assets/schemas/*.json` — add `sensitivityLevel` to each field definition
- `domain/src/main/java/com/lifepilot/domain/model/ProposedAction.kt` — `proposalSensitivity` on `MetadataUpdate`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt` — tiered approval logic
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt` — compact chip vs full card rendering
- `domain/src/main/java/com/lifepilot/domain/security/SensitiveFieldRegistry.kt` — new, drives the tier lookup

---

## ISSUE-068 — AI fails with "software caused connection abort" — socket reset not retried
**Reported:** 2026-06-30 | **Priority:** P1
**Status: RESOLVED ✓** — Fixed by Claude (Session 4). `ConnectionPool(maxIdleConnections = 5, keepAliveDuration = 30s)` added to OkHttpClient in `NetworkModule`. `retryOnConnectionFailure(true)` explicitly set. Full 3-attempt retry loop with progressive delays (from ISSUE-021) still pending in `NvidiaAiProvider`.

**Observed behaviour:**
Intermittently, an AI request fails immediately with the error: "Failed to connect to api.nvidia.com — software caused connection abort (connect failed)". This is distinct from ISSUE-021 (which handles read timeouts): the connection is reset at the TCP layer before any data is exchanged. The error happens most often when the app has been backgrounded for a few minutes and the user returns to continue a conversation, or when switching between Wi-Fi and mobile data mid-session.

**Root cause (verified):**
Android aggressively closes idle TCP connections when the app is backgrounded. OkHttp's connection pool holds stale connections and attempts to reuse them on the next request. When the server-side or network-layer connection has already been torn down, the reuse attempt receives a TCP RST (connection reset), which surfaces as `java.net.SocketException: Software caused connection abort`. OkHttp's default retry logic does not retry on `SocketException` because it could theoretically be a non-idempotent request. No explicit connection-pool configuration exists in `NetworkModule`.

**Planned fix approach:**
1. In `NetworkModule`, configure OkHttp: `connectionPool(ConnectionPool(maxIdleConnections = 3, keepAliveDuration = 30, timeUnit = SECONDS))`. Reduce keep-alive from the default 5 minutes to 30 seconds to aggressively expire stale connections before they are reused.
2. Add `retryOnConnectionFailure(true)` to the OkHttp builder (enabled by default but make it explicit).
3. In `NvidiaAiProvider`, wrap the request in the retry loop from ISSUE-021. Specifically treat `SocketException` with `"connection abort"` in the message as a retryable error — same as a timeout. First retry is immediate (0s delay) since this is a connection-level failure, not a server-side issue.
4. After the retry loop, if all 3 attempts fail with connection reset errors, surface a user-friendly message: "Connection was interrupted. Please check your network and try again." — not the raw Java exception.
5. Log the connection abort with `Timber.w` (not Timber.e) — it is a transient network condition, not a code bug.

**Related to:** ISSUE-021 (timeout handling — integrate both fixes into a single robust retry layer in `NvidiaAiProvider`).

**Files involved:**
- `data/src/main/java/com/lifepilot/data/di/NetworkModule.kt`
- `data/src/main/java/com/lifepilot/data/ai/NvidiaAiProvider.kt`

---

## ISSUE-072 — Onboarding V2: domain packs, platform demo, AI prompt generator, bulk document scan
**Reported:** 2026-06-30 | **Priority:** P1

**Observed behaviour:**
The current onboarding (ISSUE-014) asks for basic profile information and does nothing else. It provides zero context for why LifePilot matters, does not bootstrap the user's life data, and leaves the user staring at an empty Library with no guidance. First-session retention is at risk. A user who does not see value in the first 3 minutes will not return.

**Root cause (verified):**
ISSUE-014 defines Phase 1 (profile basics) and Phase 2 (domain-driven onboarding). Neither has been implemented at this level of detail. The onboarding journey has not been designed as a product experience — only as a data-collection flow. The wow moment, the demo, the AI prompt generator, and the bulk import capability are entirely absent from the specification.

**Desired onboarding journey (7 steps, designed for 3–5 minute completion):**

**Step 1 — Name and Profession**
Collect: `displayName`, `profession` (free text + suggested chips: "Software Engineer", "Product Manager", "Doctor", "Student", "Entrepreneur", "Other"). These two fields unlock domain pack selection.

**Step 2 — Domain Packs (background download)**
Based on profession, automatically pre-select relevant domain packs. Example: "Product Manager at a tech company" → Career, Finance, Travel, Health. Show a selection grid of all available domains — user can add or remove. "Downloading your packs..." runs in the background while the user proceeds to Step 3. Domain packs contain: relevant schema types pre-loaded, domain-specific AI prompt templates, and suggested tasks for the domain (e.g. Career pack suggests "Upload your latest resume and offer letter").

**Step 3 — Platform Demo (50 seconds, skippable)**
5 full-screen swipeable cards, each demonstrating one core use case:
- Card 1: "Ask anything" — show the AI answering "When does my passport expire?" with real UI screenshot
- Card 2: "One life area, every document" — show Library with a Travel domain containing visa, passport, insurance, flights
- Card 3: "Your AI plans for you" — show a Life Event Action Plan for a job change
- Card 4: "Never miss a renewal" — show a notification for a visa expiry reminder
- Card 5: "Yours forever" — "Everything stays on your device. LifePilot never sells your data."
Each card has a title, 1-sentence description, and an animated illustration. "Skip" button always visible. Auto-advances to next step after Card 5 or on "Get started" tap.

**Step 4 — AI Prompt Generator**
Screen title: "Import from your existing AI assistant."
Subtitle: "If you use ChatGPT, Gemini, or Claude, this prompt will ask it to share what it knows about you — organised by life area — so you can paste the results here."
Show a pre-written prompt in a copyable text box (tap to copy). The prompt instructs the AI to: "List everything you know about me with high confidence, organised by: Career, Finance, Health, Identity, Travel, Property, Education, Relationships. For each area, list specific facts (not guesses) you are confident about. Format as a bulleted list under each heading."
Below the copyable box: a text area labelled "Paste the AI's response here (optional)". If the user pastes a response, LifePilot's NLP pipeline parses the structured text and creates draft `AiProposal.MetadataUpdate` entries for user verification in the next step. If the user skips, they proceed directly to Step 5.

**Step 5 — Bulk Document Scan**
Screen title: "Import your important documents."
Subtitle: "LifePilot will scan common folders on your phone where documents usually live."
Scans (with explicit permission): WhatsApp/Media/WhatsApp Documents/, Telegram/Telegram Documents/, Downloads/, Documents/. Does not scan Photos or other personal media.
Shows a progress bar while scanning. After scan: presents a grid of thumbnails for all found PDFs, images that look like documents (aspect ratio check + basic ML classification). User can select/deselect each. Tapping "Import selected" runs them through `DocumentProcessingPipeline` → OCR → classification → creates draft records for verification.
"Skip for now" always available. Files that are not imported are not stored anywhere.

**Step 6 — Wow Moment**
After any documents are imported (or skipped), show a dynamically generated suggestion: "Here's what you can ask LifePilot right now." The suggestion is generated from whatever data was imported. Examples: "When does your [imported document type] expire?", "What documents do you need for your next trip?", "What career tasks do you have outstanding?". This is the first AI query — pre-filled in the chat input, ready to send. User can tap to send or edit it first. This is the moment of first value.

**Step 7 — Completion**
Brief "You're all set" confirmation with the user's name and a summary: "X records imported, Y domains active." Transitions to Home.

**Files involved:**
- New: `features/onboarding/` — full feature module
- New: `features/onboarding/ui/Step1NameProfession.kt`
- New: `features/onboarding/ui/Step2DomainPacks.kt`
- New: `features/onboarding/ui/Step3PlatformDemo.kt`
- New: `features/onboarding/ui/Step4AiPromptGenerator.kt`
- New: `features/onboarding/ui/Step5BulkDocumentScan.kt`
- New: `features/onboarding/ui/Step6WowMoment.kt`
- New: `features/onboarding/viewmodel/OnboardingViewModel.kt`
- `data/src/main/java/com/lifepilot/data/pipeline/DocumentProcessingPipeline.kt`
- `app/src/main/AndroidManifest.xml` — READ_EXTERNAL_STORAGE / READ_MEDIA_DOCUMENTS permissions

---

## ISSUE-074 — Projects: replace Goals with intelligent workspaces
**Reported:** 2026-06-30 | **Priority:** P1

**Observed behaviour:**
Goals exist as an isolated planning construct. They are disconnected from Library records, AI conversations, documents, and domain life states. A user planning "Japan Trip 2026" must manage their visa tasks in Planner, their travel documents in Library, their AI conversations on Home, and their itinerary separately — with no single place that brings it together. Goals do not provide focused context. They are just titled task lists.

**Root cause (verified):**
The current `Goal` model is a simple container for tasks with a title and deadline. There is no concept of a scoped workspace. AI queries always search the user's entire life — there is no mechanism to tell the AI "I am working on Japan Trip, focus only on that." Library objects cannot be associated with a Goal. Documents, timeline entries, and domain knowledge cannot be surfaced within a Goal's context.

**Desired architecture:**

```
Home        → Life Brief + Today's Tasks + Active Projects + Alerts
Library     → Permanent source of truth (Records, Documents, Domain Life States)
Tasks       → Today | Upcoming | Projects
Settings    → Configuration
```

Projects live inside the Tasks tab — users open Tasks daily, which keeps Projects naturally visible without a dedicated nav tab.

**Project structure (each Project is a workspace with 7 tabs):**
- **Overview** — status, progress, AI recommendations, current blockers, upcoming milestones
- **Tasks** — tasks linked to this Project (same tasks visible in global Today/Upcoming — not duplicated)
- **Documents** — Library objects related to this Project (sourced from Library, not duplicated)
- **Timeline** — chronological events for this Project (visa approved, flight booked, etc.)
- **Knowledge** — cached relevant knowledge (Japan visa requirements, JR Pass guide)
- **AI** — dedicated AI workspace scoped to this Project's data only
- **Life State** — AI's current understanding of this Project (passport valid ✓, insurance missing ⚠)

**Project AI scoping:**
When inside a Project's AI tab, `RetrievalEngine` is called with a `projectId` parameter that restricts retrieval to: Project's linked documents, Project's tasks, Project's timeline, Project's knowledge, Project's conversations, Project's life state. This produces a smaller, more relevant context, faster responses, better SLM performance, and lower cloud cost.

**Relationship with existing architecture:**
- Library remains the permanent source of truth. Projects never duplicate documents or records — they surface them.
- Tasks remain the global planning system. Task rows show a Project badge (e.g. "🇯🇵 Japan Trip") when linked.
- `DomainLifeState` remains at the domain level. Projects have a separate `ProjectLifeState` which is a focused subset.
- The existing `Goal` model becomes the backend for `Project` with extended fields. Migration: all existing Goals become Projects. The "goal" concept is retired from the UI — users see "Project" everywhere.

**Project examples:**
- 🇯🇵 Japan Trip 2026
- 💼 Senior PM Job Search
- 🎓 PhD Completion
- 🏠 Buy First House
- 👶 Family Planning
- 🏋️ Get fit by December

**Planned fix approach:**
1. Extend `Goal` domain model with Project fields: `coverEmoji`, `projectLifeState: ProjectLifeState`, `linkedObjectIds: List<String>`, `linkedKnowledgeIds: List<String>`, `projectAiConversations: List<String>`, `projectTimelineEntries: List<String>`.
2. Add `ProjectLifeState` domain model: `currentStatus`, `completedMilestones`, `blockers`, `nextActions`, `confidenceScore`, `lastUpdatedAt`.
3. Rename all user-facing "Goal" strings to "Project" (ISSUE-053 alignment).
4. Tasks screen: add "Projects" subtab alongside Today and Upcoming. Project list card shows: emoji, title, task count, deadline, life state confidence chip.
5. Project workspace screen: 7-tab `HorizontalPager` with the tabs described above.
6. Project AI: `HomeViewModel` gains a `projectId: String?` parameter for `sendMessage()`. When set, `RetrievalEngine.retrieve()` applies project-scoped filtering.
7. Home screen: "Active Projects" section shows top 3 projects with quick-resume.
8. `PlanningEngine`: add `linkObjectToProject(projectId, objectId)`, `addKnowledgeToProject(projectId, knowledgeId)`.
9. DB migration: `goals` table gains new columns; existing rows are migrated automatically.

**Files involved:**
- `domain/src/main/java/com/lifepilot/domain/model/Goal.kt` — extend with Project fields (rename consideration)
- New: `domain/src/main/java/com/lifepilot/domain/model/ProjectLifeState.kt`
- `data/src/main/java/com/lifepilot/data/database/LifePilotDatabase.kt` — migration
- `features/planner/src/main/java/com/lifepilot/features/planner/ui/PlannerScreen.kt` — Today / Upcoming / Projects tabs
- New: `features/planner/src/main/java/com/lifepilot/features/planner/ui/ProjectWorkspaceScreen.kt`
- New: `features/planner/src/main/java/com/lifepilot/features/planner/ui/ProjectAiScreen.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt` — Active Projects section
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt` — project-scoped AI
- `data/src/main/java/com/lifepilot/data/engine/RetrievalEngineImpl.kt` — project-scoped retrieval

---

# P2 — Medium (continued)

---

## ISSUE-064 — No option to uncomplete a task; accidental completions are permanent
**Reported:** 2026-06-30 | **Priority:** P2
**Status: RESOLVED ✓** — Fixed by Claude (Session 5). `PlanningEngine.uncompleteTask()` implemented. Snackbar with "Undo" action shown for 5 seconds after task completion. `SwipeToReopenTask` composable added — swipe-left on completed tasks reopens them. `PlannerViewModel` wired with `undoComplete()`, `clearUndoState()`, `reopenTask()`.

**Observed behaviour:**
Once a task is marked complete — whether by the user or via AI proposal — there is no way to undo the completion. An accidental tap, a misidentified task completion by the AI, or a task that needs to be re-done has no recovery path. The task disappears from active views and cannot be restored to pending without deleting and recreating it.

**Root cause (verified):**
`PlanningEngine.completeTask()` sets `task.completedAt = Instant.now()` and `task.status = COMPLETED`. No `uncompleteTask()` or `reopenTask()` method exists. The Planner UI shows completed tasks in a separate collapsed section (if at all) with no action other than delete.

**Planned fix approach:**
1. Add `uncompleteTask(taskId: String): Result<Task>` to `PlanningEngine` — sets `completedAt = null`, `status = PENDING`.
2. In `PlannerScreen`, completed tasks section (or wherever completed tasks are shown): each row gets an "Undo" or "Reopen" action. Swipe-left → "Reopen" option alongside Delete.
3. Immediately after completing a task via tap, show a brief snackbar: "Task completed · Undo" with a 5-second timer. Tapping Undo calls `uncompleteTask()` without navigating away.
4. AI proposal `TaskCompletion` approval also surfaces the same 5-second undo snackbar after execution.
5. Apply generically: the same undo pattern should apply to Goal completion and Object archival.

**Files involved:**
- `domain/src/main/java/com/lifepilot/domain/engine/PlanningEngine.kt`
- `data/src/main/java/com/lifepilot/data/engine/PlanningEngineImpl.kt`
- `features/planner/src/main/java/com/lifepilot/features/planner/ui/PlannerScreen.kt`
- `features/planner/src/main/java/com/lifepilot/features/planner/viewmodel/PlannerViewModel.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt` — undo after AI task completion

---

## ISSUE-065 — Biometric lock limited to fingerprint; no face unlock or PIN/pattern fallback
**Reported:** 2026-06-30 | **Priority:** P2
**Status: RESOLVED ✓** — Fixed by Claude (Session 4). `setAllowedAuthenticators` changed to `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` (excludes Class 2 face unlock, includes PIN/pattern). Settings label renamed from "Biometric Lock" to "App Lock".

**Observed behaviour:**
The biometric lock uses `BiometricPrompt`. On devices without a registered fingerprint (or where the user prefers not to use one), the prompt fails or is unavailable. Face unlock (available on most mid-range and flagship devices as a Class 2 or Class 3 biometric) is not explicitly enabled. There is no PIN or pattern option as an alternative to biometrics. A user who cannot or does not want to use fingerprint has no way to enable the security lock.

**Root cause (verified):**
`BiometricAuthManager` constructs `BiometricPrompt.PromptInfo` with `setAllowedAuthenticators(BIOMETRIC_STRONG)`. `BIOMETRIC_STRONG` (Class 3) excludes face unlock on most devices because face unlock is classified as `BIOMETRIC_WEAK` (Class 2) by Android. No PIN/pattern fallback (`DEVICE_CREDENTIAL`) is configured.

**Planned fix approach:**
1. Change `setAllowedAuthenticators` to `BIOMETRIC_WEAK or DEVICE_CREDENTIAL`. This enables:
   - Fingerprint (Class 3 — always included)
   - Face unlock (Class 2 — included when available and enrolled)
   - PIN / Pattern / Password (DEVICE_CREDENTIAL — included as a fallback on all devices with a screen lock set)
2. In Settings > Security, rename "Biometric Lock" to "App Lock". Add a subtitle describing what authentication methods are available: "Fingerprint, face, or PIN — whatever your device supports."
3. When enabling App Lock, if no biometric is enrolled, show a clear message: "No biometric enrolled. You can still use your PIN or pattern." Do not block enabling the lock.
4. Test: devices with fingerprint only, face only, both, and neither (PIN-only). All must show the appropriate authenticator prompt.

**Files involved:**
- `data/src/main/java/com/lifepilot/data/security/BiometricAuthManager.kt`
- `features/settings/src/main/java/com/lifepilot/features/settings/ui/SettingsScreen.kt`

---

## ISSUE-067 — Object "Links" section: purpose unclear; candidate for removal or redesign
**Reported:** 2026-06-30 | **Priority:** P2
**Status: RESOLVED ✓** — Fixed by Claude (Session 4). `RELATIONSHIPS("Links")` tab removed from `ObjectDetailTab` enum. `RelationshipsTab` composable and its call site removed from `ObjectDetailScreen`. `Relationship` domain model retained for ISSUE-031.

**Observed behaviour:**
`ObjectDetailScreen` contains a "Links" or "Related" section near the bottom. The section is either empty or shows records with no clear explanation of what "linked" means in this context, how links are created, or what value they provide. Users tap into the section expecting to see connected records but find no guidance. It does not appear in the AI context and is not mentioned in any spec.

**Root cause (verified):**
The `Relationship` canonical entity (ISSUE-031) is partially scaffolded but not implemented. The "Links" section renders a placeholder or empty state because `ObjectRepository` has no relationship query. The section was added to signal future intent but was never wired to anything useful.

**Decision required — two options:**

*Option A — Remove the section entirely.*
Remove the Links section from `ObjectDetailScreen` until ISSUE-031 (Family / Relationship) and ISSUE-074 (Project Workspace Documents tab) are implemented. The section is confusing in its current state. When relationships and project associations are built, they will have proper dedicated UI.

*Option B — Redefine as "Also used in".*
Replace "Links" with an "Also used in" section showing any Projects (ISSUE-074) that have linked this record. A passport linked to the Japan Trip project would show "Japan Trip 🇯🇵" in this section. No relationship model needed — just a query for `Project.linkedObjectIds` containing this object's ID.

**Recommended:** Option A (remove) for now. Option B lands naturally when ISSUE-074 is implemented.

**Planned fix approach (Option A):**
Remove the Links / Related section composable and its empty state from `ObjectDetailScreen`. Remove the corresponding ViewModel call. Do not remove the `Relationship` domain model — it is needed for ISSUE-031.

**Files involved:**
- `features/object/src/main/java/com/lifepilot/features/object/ui/ObjectDetailScreen.kt`
- `features/object/src/main/java/com/lifepilot/features/object/viewmodel/ObjectDetailViewModel.kt`

---

## ISSUE-069 — Suggestion chips persist after first message; should dismiss once conversation starts
**Reported:** 2026-06-30 | **Priority:** P2
**Status: RESOLVED ✓** — Fixed by Claude (Session 4). `hasMessages: Boolean` parameter added to `AiInputBar`. Chips now shown only when `text.isEmpty() && !isLoading && !hasMessages`.

**Observed behaviour:**
The AI workspace shows a row of suggested prompt chips (e.g. "What documents are expiring?", "How is my career going?", "What do I need for my next trip?"). Once the user sends their first message, the chips remain visible and take up vertical space above the chat messages. They are no longer useful once a conversation is in progress.

**Root cause (verified):**
`HomeScreen`'s suggestion chip row is conditionally shown based on `uiState.messages.isEmpty()` (or equivalent). The condition is not updated after the first message is sent — or the condition does not exist at all and chips are always rendered in the AI workspace view.

**Planned fix approach:**
Show suggestion chips only when `uiState.messages.isEmpty()` AND `uiState.mode == HomeMode.AI_WORKSPACE`. Once the first user message is sent, chips permanently hide for that conversation session. When `startNewChat()` is called, chips reappear (new session has no messages). Single condition check — no new state field needed.

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt` — chip visibility condition

---

## ISSUE-071 — Library life areas expanded by default; overwhelming on first view
**Reported:** 2026-06-30 | **Priority:** P2
**Status: RESOLVED ✓** — Fixed by Claude (Session 4). `expandedDomains = emptySet()` default — all domains collapsed on load. Each domain section header is tappable with a chevron toggle. When a domain filter tab is active, that domain auto-expands.

**Observed behaviour:**
When the Library screen loads, all domain sections (Identity, Career, Finance, Health, Travel, Property, etc.) are expanded simultaneously showing all records within each. A user with even 10–15 records sees a wall of content. The overall domain structure is obscured. Scanning for a specific life area requires scrolling past many records. The most important piece of information — how many life areas have records and which ones — is buried.

**Root cause (verified):**
`LibraryScreen` renders domains as always-expanded `LazyColumn` groups or nested column blocks. No `expandedDomains: Set<String>` state exists in `LibraryUiState`. Each domain section header has no collapse toggle.

**Planned fix approach:**
1. Add `expandedDomains: Set<String>` to `LibraryUiState`. Default: `emptySet()` — all collapsed on first load.
2. Each domain section header becomes a tappable row: domain icon + name + record count badge + chevron (rotates on expand). Tapping toggles that domain in/out of `expandedDomains`.
3. When a domain filter tab is active (e.g. Career), auto-expand that domain and collapse all others.
4. Persist `expandedDomains` across navigation within the session (ViewModel-scoped — no need to persist to disk).
5. "Expand all / Collapse all" option accessible via overflow menu for power users.
6. When a new record is created or updated, auto-expand its parent domain for 2 seconds (highlight the new record) then re-collapse.

**Files involved:**
- `features/library/src/main/java/com/lifepilot/features/library/state/LibraryUiState.kt`
- `features/library/src/main/java/com/lifepilot/features/library/ui/LibraryScreen.kt`
- `features/library/src/main/java/com/lifepilot/features/library/viewmodel/LibraryViewModel.kt`

---

## ISSUE-073 — No share-to-LifePilot intent; documents shared from other apps are not captured
**Reported:** 2026-06-30 | **Priority:** P2

**Observed behaviour:**
When the user receives a PDF over WhatsApp, downloads a statement from their bank app, or receives a flight booking confirmation from a travel app, they cannot share it directly to LifePilot. The only path is to manually navigate to Library → Add New → Attach File — which requires leaving the app that has the document.

**Root cause (verified):**
LifePilot is not registered as an Android `ShareTarget`. The manifest has no `<intent-filter>` for `android.intent.action.SEND` or `android.intent.action.SEND_MULTIPLE` with `mimeType="application/pdf"` or `mimeType="image/*"`. `MainActivity` does not handle incoming share intents.

**Planned fix approach:**
1. Register LifePilot as a Share Target in `AndroidManifest.xml`: handle `ACTION_SEND` for `application/pdf`, `image/jpeg`, `image/png`, `image/webp`, `image/heic`.
2. In `MainActivity.onCreate()`, check `intent.action == Intent.ACTION_SEND`. If so, extract the URI from `intent.getParcelableExtra(Intent.EXTRA_STREAM)`.
3. Launch directly into `DocumentCaptureSheet` (or a lightweight `ShareReceiverActivity`) with the URI pre-loaded — do not drop the user on the Home screen.
4. The shared file is immediately passed through `DocumentProcessingPipeline`: OCR → classification → `AiProposal.ObjectCreation` with pre-populated fields. The user verifies the classification and saves.
5. For `ACTION_SEND_MULTIPLE` (multiple files shared at once): queue each file through the pipeline sequentially, presenting one verification card per file.
6. If LifePilot is locked (biometric), authenticate first, then proceed to the share receiver flow.
7. Never store the shared file in a temporary location visible outside the app's sandbox. Copy to internal storage immediately, release the original URI.

**Files involved:**
- `app/src/main/AndroidManifest.xml` — Share Target intent filter
- `app/src/main/java/com/lifepilot/app/MainActivity.kt` — intent handling
- `data/src/main/java/com/lifepilot/data/pipeline/DocumentProcessingPipeline.kt`
- New: `features/home/src/main/java/com/lifepilot/features/home/ui/DocumentCaptureSheet.kt`

---

## ISSUE-075 — AI processing shows no meaningful status; user sees a blank spinner
**Reported:** 2026-06-30 | **Priority:** P2
**Status: RESOLVED ✓** — Fixed by Claude (Session 4). Replaced inline `AiThinkingIndicator` with a `ThinkingBubble` composable that renders as an AI chat bubble showing "Thinking ▾". Tapping expands to reveal step messages and retry status. Uses `AnimatedVisibility` with `expandVertically + fadeIn`.

**Observed behaviour:**
While an AI request is processing, the UI shows a generic loading indicator (animated dots or spinner) with no information about what is happening. For a request that takes 5–15 seconds, this creates anxiety. The user does not know if the AI is retrieving records, calling the API, processing a large document, or silently failing.

**Root cause (verified):**
`HomeViewModel.sendMessage()` sets `isAiLoading = true` at the start and `false` at the end. There is no intermediate state. `HomeUiState` has no `aiStatusMessage` field. The pipeline steps (retrieval, prompt building, API call, parsing) complete sequentially with no UI updates between them.

**Planned fix approach:**
1. Add `aiStatusMessage: String?` to `HomeUiState`.
2. In `sendMessage()`, emit status updates at each pipeline stage by updating `aiStatusMessage` before and after each step:
   - After user message added: `"Searching your records..."`
   - After `retrievalEngine.retrieve()`: `"Found ${snapshots.size} relevant records..."`
   - After `promptBuilder.build()`: `"Thinking..."`  (don't expose technical detail here)
   - On API attempt 2 (retry): `"Taking a moment longer, retrying..."`
   - On API attempt 3: `"Still working on it..."`
3. Status message must reflect actual pipeline steps — never random/generic text like "AI is working" that does not correspond to a real stage.
4. When complete (success or error), `aiStatusMessage` is cleared.
5. Render `aiStatusMessage` as a small italicised line below the animated dots in the AI loading indicator, not as a separate message bubble.

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/state/HomeUiState.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt` — loading indicator with status text

---

## ISSUE-076 — No inline task creation from AI response; must use proposal card flow
**Reported:** 2026-06-30 | **Priority:** P2

**Observed behaviour:**
When the AI gives advice that implies an action — "you should renew your passport before the trip" — the user must dismiss the response, re-prompt the AI to propose a task, and then approve the proposal card. There is no shortcut to create a task directly from an AI message without going through the proposal flow. Actionable advice is easy to read and forget.

**Root cause (verified):**
AI message bubbles have no action row. The only way to create a task from an AI response is if the AI itself emits a `TASK_CREATION` proposal block — which requires the AI to interpret the user's intent as task-creating. Informational AI responses (advice, summaries, status checks) never produce proposals.

**Planned fix approach:**
1. Add an optional action row below each AI message bubble: icon-only buttons — "Create task" (clipboard+check), "Save to record" (file+arrow), "Copy" (copy icon). Shown only on long-press or via a small "⋯" menu chip below the bubble.
2. "Create task" opens a bottom sheet pre-filled with the message content truncated to 60 characters as the task title. User edits the title, sets due date (optional), priority, and links to a project or object. Saves via `planningEngine.createTask()` directly — no AI round-trip.
3. "Save to record" opens an `ObjectPickerSheet` — user selects which record to attach the insight to. Saves as a metadata note field via `metadataRepository.upsertMetadata()`.
4. These are manual actions initiated by the user — they bypass the AI proposal / approval flow entirely. No `AiProposal` is created. The user is the approver by definition (they tapped the button).
5. After saving, show a brief confirmation: "Task created" or "Note saved to [record name]." No additional approval required.

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/ui/components/MessageBubble.kt`
- New: `features/home/src/main/java/com/lifepilot/features/home/ui/QuickTaskSheet.kt`
- New: `features/home/src/main/java/com/lifepilot/features/home/ui/ObjectPickerSheet.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

---

## ISSUE-077 — No way to manually sync AI conversation insights to records from the chat
**Reported:** 2026-06-30 | **Priority:** P2

**Observed behaviour:**
After a rich AI conversation — e.g. discussing a new job offer, talking through a health concern, reviewing a travel plan — the insights and facts discussed exist only in the conversation transcript. There is no mechanism to bulk-review the conversation and push relevant facts into the appropriate records. The conversation is a dead end; its content does not enrich the Life State Engine unless the AI itself emits proposals (which it does unreliably for conversational content).

**Root cause (verified):**
`ConversationRepository` stores messages but there is no "conversation → records" sync pipeline. The `LifeStateEngine` evaluates domain state after each AI turn but does not extract and persist discrete metadata facts from the conversation into `MetadataRepository`. There is no UI surface in the chat for reviewing and accepting extracted insights in bulk.

**Planned fix approach:**
1. Add a "Sync conversation to records" action in the conversation overflow menu (three-dot menu in the chat header).
2. Tapping it sends the last N messages (configurable, default: full conversation) through a dedicated extraction prompt in `PromptBuilderImpl`: "Review this conversation and extract every factual claim about the user's life that should be stored as a record update. For each fact, identify: which record type it applies to, which field, and the value. Return as a structured list."
3. The AI response is parsed into a list of `AiProposal.MetadataUpdate` entries — one per extracted fact.
4. Present them as a scrollable review sheet: "X facts found in this conversation." Each fact is shown as an editable row (ISSUE-044 editable cards). User checks/unchecks each fact and taps "Save checked items."
5. Approved facts are saved via `metadataRepository.upsertMetadata()` with `source = AI_EXTRACTED`, `verificationStatus = PENDING`.
6. This sync action also triggers `domainLifeStateEngine.evaluateAndUpdate()` for all domains that received new facts.
7. The sync is always manual — never automatic. The user decides when conversation content is ready to be committed to records.

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/ui/AiChatScreen.kt` — overflow menu
- New: `features/home/src/main/java/com/lifepilot/features/home/ui/ConversationSyncSheet.kt`
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt` — extraction prompt
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`

---

## ISSUE-078 — No periodic document review; expiry events not proactively surfaced
**Reported:** 2026-06-30 | **Priority:** P2

**Observed behaviour:**
LifePilot has reminders that fire when documents expire (ISSUE-035 covers the morning brief). However, there is no weekly background review that proactively surfaces upcoming expiry events, required renewals, or approaching deadlines across all domains in a single digest. Users miss important dates not because reminders weren't set, but because individual reminders are forgotten or dismissed.

**Root cause (verified):**
`WorkManagerScheduler` schedules `MorningBriefWorker` (daily) and `ReminderEvaluationWorker` (periodic). No `WeeklyReviewWorker` exists. The reminder system fires individual notifications per reminder — it does not produce a weekly summary notification with a consolidated view of everything requiring attention in the next 30/60/90 days.

**Planned fix approach:**
1. Add `WeeklyReviewWorker` — runs every Sunday evening (configurable). Uses `ConstraintBuilder` with `requiresCharging(false)`, `requiresNetwork(false)` — must run without connectivity.
2. The worker queries all objects and metadata to compute: documents expiring in next 30 days, 60 days, 90 days; tasks overdue or due this week; goals with approaching deadlines; records last updated more than 6 months ago (may be stale).
3. Generates a notification with title: "Your weekly life review" and expandable content listing the top 5 items. Tapping the notification opens a new `WeeklyReviewScreen` with the full list.
4. `WeeklyReviewScreen` groups items: "Urgent — within 30 days", "Coming up — 30–60 days", "On the horizon — 60–90 days", "Possible stale records." Each item links to the relevant object.
5. User can act on each item inline: "Renew" creates a task, "Archive" marks the record archived, "Snooze" defers the review item by 30 days.
6. The review digest is also added to the Home Daily Brief on Monday mornings.
7. Toggle in Settings: "Weekly Review" — on/off, preferred day of week, preferred time.

**Files involved:**
- New: `data/src/main/java/com/lifepilot/data/worker/WeeklyReviewWorker.kt`
- New: `features/review/src/main/java/com/lifepilot/features/review/ui/WeeklyReviewScreen.kt`
- `data/src/main/java/com/lifepilot/data/worker/WorkManagerScheduler.kt`
- `features/settings/src/main/java/com/lifepilot/features/settings/ui/SettingsScreen.kt`

---

## ISSUE-080 — No in-app feedback mechanism; user has no path to report issues or suggestions
**Reported:** 2026-06-30 | **Priority:** P2

**Observed behaviour:**
Users have no way to send feedback from within the app. Issues, feature requests, and praise exist only in the user's head or as App Store reviews that arrive too late to act on. Early adopters — particularly during a private beta — are the most valuable source of product intelligence. Their inability to report issues in-context (from inside the app, at the moment of frustration) means most feedback is lost.

**Root cause (verified):**
No feedback UI or backend mechanism exists anywhere in the codebase.

**Planned fix approach:**
1. Settings > "Send Feedback" — opens a simple bottom sheet: feedback type selector (Bug, Suggestion, General), free-text field (minimum 10 characters), optional screenshot attachment.
2. Mechanism (V1): compose a pre-formatted email via `Intent.ACTION_SENDTO` addressed to a developer email. Subject: `[LifePilot Feedback] Bug / Suggestion / General — v<versionName>`. Body: free-text + auto-appended device info (Android version, device model — no personal data). No server required. Screenshot attached if provided.
3. Optional screenshot: uses `PixelCopy.request()` to capture the current screen. Redacts sensitive screens (ObjectDetailScreen, AI chat with sensitive content) — shows a greyed overlay instead of the actual content.
4. After sending: "Thank you for your feedback" confirmation. Dismissed automatically after 2 seconds.
5. V2 (future): replace email intent with a lightweight webhook endpoint that routes to a developer dashboard (Linear, Notion, or a simple Slack integration). This is the "feedback surfacing to the developer" mechanism — specific endpoint TBD.
6. Feedback must never include: personal metadata values, AI conversation content, document content, or any user-created records. Only device info and the user-written message.

**Files involved:**
- `features/settings/src/main/java/com/lifepilot/features/settings/ui/SettingsScreen.kt`
- New: `features/settings/src/main/java/com/lifepilot/features/settings/ui/FeedbackSheet.kt`
- New: `features/settings/src/main/java/com/lifepilot/features/settings/viewmodel/FeedbackViewModel.kt`

---

## ISSUE-082 — Life Completeness score: persistent measure replacing mandatory onboarding forms
**Reported:** 2026-06-30 | **Priority:** P2

**Observed behaviour:**
Onboarding (ISSUE-014, ISSUE-072) collects initial information. But once onboarding is done, there is no persistent signal to the user about how complete their profile is or what they could add to get more value from LifePilot. A user with only 2 records gets the same Home screen as a user with 50. There is no motivation to continue adding information.

**Root cause (verified):**
No completeness scoring algorithm or domain weighting exists. The profile model has no `completenessScore` or per-domain completeness breakdown. Home screen has no completeness indicator.

**Desired state:**

```
Life Completeness
72%
██████████░░░░░░

✅ Identity        (4/4 key records)
✅ Documents       (passport, licence, PAN)
🟡 Career          (job record — missing certifications)
🟡 Finance         (bank account — missing investments, insurance)
🟡 Health          (none added)
🟡 Travel          (passport — missing visa for upcoming trip)
🟡 Home & Property (none added)
🟡 Vehicles        (none added)
🟡 Family          (none added)
🟡 Emergency       (none added)
```

**Planned fix approach:**
1. Define `completenessWeights: Map<Domain, Map<FieldCategory, Float>>` in the schema engine — what percentage of a domain's score each category contributes. Identity with a valid passport and Aadhaar = 100% of Identity basics. Career without a resume = 60%.
2. `CompletenessEngine` (new, domain service) computes: per-domain score (0–100%), overall score (weighted average). Refreshes whenever a record is added, a field verified, or a domain life state updated. Result stored in `DomainLifeState.completenessScore`.
3. Home screen: "Life Completeness" row below the greeting — shows `72% complete` with a `LinearProgressIndicator`. Tapping opens `CompletenessDetailScreen`.
4. `CompletenessDetailScreen`: per-domain breakdown. Each domain row: progress bar, count of key records, list of "What to add next" suggestions specific to that domain. Tapping a suggestion navigates to the appropriate add flow.
5. Score is always visible and always increasing — never decreases when information is removed (to avoid discouraging cleanup).
6. Onboarding step completion contributes to the score — Step 1 completion bumps Identity, Step 5 document import bumps relevant domains.

**Files involved:**
- New: `domain/src/main/java/com/lifepilot/domain/engine/CompletenessEngine.kt`
- `domain/src/main/java/com/lifepilot/domain/model/DomainLifeState.kt` — add `completenessScore`
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt` — completeness row
- New: `features/home/src/main/java/com/lifepilot/features/home/ui/CompletenessDetailScreen.kt`

---

# P3 — Low (continued)

---

## ISSUE-070 — AI suggestion chips are static; not personalised from user history
**Reported:** 2026-06-30 | **Priority:** P3

**Observed behaviour:**
The suggestion chips shown on the empty AI workspace are hardcoded strings (e.g. "What documents are expiring?", "How is my career going?"). They are identical for every user regardless of what records they have, what events are upcoming, or what they have asked before. A user who just uploaded their visa and has a trip in 3 weeks sees the same chips as a user who joined last week with no records.

**Root cause (verified):**
Suggestion chips are hardcoded in `HomeScreen.kt` as a static list of strings. No dynamic generation logic exists. `HomeViewModel` does not compute context-aware suggestions. `RetrievalEngine` results are available but not used for chip generation.

**Planned fix approach:**
1. Add `suggestionChips: List<SuggestionChip>` to `HomeUiState`. `SuggestionChip` has `text: String`, `icon: ImageVector?`.
2. `HomeViewModel.generateSuggestions()` — called after `observeBriefData()` completes and produces its first emission. Computes suggestions based on:
   - Upcoming reminders in the next 14 days → chip for each reminder's domain ("Check your travel documents")
   - Active goals → chip for the highest-priority active goal ("How is Japan Trip going?")
   - Recently added records → chip referencing the newest record type ("Tell me about my new insurance policy")
   - Most recent conversation topic → chip proposing a follow-up ("Continue our discussion about your visa")
   - Fallback static chips if no context is available (as today)
3. Suggestions are re-generated when `observeBriefData()` updates (i.e. whenever records, goals, or conversations change).
4. Cap at 5 chips maximum. Randomise order slightly so the same chip is not always first.
5. No AI call to generate chip suggestions — pure deterministic logic based on existing data. Fast, offline.

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/state/HomeUiState.kt`
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt`
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt`

---

## ISSUE-079 — No local knowledge base; every internet query starts from zero
**Reported:** 2026-06-30 | **Priority:** P3

**Observed behaviour:**
Each time the AI searches the internet (Layer 3 — ISSUE-024), it performs a fresh search with no memory of previous results. Common procedural knowledge (visa requirements, passport renewal steps, tax filing deadlines, vaccination requirements by country) is re-fetched every time, even though it changes rarely. This wastes search quota, adds latency, and produces inconsistent answers if search results vary between calls.

**Root cause (verified):**
No local knowledge cache or knowledge base exists. `InternetIntelligenceOrchestrator` (when implemented) will call the search provider directly and return results without persisting them. There is no `KnowledgeRepository` or `KnowledgeEntry` model in the domain layer.

**Planned fix approach:**
1. Add `KnowledgeEntry` domain model: `(knowledgeId, topic, domain, content, sources: List<String>, fetchedAt: Instant, expiresAt: Instant, confidenceScore: Float, isUserPersonalized: Boolean)`.
2. `KnowledgeRepository` — CRUD + query by `(topic, domain)`. Room entity + DAO.
3. **Cache lookup before internet search**: `InternetIntelligenceOrchestrator` checks `KnowledgeRepository.findByTopic(topic)` first. If an entry exists and `fetchedAt` is within `Y` days (default: 7 days for regulatory knowledge, 1 day for prices/rates), use the cached entry — no internet call.
4. **Background refresh**: when a knowledge entry is used and its age exceeds 50% of its TTL, queue a background refresh via `WorkManager`. The refresh updates the entry without blocking the user.
5. **Personalized knowledge**: certain knowledge entries are personalized from user questions. Example: user repeatedly asks about Japan visa requirements → a personalized "Japan Visa Guide" entry is maintained and updated with content specific to their passport type and travel history. `isUserPersonalized = true` entries have shorter TTLs (3 days) since personal relevance matters more.
6. **Fixed base knowledge**: a small set of always-populated knowledge entries is bundled with the app: Indian passport travel requirements overview, common document renewal timelines (passport: 10 years, driving licence: 20 years), EPFO/tax filing calendar, standard healthcare claim steps. These are pre-seeded and refresh monthly in the background.
7. **Knowledge surfaced in AI context**: `RetrievalEngine` includes relevant `KnowledgeEntry` content in `RetrievalContext.knowledgeEntries`. `PromptBuilderImpl` includes them in the prompt as "Cached knowledge (last verified X days ago)".

**Files involved:**
- New: `domain/src/main/java/com/lifepilot/domain/model/KnowledgeEntry.kt`
- New: `domain/src/main/java/com/lifepilot/domain/repository/KnowledgeRepository.kt`
- New: `data/src/main/java/com/lifepilot/data/database/entity/KnowledgeEntryEntity.kt`
- New: `data/src/main/java/com/lifepilot/data/repository/KnowledgeRepositoryImpl.kt`
- New: `data/src/main/java/com/lifepilot/data/worker/KnowledgeRefreshWorker.kt`
- `data/src/main/java/com/lifepilot/data/engine/RetrievalEngineImpl.kt`
- `data/src/main/java/com/lifepilot/data/engine/PromptBuilderImpl.kt`

---

## ISSUE-081 — No product analytics or funnel tracking; product decisions made blind
**Reported:** 2026-06-30 | **Priority:** P3

**Observed behaviour:**
No usage data is collected. There is no way to know: what percentage of users complete onboarding, where users drop off, how many users actually use the AI, how many records the average user creates, or whether any feature is broken without a user manually reporting it. Product decisions are made entirely from dogfooding — with no signal from real users.

**Root cause (verified):**
No analytics library, event tracking system, or funnel definition exists anywhere in the codebase.

**Planned fix approach:**
1. `AnalyticsEvent` sealed class — type-safe event definitions:
   ```
   app_opened, onboarding_started, onboarding_step_completed(step: Int),
   onboarding_completed, permission_granted(permission: String),
   document_imported, document_verified, ai_query_sent, ai_query_completed,
   life_brief_generated, library_opened, task_created, task_completed,
   project_created, project_ai_used, record_created, record_deleted,
   search_performed, feedback_submitted, settings_opened,
   premium_clicked, premium_started, premium_purchased
   ```
2. `AnalyticsService` interface — single method: `track(event: AnalyticsEvent)`. Injected everywhere events are needed.
3. V1 implementation: `LocalAnalyticsService` — writes events to a `analytics_events` Room table with `(eventId, eventName, properties: String JSON, occurredAt)`. No external service, no network calls. Keeps LifePilot fully offline. Privacy: event names and properties must never contain personal data (names, document values, AI content).
4. **Funnel tracking table** — `user_funnel_milestones`: records first time each milestone was reached (Install, First Open, Onboarding Completed, First Document, First AI Query, First Project, First Verified Record, Day 2 Return, Day 7 Return, Day 30 Return).
5. **Developer-only analytics screen** — hidden behind a developer menu in Settings (accessed by tapping the version number 5 times, similar to Android developer options). Shows: funnel completion rates, average events/session, most-used features, most common drop-off points.
6. **V2 migration path**: `AnalyticsService` interface is the only touch point. Replacing `LocalAnalyticsService` with `FirebaseAnalyticsService` or `PostHogAnalyticsService` requires adding one class and changing the DI binding. No other code changes.
7. **Performance**: events are written to Room asynchronously on a background dispatcher. Never blocks the UI thread. Batch write every 30 seconds.

**Files involved:**
- New: `domain/src/main/java/com/lifepilot/domain/analytics/AnalyticsEvent.kt`
- New: `domain/src/main/java/com/lifepilot/domain/analytics/AnalyticsService.kt`
- New: `data/src/main/java/com/lifepilot/data/analytics/LocalAnalyticsService.kt`
- New: `data/src/main/java/com/lifepilot/data/database/entity/AnalyticsEventEntity.kt`
- New: `features/settings/src/main/java/com/lifepilot/features/settings/ui/DeveloperAnalyticsScreen.kt`
- `data/src/main/java/com/lifepilot/data/di/AnalyticsModule.kt`

---

## ISSUE-083 — AI thinking indicator is generic; does not reflect actual pipeline steps
**Reported:** 2026-06-30 | **Priority:** P3

**Observed behaviour:**
When the AI is processing a query, the app shows an animated loading indicator with no information about what is happening. The experience is identical whether the AI is doing a 1-second deterministic lookup or a 12-second multi-step retrieval with internet search. Nothing about the display is personalised: a user asking "how is my career going?" sees the same spinner as one asking "what documents do I need for a Japan visa?" — even though the second query involves retrieving 5 records, calling internet search, and synthesising a multi-domain response.

**Root cause (verified):**
ISSUE-075 covers the basic status message fix (emit pipeline step messages during processing). This issue extends that to ensure the status messages reflect actual retrieved content — not just stage names. The thinking indicator is generic even after ISSUE-075 is resolved if messages like "Checking your records..." do not reference what was actually found.

**Planned fix approach (builds on ISSUE-075):**
After ISSUE-075 ships its stage-name messages, enhance them with content-aware detail:
- After retrieval: instead of "Found 3 records", show "Checking your Passport, Star Health Insurance and Japan Trip 🗺️…"
- After domain life state load: "Reviewing your Career and Finance understanding…"
- During API call (after a few seconds): "Generating a personalised response for you…" (never "Calling AI API" — that's internal)
- On retry: "That took longer than expected, trying again…"
These messages use the actual titles from `currentObjectIndex` and `currentRetrievalDomains` which are already available in `HomeViewModel` after retrieval.
Messages must be warm and personal — written as if a knowledgeable assistant is describing what they are doing on the user's behalf, not describing a technical process.

**Files involved:**
- `features/home/src/main/java/com/lifepilot/features/home/viewmodel/HomeViewModel.kt` — content-aware status messages post-retrieval
- `features/home/src/main/java/com/lifepilot/features/home/ui/HomeScreen.kt` — loading indicator with personalised status

---

## ISSUE-084 — SLM model selection is manual; device capability not detected automatically
**Reported:** 2026-06-30 | **Priority:** P3

**Observed behaviour:**
The on-device SLM (ISSUE-024) will require manual model selection or a hardcoded default. A user with a Pixel 9 Pro (16 GB RAM, Tensor G4) gets the same model as a user with a Redmi 12 (4 GB RAM, Snapdragon 4 Gen 2). The large-model user is under-served. The small-device user may experience crashes, extreme battery drain, or OOM errors if the wrong model is selected.

**Root cause (verified):**
ISSUE-024 defines the `EmbeddedModelInterface` abstraction and specifies Gemma 4 E2B as V1 default. No device capability detection, benchmark, or adaptive model selection mechanism is specified.

**Planned fix approach:**
1. `DeviceCapabilityDetector` — reads: `ActivityManager.MemoryInfo.totalMem`, SoC brand/model (`Build.SOC_MANUFACTURER` + `Build.SOC_MODEL`), Android version, available storage.
2. **Model selection matrix:**
   | Device tier | Criteria | Recommended model |
   |---|---|---|
   | Flagship | ≥8 GB RAM, SD 8-series / Tensor G3+ / Dimensity 9000+ | Gemma 4 4B via AICore |
   | Mid-range | ≥6 GB RAM, SD 7-series / Tensor G1+ | Gemma 4 E2B via AICore |
   | Entry-mid | ≥4 GB RAM, SD 6-series or equivalent | Gemma 2B Q4 via MediaPipe |
   | Entry / Low | <4 GB RAM or unsupported SoC | Cloud-only mode (no on-device model) |
3. **Optional inference benchmark**: on first launch after model download, run a 5-token "hello world" generation and measure tokens/second. If throughput is below a threshold (e.g. <2 tokens/second), recommend dropping to a smaller model. The benchmark takes <3 seconds.
4. **User override**: Settings > AI > On-Device Model allows the user to manually select any model their device can theoretically run. Shows a warning if selecting above the detected tier. Shows estimated download size and RAM usage for each option.
5. **AICore availability check**: `GoogleAICore.isAvailable()` is called first. If AICore is available and device is mid-range or above, use AICore path. If AICore is unavailable, fall back to MediaPipe on the same or smaller model.
6. Model selection runs once at first launch (after onboarding). If the user changes device storage significantly, re-evaluate on the next app update.

**Files involved:**
- New: `data/src/main/java/com/lifepilot/data/device/DeviceCapabilityDetector.kt`
- New: `data/src/main/java/com/lifepilot/data/ai/embedded/ModelSelector.kt`
- `data/src/main/java/com/lifepilot/data/ai/AiProviderFactory.kt`
- `features/settings/src/main/java/com/lifepilot/features/settings/ui/SettingsScreen.kt` — model override UI
- `features/onboarding/` — model detection and download step
