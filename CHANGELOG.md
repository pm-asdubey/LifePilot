# Changelog

All notable changes to LifePilot are documented here.

---

## [Unreleased] — 0.9.0-alpha

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
