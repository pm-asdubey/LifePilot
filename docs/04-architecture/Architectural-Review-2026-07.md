# Architectural Review — July 2026

**Date:** 2026-07-03
**Baseline reviewed:** branch `feature/session-5-chat-context-cascade-confirmation` (commit `19fc0fc`)
**Scope:** Whole-codebase adherence to the Clean Architecture + Schema-Engine principles in `CLAUDE.md`, with emphasis on the AI conversation → proposal → write workflow.

## Remediation progress — updated 2026-07-03

Worked through the backlog after the review. Status of each item:

| # | Item | Status |
|---|------|--------|
| 1 | Unify AI action parser (tested copy was dead) | ✅ Done — `HomeViewModel` delegates to `AiActionParser`; ~360 dup lines deleted |
| — | Delete orphaned `features:ai` module | ✅ Done — module + `settings.gradle` include removed |
| 2 | AI writes bypass the Life State Engine | ✅ Done — `HomeViewModel.executeProposal(ObjectCreation)` + `ActionPlanExecutorImpl(CreateRecord)` now call `lifeStateEngine.processObjectEvent` (tasks + reminders fire) |
| 6 | Duplicated APPEND-merge logic | ✅ Done — extracted `MetadataMerge` (domain) + test; both call sites unified |
| 7 | `PlanningEngineImpl` goal-progress no-op | ✅ Done — implemented from task completion (`getTasksByGoal`) + 3 tests |
| 4 | Business rules inside Composables | ✅ Partial — `ProjectHealth` + `ExpiryStatus` lifted to tested domain rules; full `ObjectDetail` per-type summary extraction remains |
| 5 | `PreferenceManager` (data.*) injected into ViewModels | ⏳ Open — mechanical, low-priority (a purity concern, not a bug) |
| 3 | `HomeViewModel` god object — extract collaborators | ⏳ Open — large; `AiActionParser` already extracted; `AiRetryClient`/`AttachmentIngestor`/`ProposalExecutor` remain |
| 8 | Hardcoded prompts → Schema `aiConfig` | ⏳ Open — large, additive |

The JVM unit suite grew from 246 → **267 tests** across this pass, all green.

## How to read this

This document persists a **completed** architectural review so future engineers (and future AI sessions) do not have to re-derive it. It is a snapshot, not a live tracker.

- Every finding below was verified by opening the cited file at the cited line. Line numbers are approximate and will drift as the code changes — treat them as signposts, not contracts.
- The **Status** column records what has already been remediated as of the baseline commit. `Remediated ✓` means the fix has landed; `Partial` means mitigated but not fully resolved; `Open` means untouched.
- Everything in the remediation backlog is designed to be **non-breaking** (additive or behind an unchanged public API). Order reflects leverage, not effort.

---

## Adherence Summary

**The foundations are strong.**

- The module graph matches Clean Architecture: `domain` has no Android/data dependencies, `data` implements domain interfaces, and `features/*` depend downward only.
- The **Schema Engine is genuinely config-driven**: 32 JSON schemas under `data/src/main/assets/schemas/` drive object types, metadata fields, forms, validation, lifecycle, and reminder rules. New object types are added as data, not code.
- Repository **interfaces expose only domain models** — no Room entities or DAOs leak across the boundary (with one exception noted below: `DocumentRepository.uploadDocument`).
- **Compose screens are free of Room/DAO/OCR imports** — persistence and ML concerns stay out of the UI layer.

**Adherence breaks down along the AI workflow axis.** The conversational-AI path grew faster than the discipline around it, producing three recurring smells: business logic embedded in `HomeViewModel`, **engine bypasses** (AI writes that skip the Life State Engine), and **duplicated parsers** (the same JSON→proposal logic exists in more than one place). These are the subject of the risks and backlog below.

---

## Top Risks

| # | Risk | Evidence | Status |
|---|------|----------|--------|
| 1 | **Production uses the untested parser copy.** The tested `AiActionParser` (`@Singleton @Inject`) is never called in production; `HomeViewModel` carries its own inline `parseAction()`. The "critical invariant" test guards the wrong copy. | `features/home/.../viewmodel/AiActionParser.kt` (443 lines; `@Singleton` L32, `@Inject constructor` L33, `parseAction()` L56) is covered by `AiActionParserTest.kt` (29 tests). `HomeViewModel` does **not** inject it and defines a private `parseAction()` at L1084 (with `parseActionPlan()` L1192, `parseActionItem()` L1244). | **Partial** — not yet unified, but both copies were kept in sync for the recent emoji/`targetDate` change. Unification is the #1 tracked refactor. |
| 2 | **AI creation/update paths bypass the Life State Engine.** AI-created records get no timeline entry, no task generation, and no reminder evaluation, because the write goes straight to the repositories instead of through the engine. | `HomeViewModel.executeProposal()` (L804) calls `objectRepository.createObject(...)` and `metadataRepository.upsertMetadata(...)` directly. `ActionPlanExecutorImpl` (211 lines) does the same — `createProject` L96, `upsertMetadata` L116, `createObject` L127 — without firing `LifeStateEngine.processObjectEvent`. The manual path (`CreateObjectUseCase`) is wired correctly. | **Open** |
| 3 | **`HomeViewModel` is a god object.** ~1,569 lines and **21 constructor dependencies** (L77–L97). It owns conversation state, a retry/backoff HTTP loop, file IO + OCR + AI orchestration, the JSON parse pipeline, proposal execution, and greeting heuristics. | `HomeViewModel.kt` imports `data.ai.AiProviderFactory` (L8), `data.storage.FileStorageManager` (L9), `domain.ocr.OcrService` (L23); constructor spans L76–L97. | **Open** |
| 4 | **Orphaned third parser copy (`features:ai`).** The `AiChatViewModel`/`AiChatScreen` module contained a **third divergent copy** of the parse logic and was dead code — never wired into the app. | Module and its `include(":features:ai")` line are gone: `features/ai/` no longer exists and `settings.gradle.kts` no longer references it. | **Remediated ✓** — deleted this session (module + `settings.gradle.kts` include removed). |
| 5 | **AI prompt config is hardcoded, not schema-driven.** `CLAUDE.md` lists "AI Prompt Configuration" among the things that belong in the Schema Engine, but prompts live in code. | `PromptBuilderImpl.kt` (426 lines) plus inline prompt strings in `DomainLifeStateEngineImpl.kt` (172 lines) and `HomeViewModel`. `CLAUDE.md` L477 ("AI Prompt Configuration") under "These belong in the Schema Engine." | **Open** |

---

## Complexity Hotspots

| Area | File / size | Problem |
|------|-------------|---------|
| Conversation orchestration | `HomeViewModel.kt` (~1,569 lines) | Four separable collaborators tangled into one class — see Risk 3 and the backlog. |
| Object detail UI | `features/object/.../ui/ObjectDetailScreen.kt` (~831 lines) | Business rules inside the Composable, e.g. visa 60-day-expiry math computed in the UI. |
| Planner UI | `features/planner/.../ui/PlannerScreen.kt` (~840 lines) | Project-progress calculation and the "Behind" status rule live inside the Composable. |
| Duplicated merge logic | `HomeViewModel` + `ActionPlanExecutorImpl` | The APPEND-merge logic for metadata is duplicated across both (it also lived in the now-deleted `AiChatViewModel`). |

---

## Layer-Boundary Observations

- **`HomeViewModel` reaches into the data layer.** It imports `data.ai.AiProviderFactory`, `data.storage.FileStorageManager`, and `domain.ocr.OcrService`, and drives the upload → OCR → classify pipeline inline — duplicating `UploadDocumentUseCase`.
- **Business rules inside Composables.** `ObjectDetailScreen` (visa expiry) and `PlannerScreen` (project progress + "Behind" rule) compute domain logic in the UI layer.
- **ViewModels skip the Use Case layer.** Roughly 8 of the ~13 feature ViewModels call repositories/engines directly rather than going through a use case, weakening the mandated `UI → ViewModel → UseCase → Repository` direction.
- **A `data.*` class is injected into ViewModels.** `PreferenceManager` (a `data.repository` class) is injected directly into 6 ViewModels (Settings, Home, Planner, ProjectWorkspace, ObjectDetail, MetadataVerification). Extract a domain-level `Preferences` interface and inject that instead.
- **A raw path leaks across a repository boundary.** `DocumentRepository.uploadDocument` exposes a raw `filePath` rather than a domain-modeled reference.

---

## Prioritized Remediation Backlog

All items are non-breaking (additive, or behind an unchanged public API). Ordered by leverage.

1. **Wire `AiActionParser` into `HomeViewModel` and delete the inline copy.** Inject the tested `@Singleton` parser and delegate `parseAction()` to it. Resolves Risk 1 and makes the critical-invariant test guard the real code path. *(Quick win.)*
2. **Route AI writes through `CreateObjectUseCase`.** Have `executeProposal()` and `ActionPlanExecutorImpl` create objects via the same use case the manual path uses, so `LifeStateEngine.processObjectEvent` fires (timeline + task generation + reminder eval). Resolves Risk 2.
3. **Extract an `AiRetryClient`** from `HomeViewModel` — the retry/backoff HTTP loop behind a small interface. *(Quick win; step toward Risk 3.)*
4. **Extract shared APPEND-merge logic** into a single helper used by both `HomeViewModel` and `ActionPlanExecutorImpl`. *(Quick win.)*
5. **Extract `AttachmentIngestor` and `ProposalExecutor`** from `HomeViewModel`, folding the inline upload→OCR→classify flow onto `UploadDocumentUseCase`. Together with items 1 and 3 this shrinks `HomeViewModel` to roughly 600 lines / ~13 deps. Completes Risk 3.
6. **Implement or delete `PlanningEngineImpl` no-ops.** `generateTasksForGoal()` (L195) is a logged no-op and `recalculateGoalProgress()` (L201) is unused — either give them behavior or remove them. *(Quick win.)*
7. **Make AI prompt config schema-driven (additive).** Read prompt overrides from schema `aiConfig`, falling back to the current hardcoded strings in `PromptBuilderImpl` / `DomainLifeStateEngineImpl`. Resolves Risk 5 without a breaking rewrite.
8. **Lift business rules out of Composables.** Move visa-expiry math (`ObjectDetailScreen`) and project-progress/"Behind" logic (`PlannerScreen`) into the domain/use-case layer.
9. **Introduce a domain `Preferences` interface** and stop injecting `data.PreferenceManager` into ViewModels.
10. **Replace hardcoded object-type strings with Schema Engine lookups** wherever type names are compared as literals. *(Quick win.)*

---

## Guardrails Already in Place

- **CI gate:** `.github/workflows/ci.yml` runs `./gradlew test --no-daemon` on every push and summarizes results, so a red unit suite blocks merge.
- **Unit test coverage:** **246 JVM unit tests** across the seven module `src/test` sources (verified from test-results after a full `./gradlew test` run: domain 64, data 83, home 32, library 24, settings 26, timeline 9, search 8), plus instrumented DAO and app smoke tests that require a device. See `docs/06-development/TESTING.md` for the per-module breakdown.
- **Critical-invariant tests:** `AiActionParserTest` (29 tests) and `ActionPlanExecutorImplTest` lock down the AI proposal parsing and execution contracts. Note the caveat in Risk 1 — until the parser is unified, `AiActionParserTest` guards the extracted copy rather than the code `HomeViewModel` actually runs; closing that gap is backlog item 1.
