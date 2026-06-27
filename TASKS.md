# LifePilot — Task Tracker

This document tracks implementation tasks. Check off items as they are completed.

Last updated: 2026-06-27 (all UI tests ✅, LazyColumn layout fixes ✅, metadata keyboard types ✅)

---

# Milestone 1 — Project Foundation ✅

## Project Setup ✅
* [x] Create Android project
* [x] Configure Gradle (AGP 8.5.2, Kotlin 2.0.0)
* [x] Configure Version Catalog (libs.versions.toml)
* [x] Configure Hilt (2.51.1)
* [x] Configure Room (2.6.1)
* [x] Configure Kotlin Serialization
* [x] Configure WorkManager
* [x] Configure Coil (image loading)
* [x] Configure OkHttp (AI provider network calls)
* [x] Configure GitHub Actions CI

## Design System ✅
* [x] Material 3 theme
* [x] Typography
* [x] Colors
* [x] Spacing
* [x] Domain icons (DomainIcons utility)
* [x] ObjectCard component
* [x] TaskCard component
* [x] TimelineCard component
* [x] SectionHeader component
* [x] EmptyState component
* [x] LoadingComponents
* [x] Chips component
* [x] LifePilotBottomNavBar

## Navigation ✅
* [x] Typed navigation routes
* [x] Root NavHost (LifePilotNavHost)
* [x] Bottom navigation (5 tabs)
* [x] Feature navigation modules
* [x] Deep link navigation (notifications → object detail)

---

# Milestone 2 — Persistence ✅

## Database ✅
* [x] Room database (LifePilotDatabase)
* [x] ProfileEntity + ProfileDao
* [x] ObjectEntity + ObjectDao
* [x] DocumentEntity + DocumentVersionEntity + DocumentDao
* [x] MetadataEntity + MetadataDao
* [x] EventEntity + EventDao
* [x] TaskEntity + TaskDao
* [x] ReminderEntity + ReminderDao
* [x] TimelineEntity + TimelineDao
* [x] RelationshipEntity + RelationshipDao
* [x] Type converters (Instant, enums)
* [x] Database migration support

## File Storage ✅
* [x] FileStorageManager (copy, delete, SHA-256 checksum)
* [x] Object-scoped directory structure
* [x] Content URI support
* [x] Unique filename generation
* [x] Secure file deletion

---

# Milestone 3 — Domain Layer ✅

## Repository Interfaces ✅
* [x] ProfileRepository
* [x] ObjectRepository
* [x] DocumentRepository
* [x] MetadataRepository
* [x] EventRepository
* [x] TaskRepository
* [x] ReminderRepository
* [x] TimelineRepository
* [x] SearchRepository
* [x] RelationshipRepository

## Repository Implementations ✅
* [x] ProfileRepositoryImpl
* [x] ObjectRepositoryImpl
* [x] DocumentRepositoryImpl
* [x] MetadataRepositoryImpl
* [x] EventRepositoryImpl
* [x] TaskRepositoryImpl
* [x] ReminderRepositoryImpl
* [x] TimelineRepositoryImpl
* [x] SearchRepositoryImpl (with metadata + document indexing)
* [x] RelationshipRepositoryImpl

## Use Cases ✅
* [x] CreateObjectUseCase
* [x] DeleteObjectUseCase
* [x] ArchiveObjectUseCase
* [x] UpdateObjectStatusUseCase
* [x] GetObjectWithMetadataUseCase
* [x] UploadDocumentUseCase
* [x] CompleteTaskUseCase
* [x] LinkObjectsUseCase
* [x] ExtractMetadataUseCase (AI-powered)
* [x] GetDashboardDataUseCase
* [x] ExportDataUseCase
* [x] ImportDataUseCase

## Services ✅
* [x] LifeStateEngine (pipeline orchestration)
* [x] RuleEngine (reminder/task evaluation)
* [x] SchemaEngine (schema loading from assets)
* [x] TaskGenerator (template-based task creation with deduplication)
* [x] OcrService interface + MlKitOcrService

---

# Milestone 4 — Schema Engine ✅

* [x] ObjectSchema data model
* [x] MetadataFieldDefinition with fieldType, validation, enumValues
* [x] LifecycleDefinition with states and transitions
* [x] ReminderRule definitions in schemas
* [x] SearchConfig per schema
* [x] AiExtractionConfig per schema
* [x] SchemaEngineImpl loading JSON from assets
* [x] Schema validation (REGEX, DATE_FORMAT, ENUM rules)
* [x] 12+ domain schemas

### Available Schemas (17 total)
* [x] passport
* [x] driving_licence
* [x] insurance
* [x] property
* [x] vehicle
* [x] will
* [x] job (employment)
* [x] bank_account
* [x] health
* [x] tax
* [x] investment
* [x] education
* [x] loan
* [x] pension
* [x] subscription
* [x] travel
* [x] utilities

---

# Milestone 5 — AI Pipeline ✅

## OCR ✅
* [x] OcrService domain interface
* [x] MlKitOcrService implementation
* [x] DocumentOcrWorker (WorkManager background processing)
* [x] OCR results stored in DocumentVersionEntity

## AI Providers ✅
* [x] AiProvider domain interface
* [x] AiCompletionResult sealed class (Success, Error, Unavailable)
* [x] NvidiaAiProvider (OpenAI-compatible NVIDIA NIM)
* [x] AnthropicAiProvider
* [x] OfflineAiProvider (graceful degradation)
* [x] AiProviderFactory (runtime provider selection from settings)
* [x] Encrypted API key storage (Android Keystore AES-256-GCM)

## AI Features ✅
* [x] ExtractMetadataUseCase with JSON parsing (kotlinx.serialization + regex fallback)
* [x] AI chat with structured life data system prompt
* [x] System prompt caching per conversation
* [x] MetadataVerification screen (user accepts/rejects AI suggestions)
* [x] Field type preservation (DATE, NUMBER, ENUM) from extraction through storage

---

# Milestone 6 — Core Features ✅

## Home ✅
* [x] HomeViewModel (dashboard data, attention items)
* [x] HomeScreen (domain distribution chart, tasks, recent activity)
* [x] Attention items with notification badge
* [x] Navigation to Timeline from RECENT ACTIVITY section

## Library ✅
* [x] LibraryViewModel (objects by profile, domain grouping)
* [x] LibraryScreen (domain tabs, object cards, sort order)
* [x] Empty state for no objects

## Search ✅
* [x] SearchViewModel (universal search)
* [x] SearchScreen (search bar, results with domain icons, recent searches)
* [x] Cross-index search (objects, metadata values, document names)
* [x] Result deduplication with relevance boosting

## Timeline ✅
* [x] TimelineViewModel (grouped by date)
* [x] TimelineScreen (cards with type icons, source type filtering)
* [x] Empty state

## Object Detail ✅
* [x] ObjectDetailViewModel (object, metadata, documents, tasks, relationships)
* [x] ObjectDetailScreen (4 tabs: Info, Documents, Tasks, Related)
* [x] Archive/restore/delete actions
* [x] Task completion inline
* [x] Document upload from detail screen
* [x] Relationship linking from detail screen

## AI Chat ✅
* [x] AiChatViewModel (message sending, history, system prompt)
* [x] AiChatScreen (chat bubbles, loading indicator, clear conversation)
* [x] Structured life data context in system prompt
* [x] Graceful offline/unconfigured state

---

# Milestone 7 — Ingestion ✅

* [x] DocumentUploadSheet (file picker, camera capture)
* [x] DocumentUploadViewModel
* [x] OCR worker scheduled on upload
* [x] DocumentViewerScreen (image/PDF display, OCR text panel)
* [x] AI extraction entry point from DocumentViewerScreen
* [x] MetadataVerificationScreen (review/edit/accept AI suggestions)
* [x] Object creation flow integrated with LifeStateEngine

---

# Milestone 8 — Rule Engine ✅

* [x] RuleEngine interface
* [x] RuleEngineImpl (expiry and renewal reminder evaluation)
* [x] ReminderEvaluationWorker (periodic background evaluation)
* [x] TaskGenerator (OBJECT_CREATED task templates)
* [x] Task deduplication before insertion
* [x] Reminder persistence and observability

---

# Milestone 9 — Profiles ✅

* [x] Default profile auto-created on first launch
* [x] Profile rename/delete in Settings
* [x] PreferenceManager for active profile ID
* [x] AppInitializer ensures profile exists

---

# Milestone 10 — Backup ✅

* [x] ExportDataUseCase (JSON manifest + payload)
* [x] Export UI in SettingsScreen with Android share sheet
* [x] ImportDataUseCase (manifest validation, conflict detection)
* [x] Import UI in SettingsScreen with file picker

---

# Milestone 11 — Security ✅

* [x] Biometric lock with Android BiometricManager
* [x] BiometricLockScreen
* [x] EncryptedKeyStorage (Android Keystore AES-256-GCM)
* [x] Secure file deletion on object delete
* [x] API keys never committed to repository

---

# Milestone 12 — Polish ✅

* [x] Accessibility semantics on shared components
* [x] Domain icons in search results
* [x] Proper empty states throughout
* [x] Error states in all ViewModels
* [x] Loading states with CircularProgressIndicator
* [x] Animated transitions between screens
* [x] Haptic feedback on key actions (task completion, swipe-to-complete)
* [x] Swipe-to-complete on task cards
* [x] Pull-to-refresh on Home, Library, and Timeline
* [x] PDF rendering (PdfRenderer — renders all pages, 1080px wide)
* [x] Multi-select for bulk operations (archive/delete)

---

# Milestone 13 — Testing

## Unit Tests ✅
* [x] LifeStateEngineTest
* [x] RuleEngineTest
* [x] TaskGeneratorTest
* [x] SearchRepositoryImplTest
* [x] CreateObjectUseCaseTest
* [x] DeleteObjectUseCaseTest
* [x] ArchiveObjectUseCaseTest
* [x] UpdateObjectStatusUseCaseTest
* [x] LinkObjectsUseCaseTest
* [x] ExtractMetadataUseCaseTest
* [x] GetDashboardDataUseCaseTest
* [x] GetObjectWithMetadataUseCaseTest
* [x] ExportDataUseCaseTest
* [x] ImportDataUseCaseTest
* [x] UploadDocumentUseCaseTest

## Integration Tests (In Progress)
* [x] ObjectDao instrumented tests (8 test cases)
* [x] TaskDao instrumented tests (6 test cases)
* [x] MetadataDao instrumented tests (7 test cases)
* [x] ReminderDao instrumented tests (5 test cases)
* [x] FileStorageManager integration tests (7 test cases)
* [ ] PreferenceManager integration tests
* [ ] EncryptedKeyStorage integration tests

## UI Tests (In Progress)
* [x] HiltTestRunner configured
* [x] Navigation smoke tests (5 cases: home, library, search, AI, settings tabs)
* [x] Create object flow smoke test (2 cases: home FAB, library FAB)
* [x] Document upload flow (2 cases: sheet opens, schema types visible)
* [x] Search flow (4 cases: bar visible, empty state, text input, clear)

---

# Remaining Work for 1.0

| Priority | Item |
|----------|------|
| Medium | Integration tests |
| Low | UI tests |
| Low | Performance profiling |
| Low | Play Store listing assets |
