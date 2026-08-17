# Stability & Maintainability Guide

**Purpose:** keep LifePilot stable and fast to work on as it grows, and give any engineer — human or
AI — enough context to make a change safely *without re-deriving the whole codebase*. Read this first
when picking up work.

Last updated: 2026-07-03.

---

## 1. How to work here without breaking things

1. **Run the JVM tests before and after every change.** They are fast (~1–2 min) and catch most
   regressions:
   ```bash
   ./gradlew test          # all 246 JVM unit tests (mirrors CI)
   ```
   For a device build: `./gradlew :app:assembleDebug`.
2. **Never weaken a "critical invariant" test** (see `TESTING.md`). They each guard a bug that already
   shipped once.
3. **Add a test with every change.** Bug fix → a regression test that fails before your fix. New
   behaviour → happy path + one failure case. This is what stops the back-and-forth.
4. **Keep business logic out of ViewModels and Composables.** It belongs in use cases / engines. This
   is both an architecture rule (`CLAUDE.md`) and the single biggest source of untestable code here.
5. **Prefer small, focused files.** Large files (>800 lines) are where context gets lost and bugs
   hide. When you touch one, leave it smaller than you found it.

---

## 2. Architecture map (so you don't have to grep for it)

```
app            → Application, MainActivity, navigation host, AppInitializer, SampleDataSeeder
core:common    → shared utilities
domain         → models, repository INTERFACES, use cases, engine INTERFACES (pure JVM, no Android)
data           → Room DB + DAOs, repository IMPLs, engines (LifeState/Rule/Schema/Retrieval/
                 PromptBuilder/ActionPlanExecutor/DomainLifeState/Planning), AI providers, OCR, workers
designsystem   → Material 3 theme, Spacing, reusable components, DomainIcons, LifePilotLogo
features:home        → AI chat + daily brief (HomeViewModel — the big one)
features:library     → Records browser, grouped by the 12 canonical domains
features:planner     → Tasks + Projects (Projects live HERE per ADR-002, not Library)
features:object      → create / detail / metadata verification for a record
features:search      → cross-entity search
features:settings    → AI provider key, export/import, preferences
features:timeline    → chronological life events
features:document    → document viewer
```

**Dependency direction (never violate):** UI → ViewModel → UseCase → Repository (interface) → Room/File.
The `domain` module must never import Android.

**Canonical entities:** Profile, Object (record), Document, Event, Task, Reminder, Relationship,
Timeline, Domain, Project. Avoid adding new root entities.

**12 canonical life domains** (source of truth: `SchemaEngineImpl.canonicalDomains`, guarded by
`SchemaEngineCanonicalDomainsTest`): Career, Education, Finance, Health, Home, Identity, Legal,
Major Life Events, People, Property, Transport, Travel.

---

## 3. Guardrails already in place

- **CI gate** (`.github/workflows/ci.yml`): every push/PR runs `./gradlew test`; a failure blocks the
  branch and uploads reports.
- **246 JVM unit tests** across 7 modules (see `TESTING.md` for the per-module breakdown), plus
  instrumented DAO + smoke tests.
- **Critical-invariant tests** for the three bugs that already shipped (CASCADE-DELETE conversations,
  bare-string action JSON, stripMarkdown-before-extraction).
- **`CLAUDE.md` → "Known Dangerous Patterns"** documents the traps with their root-cause analysis.

---

## 4. Known technical debt (prioritized, all non-breaking to fix)

Full analysis in `docs/04-architecture/Architectural-Review-2026-07.md`. The short list:

| # | Debt | Why it matters | Fix (non-breaking) |
|---|------|----------------|--------------------|
| 1 | `HomeViewModel` is ~1,550 lines / 21 deps and has an **inline copy of the AI parser** that duplicates the tested `AiActionParser`. | The invariant test guards the *unused* copy; the two can drift. God object is hard to test. | Inject `AiActionParser`; delete the inline `parseAction()`. Then extract `AiRetryClient`, `AttachmentIngestor`, `ProposalExecutor`. Each behind the same public API. |
| 2 | AI creation paths bypass the Life State Engine (write straight to repos). | AI-created records get no timeline/task/reminder pipeline. | Route AI writes through `CreateObjectUseCase`. |
| 3 | Business rules inside Composables (`ObjectDetailScreen` visa-expiry math, `PlannerScreen` "Behind" rule). | Untestable, violates layering. | Lift to pure functions the ViewModel exposes. |
| 4 | `PreferenceManager` (a `data.*` class) injected into 5 ViewModels. | Layer leak. | Extract a `domain` interface, bind in Hilt. |
| 5 | Duplicated APPEND-merge logic in `HomeViewModel` + `ActionPlanExecutorImpl`. | Fix-in-one-not-the-other bugs. | Extract one `MetadataMergeStrategy`. |
| 6 | `PlanningEngineImpl.recalculateGoalProgress()` / `generateTasksForGoal()` are silent no-ops. | `completeTask` claims to recalc but doesn't. | Implement or delete + track. |

> **When you split a large file or extract a class, do it behind the existing public API and keep the
> tests green at each step.** That is how this stays regression-free.

---

## 5. Session change log — 2026-07-03 (stability pass)

Done and verified green (`./gradlew test` + `:app:assembleDebug`):

- **Test suite recovered & unified** after a multi-agent session left divergent worktrees. 246 JVM
  unit tests, 0 failures. Added `org.json` test dep to `:data` (silent-parse-failure fix), fixed the
  `DomainLifeStateEngineImplTest` mocking (non-relaxed `AiProvider`, correct `complete` arity), and a
  **real production bug** in `DomainLifeStateEngineImpl` markdown-fenced JSON parsing.
- **Library: all 12 domains are always-visible containers** — expandable sections even at 0 records,
  each with a domain-scoped "+" (files documents/records into that domain), first-run banner,
  per-domain life-history cards. Regression test added.
- **Projects (per decisions.md):** propagated `emoji` + `targetDate` through the AI path; added a
  deterministic `DomainEmoji` domain→emoji map (ADR-002). Removed the orphaned Library `ProjectDetail`
  trio + duplicate `project/{projectId}` route (ADR-002 violation / shadowed dead code).
- **Camera flow (AI chat):** added the runtime CAMERA permission gate (was a crash risk), persisted
  the capture URI across process death (`rememberSaveable`), and fixed the infinite "thinking" spinner
  on an empty AI response.
- **Dead code removed:** deleted the orphaned `features:ai` module (918 lines, incl. a 3rd divergent
  parser copy) + its `settings.gradle` include.
- **New compass/pilot logo:** adaptive launcher icon + `LifePilotLogo` brand mark.
- **Docs:** this guide, updated `TESTING.md`, `Architectural-Review-2026-07.md`,
  `Onboarding-Flow-Analysis.md` + HTML mockup, `Manual-QA-Checklist.md`.

---

## 6. Recommended next steps (not yet done — ordered)

These are scoped so a future session can pick up ONE at a time and stay green:

1. ~~**Unify the AI parser**~~ ✅ **DONE 2026-07-03** — `HomeViewModel` now delegates both parse call
   sites to the injected `AiActionParser`; the ~360-line inline duplicate was deleted. The
   `AiActionParserTest` suite now guards the exact code production runs. Next god-object extractions
   (`AiRetryClient`, `AttachmentIngestor`, `ProposalExecutor`) remain — see debt table rows above.
2. **Remaining Projects spec items** (see `Architectural-Review` + the Projects audit): runtime status
   chip (On-track/Behind/Blocked) as a shared helper; Overview tab Life-State chips + AI-insight card;
   Migration 7 for `documents.project_id` + `linkDocument`; flip `is_ai_proposed` on approval; remove
   the `🎯` pre-selection in the manual create-project sheet.
3. **Task notifications** (requested): due-date scheduling via WorkManager, notification channels,
   a `BroadcastReceiver` "Complete" action, and Settings for priority filter + time-of-day. Build on the
   existing `ReminderEvaluationWorker` + reminder infra.
4. **ISSUES.md current-feature improvements** (triaged, non-breaking): quick wins 006 (dead "Ask AI"
   nav), 008 (proactive prompt), 012/044 (editable proposal cards), 083 (contextual thinking status),
   028-remainder (confidence surfacing); then 015 (travel↔passport retrieval), 002 (task completion →
   domain update), 070 (personalized suggestion chips). Mark ISSUE-035 resolved (already offline-safe).
5. **UI polish pass** (calm & premium + animation) — refine theme + add tasteful spring animations; run
   the full test suite + smoke tests afterward to confirm no functional regression.
6. **Split the god objects** (debt #1 steps 2–4, and the large Composables) once #1 lands.

Keep the "one change → tests green → commit" rhythm. That, plus this doc and the review, is what keeps
the next session from thrashing.
