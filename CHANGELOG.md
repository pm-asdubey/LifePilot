# Changelog

All notable changes to LifePilot are documented here.

---

## [Unreleased] — Continuation, Deterministic Formatting & Prompt Slimming (2026-07-04)

Driven by on-device marriage-flow testing. Same-day continuation of the pass below.

### Fixed
- **Raw action JSON leaked into the chat on long plans (RCA).** A 14-task ACTION_PLAN exceeded the
  provider `max_tokens` (1024) and truncated mid-JSON, so there was no closing `[/LIFEPILOT_ACTION]`
  tag, `ACTION_PATTERN` didn't match, and the raw block (underscores eaten) was shown with no approval
  card. Fixed by (a) raising `max_tokens` to 4096, (b) **auto-continuation** (below), and (c) a
  parse-time safety net that strips any dangling unclosed block and shows a retry hint.
- **Library "Current Understanding" card had a delete/cross button.** It was a session-only dismiss
  that read as delete on AI-managed understanding. Removed the affordance (and its state/params).

### Added
- **AI response continuation.** `AiCompletionResult.Success` now carries a `truncated` flag from the
  provider's `finish_reason`/`stop_reason` (NVIDIA + Anthropic). `HomeViewModel.completeWithContinuation`
  transparently asks the model to continue (up to `MAX_CONTINUATIONS` = 2), assembles the chunks, and
  only then parses/displays — so big plans arrive whole, as one message with the approval card.
- **Styled Markdown rendering, dependency-free.** `MarkdownInline` (`designsystem/text/`, unit-tested)
  parses `**bold**`, `*italic*` and `#` headers into a Compose `AnnotatedString`; `MessageBubble`
  renders AI text through it. Plus `normalizeNumberedList` splits run-on numbered lists onto their own
  lines. We roll our own because the maintained Compose-Markdown libraries need Kotlin 2.2 / JitPack and
  the project is on Kotlin 2.0.
- **Thoroughness & credibility prompt block.** Instructs the AI to be exhaustive by default, think
  across ALL affected life domains, be specific per named record, and cite the records it reviewed.
- **Background AI work + "answer ready" notification.** A lightweight foreground service
  (`AiThinkingService`, declared in app, class in `data`) keeps the process alive while the existing
  `viewModelScope` AI call runs, so switching apps no longer freezes the request. `AiTaskNotifier`
  starts it on turn start and, on completion, posts an "Your answer is ready" notification **only if the
  app is backgrounded** (checked via `ForegroundStateProvider`/`ProcessLifecycleOwner`), deep-linking to
  the conversation (`conversationId` extra → `MainActivity` → `HomeScreen.resumeConversation`). New perms:
  `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC`. Gating logic unit-tested (`AiTaskNotifierTest`).

### Changed
- **Friendly AI errors.** Raw provider errors (e.g. `ETIMEDOUT`) are mapped to calm messages
  (connection / key / rate-limit variants); the raw text goes to logcat only.
- **Slimmer system prompt (enforcement moved into code).** The 30-item trigger list is now one line;
  the ACTION_PLAN schema no longer instructs a `CREATE_PROJECT` item or `projectItemId` because
  `ActionPlanNormalizer` injects the project and links tasks/records deterministically. Removed the
  formatting instructions (handled deterministically). The TURN 1/2/3 structure and few-shot examples
  were left intact (they work well).

### Not done (tracked)
- Persisting an attachment reference on the chat message bubble.

---

## [Unreleased] — Attachment Flow, Parser & Projects Pass (2026-07-04)

On-device testing of the `stable` (no-onboarding) build drove this pass. Build/verify loop via USB ADB.

### Changed
- **Document → chat flow reworked so nothing is stored on scan.** `HomeViewModel.processAttachment` now
  *queues* the document (chip in chat) and runs OCR + AI classification in the background without storing.
  The document travels with the user's first message (`sendMessage`), the AI answers grounded in it, then
  the classification is presented for save (approval → PDF record + Life State Engine event), or a general
  document is saved if classification failed. Previously it OCR'd + classified + **stored on scan** before
  any message — the cause of "blanked out then saved instantly without a message."
- **Camera ≠ Scan.** Camera is a normal camera again (`TakePicture`); only "Scan a document" opens the ML
  Kit scanner. A photographed document is still OCR'd/classified; a plain photo is just kept.
- **AI thinking state always cycles.** The indicator previously froze whenever the VM set a status; it now
  always rotates the relevant words, with the compass avatar + WhatsApp-style animated dots. The AI message
  avatar is the compass too.
- **In-app logo is a real compass.** `LifePilotLogo` rewritten from the 4-point star (read as a shuriken)
  to a ring + N/E/S/W ticks + an elongated needle that points up — consistent with the launcher icon.

### Fixed
- **Scanned documents failing to classify (RCA).** `AiActionParser.parseAction` threw
  `JSONException: Value OBJECT_CREATION … cannot be converted to JSONObject` because the model emitted a
  bare action-type token before the JSON. The parser now isolates the outermost `{…}` and recovers the
  action type from a stray prefix / markdown fences. Classification prompt also strengthened with a JSON
  example.
- **Tasks showed two circles.** `SwipeToCompleteTask`/`SwipeToReopenTask` rendered a check/reopen icon at
  the card's left edge that showed through the transparent card at rest. Removed both swipe wrappers — each
  task now has a single tap-to-complete/reopen circle.

### Added
- **Projects are mandatory for grouped work.** A prompt MULTIPLE-TASK RULE plus a deterministic safety net:
  `ActionPlanNormalizer.ensureProject` injects a synthetic Project (and links tasks/records) whenever a plan
  has 2+ tasks and none — regardless of model compliance. Applied at parse time so it shows in the proposal.
- **Tests:** `ActionPlanNormalizerTest` (6), `ThinkingMessagesTest` (3), AiActionParser stray-prefix +
  markdown-fence cases (2). New `:designsystem` unit-test source set. JVM suite remains green.

### Distribution
- `stable` flavor (`com.lifepilot.app.stable.debug`, no onboarding) is the primary build installed on
  device. Onboarding remains behind the `standard` flavor. Artifact: `artifacts/LifePilot-stable-no-onboarding-debug.apk`.

---

## [Unreleased] — Stability & Maintainability Pass (2026-07-03)

### Added
- **Library: all 12 canonical domains are always-visible containers.** Every life domain shows as an
  expandable section even at zero records, each with a domain-scoped "+" to file a record/document into
  it; first-run onboarding banner; per-domain "Current Understanding" life-history cards. Regression
  test guards that all canonical domains are always present.
- **`DomainEmoji`** deterministic domain→emoji map (ADR-002) so AI-proposed projects get a
  domain-appropriate emoji instead of a generic marker. Unit-tested.
- **New compass/pilot logo** — adaptive launcher icon (foreground/background/monochrome) + reusable
  `LifePilotLogo` brand mark in `designsystem`.
- **Documentation:** `Stability-And-Maintainability.md`, `Architectural-Review-2026-07.md`,
  `Onboarding-Flow-Analysis.md` + interactive HTML mockup, `Manual-QA-Checklist.md`; refreshed
  `TESTING.md` (246 tests across 7 modules).

### Fixed
- **AI-proposed projects now carry emoji + target date** end-to-end (parsers, `AiProposal.ProjectCreation`,
  `ActionItem.CreateProject`, `HomeViewModel`, `ActionPlanExecutorImpl`). Previously every AI project got
  `🎯` and no date, so "days left" never rendered and the "Behind" heuristic could never fire.
- **`DomainLifeStateEngineImpl` markdown-fenced JSON parsing** — model responses wrapped in ```json fences
  were silently dropped (no life-state update). Now extracts the outermost `{…}` robustly.
- **Camera flow (AI chat):** runtime CAMERA permission is now requested before capture (was a
  SecurityException/crash risk on some OEMs); the capture URI survives process death
  (`rememberSaveable`); an empty AI response no longer leaves the "thinking" spinner spinning forever.

### Removed
- **Orphaned `features:ai` module** (918 lines, incl. a third divergent copy of the AI action parser) —
  not wired into the app; deleted along with its `settings.gradle` include.
- **Orphaned Library `ProjectDetail` screen/viewmodel/state + duplicate `project/{projectId}` route** —
  it collided with and shadowed Planner's Project Workspace (ADR-002: Library is Records-only).

### Testing / Infra
- Recovered and unified the test suite after a prior multi-agent session left divergent worktrees.
  **246 JVM unit tests, 0 failures.** Added `org.json` test dependency to `:data` (fixes silent
  parse-failure in JVM unit tests). CI gate (`./gradlew test`) verified.

---

## [Unreleased] — P0 Bug Fixes

### Fixed

- **ISSUE-004** — Search screen no longer crashes: replaced conditional early-return layout with stable `when`-based content and aligned placeholder/empty copy with smoke tests.
- **ISSUE-018** — `FLAG_SECURE` verified on `MainActivity`; added `SensitiveFieldRegistry` and a `ScrubbingTree` so Timber logs never emit sensitive field values.
- **ISSUE-055** — Added formal data-flow documentation (`docs/security/DATA_FLOW.md`) and privacy declaration (`docs/security/PRIVACY_DECLARATION.md`); `PromptBuilderImpl` now scrubs structured metadata before transmission; automated redaction tests added.
- **ISSUE-001** — New AI conversations no longer append to the first conversation: added `startNewConversation()` and auto-clear conversation ID after an idle threshold when returning to the AI workspace.
- **ISSUE-022** — AI status changes now work: added `STATUS_UPDATE` parsing and execution path in `HomeViewModel`, plus a `StatusUpdateCard` proposal UI.
- **ISSUE-025** — Document re-upload no longer duplicates AI-extracted metadata: stale extracted metadata is deleted before saving verified fields, and `ObjectReasonerImpl` deduplicates any existing duplicate field rows.

---

## [Unreleased] — Architecture Stabilization Pass

### Added

- `RetrievalEngine` domain interface and `RetrievalEngineImpl` — keyword scoring (title=3pt, type=2pt, domain=1.5pt, metadata=0.5pt), selects max 5 objects per AI request
- `PromptBuilder` domain interface and `PromptBuilderImpl` — pure string formatting, no I/O, converts `RetrievalContext` into a structured system prompt
- `ObjectReasoner` domain interface and `ObjectReasonerImpl` — builds `ObjectSnapshot` per object (metadata, task count, document count, `AiObjectContext`)
- `AiObjectContext` domain model — structured AI analysis stored as JSON in `metadata` table under `fieldId = "ai_context"`
- `ObjectSnapshot` domain model — per-object view passed from `RetrievalEngine` to `PromptBuilder`
- `RetrievalContext` domain model — complete input to `PromptBuilder` (snapshots, task index, reminder index, full object index)
- `AiProposal` sealed class replacing legacy `ProposedAction`: `MetadataUpdate`, `GoalProposal`, `TaskCompletion`, `TaskCreation`, `ObjectCreation`
- `PlanningEngine` domain interface — single mutation path for all goal and task state changes (`createGoal`, `completeGoal`, `archiveGoal`, `createTask`, `completeTask`, `cancelTask`)
- `GoalStatus` enum expanded: `DRAFT`, `PROPOSED`, `ACTIVE`, `COMPLETED`, `ARCHIVED`, `CANCELLED`
- `TaskSource` enum expanded: `MANUAL`, `GOAL`, `RECURRING`, `SYSTEM`, `AI_PROPOSED` (legacy `USER`, `RULE_ENGINE`, `AI` retained)
- `Task.goalId: String?` field linking tasks to goals
- `VerificationStatus` enum: `UNVERIFIED`, `VERIFIED`, `REJECTED`
- `MetadataEntry.verificationStatus` field (default `UNVERIFIED`)
- `MetadataRepository.verifyMetadata()` and `rejectMetadata()` methods; `upsertMetadata` now accepts `verificationStatus` parameter
- `MetadataRepository.getMetadataForObjects()` batch fetch method
- `MIGRATION_2_3` — adds `verification_status TEXT NOT NULL DEFAULT 'UNVERIFIED'` to `metadata` table; DB version now 3
- `MIGRATION_1_2` — adds `goals`, `conversations`, `chat_messages` tables and `goal_id` column to `tasks` table; DB version 2
- `GoalEntity`, `ConversationEntity`, `ChatMessageEntity` Room entities
- Home screen dual-mode: `DAILY_BRIEF` (attention items, goals, recent conversations) and `AI_WORKSPACE` (conversation + proposal cards)
- Structured proposal cards in `designsystem/`: `GoalProposalCard`, `TaskCompletionCard`, `TaskCreationCard`, `ObjectCreationCard`, `ActionProposalCard` (MetadataUpdate)
- AI Workspace in `HomeViewModel` — single AI entry point: `sendMessage()` → `RetrievalEngine` → `PromptBuilder` → `AiProvider` → `parseAiResponse()` → proposal card → `executeProposal()`
- `MorningBriefWorker` — WorkManager periodic job, fires daily at 9 AM, shows notification with pending task count and active goal count
- Library screen: domain-grouped Object Tree with sticky section headers when no domain filter is selected
- Search expanded to include Goals and Tasks in addition to Objects, Metadata, Documents
- Object Detail: `ai_context` metadata field rendered as a distinct `secondaryContainer` "AI Context" card (not in the regular Details list)
- Object Detail: `ProvenanceBadge` on every metadata entry showing source and verification status
- DI bindings added to `RepositoryModule` for `ObjectReasoner`, `RetrievalEngine`, `PromptBuilder`, `PlanningEngine`
- ADR-008 documenting all Architecture Stabilization Pass decisions

### Removed

- `features/ai` module (`AiChatViewModel`, `AiChatScreen`) detached from `app/build.gradle.kts`; all AI interaction now routes through `HomeViewModel`

---

## [1.0.0-rc1] — Release Candidate

### Release Prep
- versionName updated to `1.0.0`, versionCode = 1
- Removed unused permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `READ_MEDIA_VIDEO`
- Fixed `android:autoVerify` on custom URI scheme deep link (autoVerify is invalid for non-http schemes)
- Wired Notifications bell on HomeScreen to navigate to Timeline (was dead `onClick = {}`)
- Removed empty stubs `indexObject` / `removeFromIndex` from `SearchRepository` interface and implementation
- Removed `compose.compiler` plugin from `:data` module (data has no Compose code)
- Removed dead version catalog entries: moshi, moshi-codegen, retrofit, accompanist, pdfRenderer, detekt, ktlint
- Added signing configuration (reads from `local.properties` — never committed)
- Created `RELEASE_CHECKLIST.md` with complete pre-release verification requirements
- Created `ARCHITECTURE.md` documenting module structure, engines, data flow, and design decisions
- Created `data/schemas/` directory for Room schema export

## [Unreleased] — 0.11.0-alpha

### Added
- `driving_licence` schema (17th schema) with expiry reminders, all standard fields
- `MetadataRepository.getMetadataForObjects()` batch method to eliminate N+1 queries
- `ObjectDao.getObjectsByIds()` batch method for search result hydration
- DocumentDao instrumented tests (8 cases: insert, object isolation, delete, versions, OCR text, search)
- TimelineDao instrumented tests (6 cases: ordering, profile isolation, object/domain filter, delete, limit)
- RelationshipDao instrumented tests (6 cases: bidirectional, between-query, filtering, delete)
- EventDao instrumented tests (5 cases: ordering, isolation, cascade, empty)
- Search flow UI tests (4 cases: bar visible, empty state, text input, clear)
- Document upload flow UI tests (2 cases: sheet opens, schema types load)

### Fixed
- N+1 metadata query in AI chat context builder replaced with single batch query (capped at 30 objects)
- N+1 object queries in SearchRepositoryImpl replaced with `getObjectsByIds` batch calls
- `LazyColumn` inside `Column(fillMaxSize)` now uses `Modifier.weight(1f)` in Library, Search, Timeline screens
- Schema domain values normalized to Title Case: Finance, Travel, Home, Transport (was mixed case)
- CURRENCY field type now shows decimal keyboard in MetadataEditScreen

## [0.10.0-alpha] — 2026-06-27

### Added
- PDF rendering using Android `PdfRenderer` API: all pages rendered at 1080px width with loading/error states
- Multi-select bulk operations in Library: long-press to enter selection mode, Archive/Delete buttons in bottom action bar with confirmation dialog
- Haptic feedback on task completion (check button) and swipe-to-complete gesture
- Animated screen transitions (slide+fade) via NavHost enter/exit/pop animations
- Pull-to-refresh on Home, Library, and Timeline screens
- Swipe-to-complete on task cards in Object Detail screen
- AI unconfigured setup banner in AI Chat screen with Settings deep link
- Instrumented Room DAO integration tests: ObjectDao (8), TaskDao (6), MetadataDao (7), ReminderDao (5)
- FileStorageManager integration tests (7 cases)
- Navigation smoke tests (5 cases), Create object smoke tests (2 cases)
- HiltTestRunner for instrumented test setup

### Fixed
- All fully-qualified class references in feature composables replaced with proper imports
- `ObjectCard` now uses `combinedClickable` for long-press support
- `PdfRenderer` state updates correctly happen on main thread after `withContext(Dispatchers.IO)`

## [0.9.0-alpha] — 2026-06-27

### Fixed
- Metadata saved with correct `fieldType` (DATE, NUMBER, ENUM) instead of always TEXT
- `fieldType` now threaded from schema → `ExtractedField` → `FieldSuggestion` → `MetadataEntry`

---

## [0.8.0-alpha] — 2026-06-27

### Added
- Task deduplication: prevent duplicate tasks when `OBJECT_CREATED` is processed twice
- `RuleEngine` interface extracted from `RuleEngineImpl` for proper layer decoupling (ADR-005)
- `kotlinx.serialization.json` used in domain layer for JVM-testable JSON parsing (ADR-006)
- `ExportDataUseCaseTest` covering payload structure, metadata inclusion, error paths
- `GetObjectWithMetadataUseCaseTest` covering success, missing object, empty metadata
- AI system prompt cached in `AiChatViewModel` to avoid repeated DB queries per message
- Timeline navigation wired from HomeScreen "View All" action

### Fixed
- Blocking OkHttp calls moved to `Dispatchers.IO` in NVIDIA and Anthropic AI providers
- `archiveObject` race condition fixed: callback passed through ViewModel instead of fire-and-navigate
- `CreateObjectUseCaseTest` missing `archived`/`deleted` fields in `LifeObject` constructor
- Hardcoded Canvas hex colors replaced with `MaterialTheme.colorScheme.primary`
- Settings Export/Import cards now use `Upload`/`Download` icons
- AI metadata extraction JSON parsing improved with `kotlinx.serialization` primary + regex fallback
- `DocumentOcrWorker` redundant null check removed for non-nullable `documentId`
- `ReminderEvaluationWorker` and `LifeStateEngineImpl` now inject `RuleEngine` interface

---

## [0.7.0-alpha] — 2026-06-26

### Added
- Object-to-object relationship linking via `LinkObjectSheet`
- Camera capture in document upload flow
- NVIDIA NIM AI provider (OpenAI-compatible API endpoint)
- Data export as JSON with full object/metadata/timeline payload and sharing
- Data import with manifest validation and integrity checking
- Biometric lock with Android BiometricManager
- AI API key encrypted with Android Keystore AES-256-GCM (ADR-004)
- Launcher icons with adaptive icon configuration
- Notification deep links navigate to object detail screen
- Domain icons consolidated into shared `DomainIcons` utility
- Search extended to index metadata field values and document names
- Timeline filtering by source type (USER_ACTION, AI_SUGGESTION, SYSTEM)
- Sort order control in LibraryScreen
- Attention items surfaced on HomeScreen notification badge
- Task completion from ObjectDetail tasks tab
- Delete object with confirmation dialog and secure file cleanup
- Archive/restore object with status transitions
- Schema validation: REGEX, DATE_FORMAT, ENUM rules

### Fixed
- ReminderRule model and schema format consistency
- MetadataFieldType.MULTILINE_TEXT added for text area fields
- Enum value consistency across domain model and schemas
- Notification channel creation and reminder evaluation
- SearchViewModel state management

---

## [0.6.0-alpha] — 2026-06-24

### Added
- MetadataVerification screen: user reviews and accepts/rejects AI-extracted fields
- `ExtractMetadataUseCase` with schema-driven field extraction
- `TaskGenerator` wired into `LifeStateEngine` `OBJECT_CREATED` pipeline
- OCR pipeline via ML Kit, `FileStorageManager`, `DocumentOcrWorker` (WorkManager)
- AI metadata extraction entry point from `DocumentViewerScreen`
- Document viewer with OCR text panel and file metadata section
- 6 domain-specific JSON schemas: passport, driving-licence, insurance, property, vehicle, will

### Fixed
- `LifeStateEngine` integrated into OCR pipeline and metadata verification
- `ExtractMetadataUseCase` type mismatches and AI context for reminders

---

## [0.5.0-alpha] — 2026-06-22

### Added
- Anthropic AI provider with real API integration
- Relationship engine: `RelationshipRepository`, `LinkObjectsUseCase`
- Metadata editing screen: field-by-field editor from schema definition
- Notification infrastructure: channels, `NotificationHelper`, `ReminderEvaluationWorker`
- AI Settings screen: provider selection, model, API key input
- 10+ new object schemas across Identity, Finance, Property, Health domains

---

## [0.4.0-alpha] — 2026-06-20

### Added
- Core engines: `LifeStateEngineImpl`, `RuleEngineImpl`, `SchemaEngineImpl`
- Object creation flow with domain/type selection sheet
- Document upload sheet with file picker and storage
- AI chat screen with conversational interface
- Search screen with universal search and recent searches
- Timeline screen with grouped entries
- Home dashboard with attention items, tasks, and recent activity
- Library screen with domain grouping and object cards
- Settings screen with profile management
- All 10 repository implementations
- Room database with 10 entities, 9 DAOs
- Hilt dependency injection across all modules
- Navigation: bottom nav, back stack, typed routes

---

## [0.3.0-alpha] — 2026-06-18

### Added
- Complete domain layer: 10 repository interfaces, 11 use cases, 8 domain models
- Schema definition DSL with `ObjectSchema`, `MetadataFieldDefinition`, `ValidationRule`
- `LifeStateEngine` and `RuleEngine` domain interfaces
- AI provider abstraction: `AiProvider`, `AiCompletionResult`, `AiMessage`

---

## [0.2.0-alpha] — 2026-06-16

### Added
- Design system: Material 3 theme, typography, colors, spacing
- Shared components: `ObjectCard`, `TimelineCard`, `TaskCard`, `SectionHeader`, `EmptyState`
- Multi-module Gradle project structure (app, core, domain, data, designsystem, 8 feature modules)
- Version catalog (`libs.versions.toml`) with all dependencies

---

## [0.1.0-alpha] — 2026-06-15

### Added
- Initial Android project foundation
- Product specification (MASTER-SPEC.md)
- Architecture documentation
- CLAUDE.md with engineering standards
