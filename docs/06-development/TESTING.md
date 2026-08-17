# Testing Guide

This document describes the test strategy, how to run tests locally, what each test module covers, and critical invariants that must never be broken.

---

## Running Tests Locally

### All JVM unit tests (mirrors CI)

```bash
./gradlew test --no-daemon
```

This runs every module that has a `src/test/` source set. As of 2026-07 the JVM unit suite is green
across the module `src/test` sources, including `:designsystem` (added 2026-07-04 for
`ThinkingMessagesTest`). Notable additions this pass: `ActionPlanNormalizerTest` (`:domain`, deterministic
Project fallback), `ThinkingMessagesTest` (`:designsystem`, thinking-indicator rotation), and
`AiActionParserTest` stray-action-type-prefix + markdown-fence recovery cases (`:features:home`).

### Per-module (faster for targeted iteration)

```bash
./gradlew :domain:test
./gradlew :data:testDebugUnitTest
./gradlew :features:home:testDebugUnitTest
./gradlew :features:library:testDebugUnitTest
./gradlew :features:search:testDebugUnitTest
./gradlew :features:settings:testDebugUnitTest
./gradlew :features:timeline:testDebugUnitTest
```

> `:domain` is a plain Kotlin library, so its task is `:domain:test` and its results land in
> `domain/build/test-results/test/`. The Android library modules use `testDebugUnitTest` and write to
> `<module>/build/test-results/testDebugUnitTest/`.

### Android instrumented tests (requires a connected device or emulator)

```bash
./gradlew :data:connectedAndroidTest
```

This covers Room DAO tests and database migration tests that require a real SQLite engine.

---

## Test Modules (JVM unit)

| Module | Tests | Focus |
|--------|------:|-------|
| `:domain` | 64 | Pure business logic — 12 use cases, `ReminderRule`, `Project`, `SensitiveFieldRegistry`, `DomainEmoji`. No Android deps. |
| `:data` | 83 | Engines & parsers — `LifeStateEngine`, `RuleEngine`, `SchemaEngine` (+ canonical domains), `PromptBuilder`, `RetrievalEngineImpl`, `ActionPlanExecutorImpl`, `DomainLifeStateEngineImpl`, `SearchRepositoryImpl`, `TaskGenerator`, entity mappers. |
| `:features:home` | 32 | `AiActionParser` (incl. ACTION_PLAN / PROJECT_CREATION), `HomeViewModel` attachment flow. |
| `:features:library` | 24 | `LibraryViewModel` — sort/filter/selection + the **all-canonical-domains-visible** container guard. |
| `:features:settings` | 26 | `SettingsViewModel`. |
| `:features:timeline` | 9 | `TimelineViewModel`. |
| `:features:search` | 8 | `SearchViewModel`. |

Also covered via `*/src/androidTest/` (instrumented, device required): Room DAO round-trip tests with
an in-memory database (11 DAO/storage suites) and app-level smoke tests (navigation, create-object,
document upload, search, AI Q&A).

> **Coverage gaps (tracked for future work):** `HomeViewModel` core send/parse/execute path (only the
> attachment flow is unit-tested — it is a 1,500-line god object, see the Architectural Review),
> `PlannerViewModel`, `ProjectWorkspaceViewModel`, `ObjectDetailViewModel`,
> `CreateObjectViewModel`, and the background Workers (OCR, Morning Brief, Reminder).

---

## CI Gate

Every push and pull request runs `.github/workflows/ci.yml`.

The workflow:
1. Checks out the code on `ubuntu-latest`
2. Installs JDK 17
3. Runs `./gradlew test --no-daemon` (all JVM unit tests)
4. Fails the build if any test fails — merging is blocked
5. Uploads `build/reports/tests/` and `build/test-results/` as a downloadable artifact when tests fail
6. Posts a test count summary to the GitHub Actions job summary

A green CI check is required before any branch can be considered for review.

---

## Test Stack

| Tool | Purpose |
|------|---------|
| JUnit 4 | Test runner |
| Google Truth | Fluent assertions (`assertThat(x).isEqualTo(y)`) |
| MockK (`mockk<T>(relaxed = true)`) | Dependency mocking |
| `kotlinx-coroutines-test` + `runTest` | Suspend function testing |
| Room in-memory DB | DAO tests without a device |

---

## JVM Unit Test Gotcha

`org.json.JSONObject` is part of the Android SDK — it is **not** available in plain JVM unit tests.
On Android modules the stub throws at runtime, so a test that parses JSON will *silently* fail (the
`runCatching` around the parser swallows it and returns null) rather than error at compile time. If a
module tests JSON parsing logic, add this to its `build.gradle.kts`:

```kotlin
testImplementation("org.json:json:20240303")
```

`:features:home` and `:data` both already declare this — `AiActionParser`, `PromptBuilderImpl`, and
`DomainLifeStateEngineImpl` all parse `org.json`. This was learned the hard way: `DomainLifeStateEngineImplTest`
failed only at runtime (write never happened) until the dependency was added.

---

## Critical Invariants — Never Remove These Tests

The tests below guard against bugs that have already caused real regressions. They must not be deleted or weakened.

### `ConversationDaoTest.updateTitle_doesNotDeleteExistingMessages`

Guards against the CASCADE DELETE bug.

`ConversationDao.upsertConversation()` uses `OnConflictStrategy.REPLACE`. SQLite's INSERT OR REPLACE *deletes* the old row then inserts a new one. Because `chat_messages` has `onDelete = CASCADE` on the conversation FK, this silently deletes **all** messages for a conversation.

**Rule:** Never call `upsertConversation` after messages have been saved for a conversation. Use `touchConversation` or `updateTitle` (targeted UPDATE queries) instead. This was the root cause of the "first message always disappears" bug, confirmed by pulling the SQLite DB from the device and inspecting it with `sqlite3`.

### `ConversationDaoTest.upsertConversation_afterMessagesExist_deletesAllMessages`

Documents that `upsertConversation` IS destructive by design. If this test ever starts failing it means the FK strategy changed — that must be a deliberate, reviewed decision, not an accident.

### `AiActionParserTest.parse plain string OBJECT_CREATION returns null`

Guards against AI returning bare strings instead of JSON in `[LIFEPILOT_ACTION]` blocks.

`AiActionParser.parseAction()` wraps parsing in a try/catch and returns `null` on failure. If the prompt format changes, this test must still pass — it guards against the crash that occurred when the AI returned a bare type string instead of a JSON object.

---

## Known Dangerous Patterns (Summary)

### Room CASCADE DELETE via INSERT OR REPLACE

See the `ConversationDaoTest` invariants above. The full analysis is in `CLAUDE.md` under "Known Dangerous Patterns".

### AI Action Block Parsing — stripMarkdown Must Run AFTER Extraction

`HomeViewModel.parseAiResponse()` must call `ACTION_PATTERN.find(raw)` on the **unmodified** raw string, remove the action block from `content`, and only then call `stripMarkdown()` on the remaining visible text.

The `[LIFEPILOT_ACTION]` tag contains underscores. `stripMarkdown()` applies `_(.+?)_` (non-dotall), which can match across both underscores in a compact single-line response, corrupting `[LIFEPILOT_ACTION]` to `[LIFEPILOTACTION]` so `ACTION_PATTERN` never matches and raw JSON appears in the chat.

Do not revert the order of operations in `parseAiResponse()`.

### NavDestination Route Matching with Query Parameters

Routes registered as `"planner?taskId={taskId}"` do **not** match `"planner"` with `==`. Always use `substringBefore('?')` when checking `NavDestination.hierarchy` to strip query parameters before comparison.
