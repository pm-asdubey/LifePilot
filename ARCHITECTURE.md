# LifePilot Architecture

This document describes the technical architecture of LifePilot.

It is intended for engineers who need to understand, extend, or maintain the codebase.

---

## Overview

LifePilot is an offline-first Android application built on Clean Architecture principles.

The application maintains structured information about a person's administrative life. AI is an interface for retrieving and reasoning over that structured data — not a source of truth.

---

## Module Structure

```
LifePilot/
├── app/                    # Application entry point, navigation, DI wiring
├── core/
│   └── common/             # Shared utilities (Instant converters, extensions)
├── domain/                 # Pure Kotlin business logic — no Android framework dependencies
├── data/                   # Android-specific implementations (Room, DataStore, WorkManager)
├── designsystem/           # Shared Compose components and Material 3 theme
└── features/
    ├── home/               # Dashboard screen
    ├── library/            # Object list and management
    ├── search/             # Universal search
    ├── timeline/           # Activity timeline
    ├── object/             # Object detail, creation, metadata editing, verification
    ├── document/           # Document viewer
    ├── ai/                 # AI chat interface
    └── settings/           # Settings, profile, export/import
```

### Module Dependencies

```
app
├── domain
├── data
├── designsystem
└── features/*
    ├── domain
    └── designsystem

data
├── domain
└── core:common

domain
└── (pure Kotlin — no dependencies on other modules)
```

---

## Dependency Direction

```
UI (features)
    ↓
ViewModel
    ↓
Use Case (domain)
    ↓
Repository Interface (domain)
    ↓
Repository Implementation (data)
    ↓
DAO / DataStore / FileStorage (data)
    ↓
Room / File System
```

This direction is strictly enforced. Higher layers depend on lower layers through interfaces. Lower layers never depend on higher layers.

---

## Core Engines

### Life State Engine

The Life State Engine is the central pipeline that processes every significant change to application state.

**Location:** `domain/engine/LifeStateEngine.kt` (interface), `data/engine/LifeStateEngineImpl.kt`

**Pipeline:**

```
Document Upload
    ↓ FileStorageManager copies file
    ↓ DocumentEntity created in Room
OCR Processing (background, WorkManager)
    ↓ MlKitOcrService extracts text
    ↓ DocumentVersionEntity updated with ocrText
AI Metadata Extraction (user-initiated)
    ↓ ExtractMetadataUseCase calls AI provider
    ↓ User verifies suggestions in MetadataVerificationScreen
Metadata Persisted
    ↓ MetadataRepository.upsertMetadataBatch
Object State Updated
    ↓ UpdateObjectStatusUseCase
Timeline Entry Created
    ↓ TimelineRepository.insertEntry
Task Generation
    ↓ TaskGenerator evaluates schema task templates
    ↓ Deduplication before insert
Reminder Evaluation
    ↓ RuleEngine evaluates reminderRules from schema
    ↓ ReminderEntity created for triggered rules
```

All major user actions (create object, upload document, accept metadata) flow through this pipeline. Nothing bypasses it.

### Rule Engine

Evaluates reminder rules defined in schema JSON files.

**Location:** `domain/engine/RuleEngine.kt`, `data/engine/RuleEngineImpl.kt`

**Trigger:** `ReminderEvaluationWorker` runs every 6 hours via WorkManager.

**Rule evaluation:**
1. Load object metadata for each object
2. For each `reminderRule` in the schema, check if `triggerField` is set
3. If `triggerDate + offsetDays` is within the trigger window, create a `ReminderEntity`
4. Skip if a non-dismissed reminder for the same `ruleId` already exists (deduplication)
5. Trigger notification if `triggerDate` has passed

### Schema Engine

Loads object schemas from JSON assets at startup.

**Location:** `domain/engine/SchemaEngine.kt`, `data/schema/SchemaEngineImpl.kt`

**Schema format:** `data/src/main/assets/schemas/*.json`

Schemas define:
- Object type identifier and display name
- Metadata field definitions (fieldId, fieldType, validation rules, AI extractability)
- Lifecycle states and transitions
- Reminder rules with trigger fields and offsets
- Search configuration (primary, full-text, filterable fields)
- AI extraction hints

17 schemas are bundled: `passport`, `driving_licence`, `insurance`, `property`, `vehicle`, `will`, `job`, `bank_account`, `health`, `tax`, `investment`, `education`, `loan`, `pension`, `subscription`, `travel`, `utilities`.

---

## Database

### Technology

Room 2.6.1 with SQLite on-device storage.

### Entities

| Entity | Table | Description |
|--------|-------|-------------|
| `ProfileEntity` | `profiles` | User profiles (multi-profile support) |
| `ObjectEntity` | `objects` | Core life objects (passport, job, etc.) |
| `MetadataEntity` | `metadata` | Key-value metadata for objects, schema-driven |
| `DocumentEntity` | `documents` | Documents attached to objects |
| `DocumentVersionEntity` | `document_versions` | File versions with OCR text |
| `EventEntity` | `events` | Domain events on objects |
| `TaskEntity` | `tasks` | Generated tasks with status |
| `ReminderEntity` | `reminders` | Rule-evaluated reminder records |
| `RelationshipEntity` | `relationships` | Bidirectional object links |
| `TimelineEntity` | `timeline` | Immutable activity log (100 entries per profile) |

### Key Design Decisions

- **Soft delete:** Objects are marked `deleted = 1` rather than physically removed. File cleanup is handled separately.
- **No schema versions beyond 1.0:** Version 1 is the first release. Future schema changes require Room migrations with the exported schema JSON as reference.
- **`exportSchema = true`:** Schema JSON is exported to `data/schemas/` and should be committed to track schema history.

---

## File Storage

**Location:** `data/storage/FileStorageManager.kt`

Documents are stored in the app's private directory:
```
files/objects/{objectId}/{sanitized_filename}
```

- Files are copied from content URIs at upload time (user retains original)
- Filenames are sanitized and deduplicated with numeric suffixes
- SHA-256 checksums are computed and stored in `DocumentVersionEntity`
- All files for an object are deleted when the object is hard-deleted

---

## AI Architecture

### Providers

All AI providers implement `AiProvider`:

| Provider | Class | Description |
|----------|-------|-------------|
| NVIDIA NIM | `NvidiaAiProvider` | OpenAI-compatible API, production default |
| Anthropic | `AnthropicAiProvider` | Anthropic Claude API |
| Offline | `OfflineAiProvider` | Graceful degradation when no key configured |

Provider selection is driven by `PreferenceManager.getAiProvider()`. The selected provider is resolved at runtime via `AiProviderFactory`.

### API Key Security

API keys are stored using Android Keystore AES-256-GCM encryption via `EncryptedKeyStorage`. Keys are never committed to the repository and are entered at runtime in Settings > AI Provider.

### Context Building

When a user sends a message, the AI system prompt is built from:
- Active profile name
- Up to 30 most-recently-updated objects (title, type, domain, status)
- Up to 5 metadata fields per object (batch-fetched in a single query)
- Up to 10 pending tasks
- Up to 10 upcoming reminders (next 30 days)

The system prompt is cached per conversation session. Clearing the chat resets the cache.

**Design constraint:** AI never persists data. It suggests; the user verifies; the system stores.

---

## Search Architecture

Search uses database queries, not a search index.

**Location:** `data/repository/SearchRepositoryImpl.kt`

**Query strategy:**
1. Object title/description/type: `LIKE` query on `objects` table (LIMIT 50)
2. Metadata values: `LIKE` query on `metadata` table joined with `objects`
3. Document names/types: `LIKE` query on `documents` + `document_versions` tables
4. Results from steps 2 and 3 are batch-fetched in single queries using `IN` clauses
5. Object results boosted if they also match metadata
6. Results sorted by relevance score (exact → prefix → contains → metadata → document)

Recent searches are stored in-memory (not persisted — intentional for V1).

---

## Navigation

Navigation uses Jetpack Navigation Compose with typed routes.

**Location:** `app/navigation/LifePilotNavHost.kt`

**Screen transitions:** All navigations use `slideInHorizontally + fadeIn` / `slideOutHorizontally + fadeOut`.

**Deep links:** The `lifepilot://` custom scheme is used for notification deep links (e.g., `lifepilot://object/{objectId}`).

**Bottom navigation:** 5 tabs — Home, Library, Search, Ask AI, Profile (Settings).

---

## Dependency Injection

Hilt is used throughout with `SingletonComponent` for all repositories, engines, and databases.

**Key modules:**
- `DatabaseModule` — Room database and all DAOs
- `RepositoryModule` — binds interfaces to implementations
- `AiModule` — AI provider factory
- `DataModule` — FileStorageManager, SchemaEngine, etc.

Workers use `@HiltWorker` and `@AssistedInject`.

---

## Background Processing

WorkManager is used for all background tasks.

| Worker | Schedule | Purpose |
|--------|----------|---------|
| `DocumentOcrWorker` | One-shot, on document upload | ML Kit OCR processing |
| `ReminderEvaluationWorker` | Periodic, every 6 hours | Evaluate reminder rules |

WorkManager is initialized manually (not via Jetpack Startup — disabled in manifest) to allow Hilt injection via `HiltWorkerFactory`.

---

## Security

| Concern | Approach |
|---------|----------|
| API keys | Android Keystore AES-256-GCM via `EncryptedKeyStorage` |
| Biometric lock | `BiometricManager` + `BiometricPrompt`, evaluated on each resume |
| File access | App-private directory; `FileProvider` for sharing |
| Logging | Timber with no personal data in log messages |
| Network | HTTPS only for all AI API calls |
| Backup | `android:allowBackup="false"` — user controls data via export |

---

## Extension Points

The architecture is designed to accommodate future features without major refactoring:

| Future Feature | Extension Point |
|----------------|----------------|
| Cloud sync | Add `SyncRepository` to domain; implement in data layer |
| New AI provider | Implement `AiProvider`; add to `AiProviderFactory` |
| On-device AI | Implement `AiProvider` backed by on-device model |
| New domain pack | Add schema JSON to `assets/schemas/` |
| iOS / multiplatform | Domain layer is pure Kotlin — no Android dependencies |
| Multiple devices | `LifePilotDatabase` is version 1; migrations are supported |
| New object type | Add schema JSON — no code changes required |

---

## Key Design Decisions

### Why offline-first?

Personal administrative data (passports, insurance, property) must be accessible without connectivity. AI features require connectivity but are optional enhancements.

### Why objects over folders?

Traditional document management organizes by folder. LifePilot organizes by life entity — an object (e.g., Passport) has documents, metadata, events, tasks, and reminders. This models real administrative life more naturally.

### Why AI retrieves rather than owns?

If AI were the source of truth, users could not trust or verify what the system "knows." By having AI retrieve from structured storage and users verify AI suggestions before they become canonical, the system remains trustworthy even when AI makes mistakes.

### Why configuration-driven schemas?

New document types can be added without code changes. The schema JSON defines everything — fields, lifecycle, reminders, search config, AI extraction hints. This enables future "domain packs" as downloadable configuration.

### Why Room over other persistence?

Room provides type-safe SQLite access, schema export for migration tracking, Flow integration for reactive UI, and comprehensive instrumented testing support. The offline-first nature of the app makes a local relational database the right choice.

---

## Architecture Decision Records (ADRs)

ADRs are located in `docs/adr/`. Key decisions include:

- ADR-001: Offline-first architecture
- ADR-002: Room over other persistence options
- ADR-003: Schema-driven configuration
- ADR-004: AI as retrieval interface, not source of truth
- ADR-005: WorkManager for background processing
- ADR-006: Android Keystore for API key encryption
