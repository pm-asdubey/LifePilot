> [!NOTE]
> **Notice for Claude / AI Assistants**: This is an architectural assessment document only. Do NOT take any action, migrate, or modify any files based on this document unless explicitly instructed by the user.

# Kotlin Multiplatform (KMP) Migration Architectural Assessment
**Project**: LifePilot
**Role**: KMP Migration Architect
**Date**: 2026-07-01

## 1. Executive Summary

Based on a thorough architectural review of the LifePilot codebase, the project is an **excellent candidate** for Kotlin Multiplatform (KMP). The system's strict adherence to Clean Architecture isolates the core business logic (Domain Layer, AI Pipeline, Planning Engine) from platform-specific dependencies, making the core highly portable once key JVM dependencies (like `java.time`) are migrated.

* **Overall Recommendation**: **Proceed** (with a phased approach, gated on v1.0 stability).
* **Confidence Level**: **High** for the Domain and Core Logic; **Medium** for the Data Layer (due to persistence and OCR).
* **Estimated Engineering Effort**: 5-7 months for a full domain and data layer migration, accounting for extensive `java.time` refactoring across the domain and the persistence strategy chosen (SQLDelight).
* **Major Risks**: 
  1. Migrating local SQLite data without loss.
  2. Discrepancies between Android ML Kit OCR and iOS Vision framework.
  3. Abstracting Android's `WorkManager` for background tasks to work reliably within iOS background constraints.

---

## 2. Current Architecture Assessment

An analysis of the existing modules and their readiness for KMP:

| Module | Classification | Rationale |
| :--- | :--- | :--- |
| **`domain`** | **Requires refactoring first (JVM-only)** | While it has zero *Android* framework dependencies, it is heavily dependent on JVM APIs (`java.time.Instant` and `java.time.LocalDate`) across 26+ files (models and use cases). These must be migrated to `kotlinx-datetime` before it is KMP-ready. |
| **`core:common`** | **Build config refactor only** | Contains exactly one file (`Result.kt`) which has zero platform-specific imports and is already KMP-compatible. The only work needed is swapping the build plugin and replacing `timber`/`kotlinx.coroutines.android` dependencies. |
| **`data`** | **Requires refactoring first** | Heavily tied to Android (Room database, WorkManager, ML Kit, EncryptedSharedPreferences). Must be abstracted using `expect/actual` or interface implementations, and persistence must move to KMP. |
| **`features:*`** | **Android-specific** | Currently built with Jetpack Compose. Can remain platform-specific if native iOS UI is desired, or migrated to Compose Multiplatform (CMP) in a later phase. |
| **`designsystem`** | **Android-specific** | Built on Android's Material 3 Compose implementation. Requires CMP migration if sharing UI. |
| **`app`** | **Should remain platform-specific** | Handles Android dependency injection (Hilt) and entry point (Activity). A separate `iosApp` will be needed. |

---

## 3. Shared Code Analysis

By migrating to KMP, we can drastically reduce business logic duplication.

### Code to Move to Shared KMP Module (`commonMain`):
* **Domain Models**: All JSON schemas, `ObjectEntity`, `Goal`, `Task`, `TimelineEntity`, etc. (Post `java.time` migration).
* **Business Logic**: `LifeStateEngine`, `SchemaEngine`, `RuleEngine`.
* **Use Cases**: All `UseCase` classes coordinating data flow.
* **Repositories (Interfaces)**: All repository contracts (e.g., `MetadataRepository`).
* **AI Pipeline**: Prompt builders, object reasoners, response parsers, and the AI state machine.
* **Planning & Retrieval Engines**: Search ranking logic and task generation algorithms.
* **Utilities**: Validation logic, string formatting.
* **Validation & Serialization**: Schema validation rules, powered by `kotlinx.serialization`.
* **Networking**: API clients for Anthropic/NVIDIA NIM, powered by `Ktor`.

**Estimated Shared Codebase Percentage**: **~65-75%**. The remaining code will be UI (`features`, `designsystem`) and platform-specific drivers (Database drivers, OCR actuals).

---

## 4. Dependency Audit

To achieve KMP compatibility, Android/Java-specific dependencies must be replaced with multiplatform equivalents:

* **Date/Time**: `java.time.Instant` / `java.time.LocalDate` ➔ **`kotlinx-datetime`**. (Highest priority for the `domain` module).
* **Persistence**: `androidx.room` ➔ **SQLDelight**. 
* **Serialization**: `Gson` / `Moshi` ➔ **`kotlinx.serialization`**.
* **AI Providers JSON**: `org.json.JSONObject` / `JSONArray` ➔ **`kotlinx.serialization`**. (Currently used directly in `NvidiaAiProvider` and `AnthropicAiProvider`).
* **Networking**: `Retrofit` / `OkHttp` ➔ **`Ktor Client`**. 
  * *Note: AI provider migration is a two-part change: OkHttp ➔ Ktor Client AND `org.json.*` ➔ `kotlinx.serialization` models.*
* **Concurrency**: Pure `kotlinx.coroutines.core` (swap out `.android` where applicable).
* **Dependency Injection**: `Hilt` / `Dagger` ➔ **`Koin`** or manual DI for the shared module.

---

## 5. Database & Persistence Strategy

The Data layer is the most complex part of this migration because LifePilot is an offline-first app relying heavily on Room.

* **Strategy**: Migrate to **SQLDelight**. While Room KMP exists, it requires Room version 2.7.0+. The project currently uses Room 2.6.1. Upgrading Room carries its own migration risks, making SQLDelight the more pragmatic and historically stable choice for KMP persistence.
* **Migration Path**: 
  1. Port all Room `@Entity` classes and `@Dao` queries to `.sq` files.
  2. Implement a SQLDelight custom dialect or adapter for JSON fields.
  3. Ensure SQLite migrations (`MIGRATION_1_2`, `MIGRATION_2_3`) are carefully ported to `.sqm` files so existing Android users do not lose their Life State data during the app update.
* **KeyStore**: Replace `EncryptedSharedPreferences` with a KMP settings library (e.g., `Multiplatform Settings`) utilizing `expect/actual` for Android KeyStore and iOS Keychain.

---

## 6. AI Pipeline Assessment

The AI pipeline (`Retrieval ➔ Object Reasoner ➔ Prompt Builder ➔ AI Provider ➔ Response Parser`) is perfectly suited for KMP.
* All string manipulation, JSON extraction, and heuristic scoring can be moved directly to `commonMain`.
* The `AI Provider` network calls must be rewritten using `Ktor`, and the Android-bundled `org.json.*` parsing replaced with `kotlinx.serialization`.
* The `AiProposal` variants and state management will be shared, allowing iOS and Android to render the exact same logical AI suggestions natively.

---

## 7. Build System Changes

The Gradle build system will require an overhaul:
1. Replace `kotlin-android` and `kotlin-jvm` plugins in `domain`, `core:common`, and `data` with `org.jetbrains.kotlin.multiplatform`.
2. Introduce KMP source sets: `commonMain`, `androidMain`, `iosMain`.
3. Consolidate dependency management (e.g., using Version Catalogs) to handle KMP library variants.
4. Setup `XCFramework` exporting for iOS consumption if not using Compose Multiplatform.

---

## 8. Phased Roadmap

A "big bang" rewrite is highly risky. I recommend a 4-phase rollout:

* **Phase 1: Foundation (Weeks 1-4)**
  * Add `kotlinx-datetime` to the `domain` module.
  * Replace every `java.time.Instant` and `java.time.LocalDate` across all models and use cases (26+ files).
  * Only after this is complete, swap the build plugin to `kotlin.multiplatform`.
  * Refactor `core:common` build config (`timber` ➔ KMP logger, `.android` coroutines ➔ `.core`).
* **Phase 2: The Core Brain (Weeks 5-7)**
  * Move the `domain` module, AI Pipeline, and Engines to `commonMain`.
  * Validate with JVM/Android unit tests to ensure zero regression.
* **Phase 3: The Persistence Layer (Weeks 8-13)**
  * Abstract ML Kit OCR and WorkManager using `expect/actual`.
  * Migrate Room to SQLDelight.
  * Rigorously test Android database migrations.
* **Phase 4: iOS Integration (Weeks 14+)**
  * Build the `iosApp` Xcode project.
  * Implement the SQLDelight `NativeSqliteDriver` for iOS, Apple Vision framework for OCR, and `BGTaskScheduler` for background tasks.
  * Build the iOS UI (SwiftUI or Compose Multiplatform).

---

## 9. Risk Assessment

| Risk | Impact | Mitigation Strategy |
| :--- | :--- | :--- |
| **Database Migration Data Loss** | Critical | Extensive integration testing for Room-to-SQLDelight migration paths on local devices before release. |
| **OCR Quality Parity** | High | Abstract OCR behind an interface (`DocumentScanner`). Use ML Kit on Android and Apple Vision on iOS. Tuning prompts may be required if Vision's output differs from ML Kit's. |
| **Background Processing** | Medium | WorkManager has no KMP equivalent. Use an `expect/actual` interface (`BackgroundJobScheduler`) and implement `BGTaskScheduler` on iOS, accepting that iOS background execution is less reliable. |
| **MockK Test Failures** | Low | The `domain` tests heavily use `MockK`, which is JVM-centric. Tests may need to be migrated to `Mockative` or manual fakes to run on iOS targets. |

---

## 10. Final Recommendation

**Begin Phase 1 after v1.0 GA is shipped and the v1.0.x P0 bug backlog is cleared.**

The project is currently in v1.0.x stability mode with 6 open P0 critical bugs (search crash, conversation ID bug, document re-upload duplication, security gaps, cancellation status bug, and a pending security audit). Starting a KMP migration while P0 issues are open would destabilize the release branch and violate the project's Definition of Done.

Once the backlog is clear, start with Phase 1. Because LifePilot already uses Clean Architecture, moving the `domain` and utility logic to KMP is a low-risk structural improvement that strictly enforces architectural boundaries, even if an iOS app is not released immediately. 

For the Data layer (Phase 3), allocate dedicated QA time for the SQLite transition. Do not adopt Compose Multiplatform for the UI yet; focus on sharing the "Brain" (Engines, AI Pipeline, Validation) first, and build the UI natively on iOS to ensure the premium, offline-first experience is maintained.
