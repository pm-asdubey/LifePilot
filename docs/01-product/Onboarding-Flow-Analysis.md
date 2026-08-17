# Onboarding / First-Run Experience — Architectural Analysis & Recommendation

**Status:** Proposal
**Author:** Architecture review
**Date:** 2026-07-03
**Scope:** Version 1 (offline-first, Android)
**Related mockup:** `docs/mockups/onboarding/index.html`

---

## 1. Purpose

This document analyses LifePilot's current first-run experience, evaluates what
is architecturally achievable given the offline-first + Schema Engine + Profiles +
pluggable AI-provider design, and recommends a concrete, buildable onboarding flow
that respects every constraint in `CLAUDE.md` and `MASTER-SPEC.md`.

The guiding tension: LifePilot is **not a chatbot** and **not a folder app** — it is a
structured Life State Engine. A new user opening the app for the first time sees an empty
Home ("What needs attention?") with nothing in it. Onboarding's job is to convert an empty
database into a *personally relevant, populated Library* and to establish trust ("your data
stays on your device") — without ever violating offline-first or the "AI proposes, user
verifies" rule.

---

## 2. Current State (what exists today)

**There is no onboarding.** A brand-new user is dropped directly onto an empty Home screen.
The "first-run" behaviour is entirely implicit and happens in the background.

### 2.1 Launch path

`MainActivity.onCreate()` installs the OS splash screen and immediately sets the Compose
content to `LifePilotApp`, which renders `LifePilotNavHost`.

- `app/src/main/java/com/lifepilot/app/MainActivity.kt:41-55` — `installSplashScreen()`,
  then `LifePilotApp(...)`.
- `app/src/main/java/com/lifepilot/app/navigation/LifePilotNavHost.kt:67` —
  `startDestination = HOME.route`. There is **no** welcome/onboarding destination in the graph;
  the eight registered screens are Home, Library, Search, AI, Settings, Object, Document, Timeline.
- The only pre-Home gate is `BiometricLockScreen`, shown *only if* the user has already
  enabled biometric lock in Settings (`MainActivity.kt:81-88`). For a first-run user this is
  never active, so launch → Home is immediate.

### 2.2 First profile is created silently

The first `Profile` is created automatically, with a hardcoded name, with no user input.

- `app/src/main/java/com/lifepilot/app/initializer/AppInitializer.kt:42-55` —
  `ensureDefaultProfile()` runs on a background IO coroutine from
  `LifePilotApplication.onCreate()`. If no profiles exist it calls
  `profileRepository.createProfile("My Profile", isPrimary = true)` and stores the active
  profile id in `PreferenceManager`.
- `domain/src/main/java/com/lifepilot/domain/repository/ProfileRepository.kt` already exposes
  everything an onboarding step would need: `createProfile(displayName, isPrimary)`,
  `observeProfiles()`, `setActiveProfile(...)`.

The Profile is a first-class canonical entity, but the user never names it, never sees it,
and never learns it exists.

### 2.3 AI key setup is buried in Settings

There is no first-run AI step. The user can only configure AI *after* discovering Settings.

- `features/settings/.../ui/SettingsScreen.kt:134-149` — a free-text "Provider" field
  (placeholder `"anthropic / nvidia"`) and an "API Key" field. This is developer-grade UX:
  the provider is typed as a raw string.
- `PreferenceManager` (`data/.../repository/PreferenceManager.kt`) persists `ai_provider`
  and `ai_model` in DataStore and stores the API key via `EncryptedKeyStorage`
  (encrypted-at-rest — good). `clearAiConfig()` exists for reset.
- `data/.../ai/AiProviderFactory.kt:17-27` — **graceful offline fallback already exists**:
  if the API key is null/blank it returns `OfflineAiProvider`; otherwise it dispatches to
  `AnthropicAiProvider` / `NvidiaAiProvider`. `isConfigured()` reports whether a key is set.

This is the single most important architectural fact for onboarding: **the app is fully
functional with zero AI configuration.** An onboarding AI step can be entirely optional.

### 2.4 Permissions are requested cold, with no priming

- `MainActivity.kt:64-71` — `requestNotificationPermissionIfNeeded()` fires the
  `POST_NOTIFICATIONS` system dialog *immediately on first launch*, before the user
  understands what notifications are for. There is no rationale screen ("priming").
- The manifest declares `CAMERA`, `READ_MEDIA_IMAGES/VIDEO`, `POST_NOTIFICATIONS`,
  `USE_BIOMETRIC`, etc. (`app/src/main/AndroidManifest.xml`). Camera/media are requested
  lazily elsewhere (document capture), which is correct, but notifications are the one
  cold-prompt today.

### 2.5 The Library starts empty; the Schema Engine is ready to seed it

- `data/src/main/assets/schemas/*.json` — 20 object-type schemas exist (Job, Passport,
  Vehicle, Insurance, Property, Bank Account, Loan, Tax, Health, Education, Travel, Will…).
  Each declares a `"domain"` (Career, Finance, Identity, Health, Home, Legal, Property,
  Transport, Travel, Education observed in current files).
- `domain/.../engine/SchemaEngine.kt` exposes `getAllDomains()`, `getObjectTypesByDomain(domain)`,
  and `getAllObjectTypes()`. Schemas are loaded at startup via `schemaEngine.loadSchemas()`
  (`AppInitializer.kt:33`).
- **There is no `SampleDataSeeder`** and no domain-selection step. Nothing pre-populates or
  prioritises the Library for a new user, so first Home is blank and undifferentiated.

### 2.6 Splash

- `app/src/main/res/values/themes.xml` — `Theme.LifePilot.Splash` uses the Android 12
  splash-screen API: `windowSplashScreenBackground = @color/splash_background` and the
  launcher foreground as the animated icon. This is a pure OS splash (brand color + icon);
  it is **not** a welcome screen and cannot carry copy or choices.

### 2.7 Summary of the current gap

| Concern | Today |
|---|---|
| Welcome / brand moment | None (OS splash only) |
| Profile creation | Silent, hardcoded `"My Profile"` |
| Life-domain personalisation | None — Library starts empty |
| AI setup | Buried in Settings, free-text provider |
| Offline expectation-setting | None |
| Notification permission | Cold system prompt on launch |
| Camera/storage priming | Lazy at point of use (acceptable) |
| Sample data | None |
| "Your data stays on device" trust message | Never stated |

---

## 3. What Is Architecturally Possible

Everything below is buildable *today* on existing abstractions, with **no backend** and
**no new root entities**.

1. **A pre-Home onboarding graph.** Add an `onboarding` nav destination and make
   `startDestination` conditional on a `hasCompletedOnboarding` flag. The flag is a trivial
   addition to `PreferenceManager` (a `booleanPreferencesKey`, mirroring the existing
   `biometric_lock_enabled` pattern) — no schema/DB migration required.

2. **User-named Profile creation.** Replace the silent `createProfile("My Profile", …)` with
   a real onboarding step that calls the *same* `ProfileRepository.createProfile(name, isPrimary=true)`
   and `PreferenceManager.setActiveProfileId(...)`. `ensureDefaultProfile()` stays as a
   safety net (idempotent) for upgrades and edge cases.

3. **Domain-selection onboarding that seeds/prioritises the Library.** Because the Schema
   Engine is config-driven and already groups object types by domain, an onboarding step can
   present the 12 canonical life domains (Career, Education, Finance, Health, Home, Identity,
   Legal, Major Life Events, People, Property, Transport, Travel), let the user pick the ones
   relevant to them, and persist the selection. That selection can (a) order/filter the Library
   and Home suggestions and (b) drive an *opt-in* set of starter Objects — all through existing
   repositories and the Life State Engine pipeline. No hardcoding: the domains come from
   `SchemaEngine.getAllDomains()` and object types from `getObjectTypesByDomain(domain)`.

4. **Optional AI step with graceful offline skip.** `AiProviderFactory` already returns
   `OfflineAiProvider` when unconfigured. An onboarding AI step can therefore offer "Add an AI
   key" vs. "Continue offline" as equal, first-class choices. Provider selection should become
   a proper picker (Anthropic / Nvidia / Offline) instead of the free-text field, writing to
   the existing `setAiProvider` / `setAiApiKey`.

5. **Permission priming (rationale-before-request).** Move the `POST_NOTIFICATIONS` request
   out of `MainActivity.onCreate()` into an onboarding "Stay ahead of deadlines" screen that
   explains *why* before launching the system dialog. Camera/media priming can be handled the
   same way, or left lazy at document-capture time (recommended).

6. **Opt-in sample data.** A new `SampleDataSeeder` (data layer) can create a few example
   Objects/Documents/Events *only if the user explicitly opts in*, routed through the normal
   Life State Engine so the seeded data is indistinguishable from real data and fully deletable.

7. **Progressive disclosure & resumability.** Onboarding state (current step, selected domains)
   can live in an onboarding `ViewModel` backed by `PreferenceManager`, so a killed process
   resumes gracefully — consistent with the "single source of truth / immutable UI state"
   principle.

---

## 4. Recommendations (prioritised, tied to architecture)

**P0 — Establish the profile with intent.** Introduce a real welcome + profile-name step.
Reuse `ProfileRepository.createProfile`. Add `hasCompletedOnboarding` to `PreferenceManager`.
Gate `LifePilotNavHost.startDestination` on it. *Low effort, high trust payoff.*

**P0 — Fix the cold notification prompt.** Remove the eager request from `MainActivity` and
prime it inside onboarding with a rationale screen. *Play-Store-quality UX and policy-friendly.*

**P1 — Domain-selection step that shapes the Library.** Drive the 12 canonical domains from
`SchemaEngine.getAllDomains()`. Persist the chosen set; use it to order Home/Library and to
scope the (optional) starter Objects. This is the step that makes the empty app feel personal
and is the strongest justification for having onboarding at all.

**P1 — Turn AI setup into a proper, optional step.** Replace the free-text provider field with
a segmented picker (Anthropic / Nvidia / Offline). Make "Continue offline" a prominent,
guilt-free choice. Reuse `AiProviderFactory` fallback semantics; validate the key format
locally only (no network call required to finish onboarding).

**P2 — Opt-in sample data.** Add `SampleDataSeeder` routed through the Life State Engine,
triggered only by explicit consent, clearly labelled and one-tap removable.

**P2 — "Your data stays here" trust screen.** A short offline-first / on-device / encrypted-key
statement. Cheap to build, disproportionately valuable for a personal-data product.

---

## 5. Improvements To What Already Exists

- **`AppInitializer.ensureDefaultProfile()`** — keep it, but demote it to a *fallback*. After
  onboarding ships, the primary path names the profile explicitly; the initializer only creates
  `"My Profile"` if somehow none exists (upgrades, restore). Idempotency is already correct.
- **`SettingsScreen` AI provider field** — the free-text `"anthropic / nvidia"` input is a
  latent bug source (typos silently fall through to Offline in `AiProviderFactory`). Convert to
  an enum-backed picker in both Settings and onboarding, sharing one component.
- **`MainActivity` permission logic** — remove eager notification request; centralise permission
  requests behind primers.
- **Splash** — the OS splash is fine and should stay; do not try to overload it with copy.
  The branded "welcome" belongs on the first onboarding Compose screen, not the window splash.

---

## 6. What Is NOT Possible / NOT Advisable

- **Any account / sign-in / cloud step.** Out of scope by `MASTER-SPEC.md` ("Explicitly Out of
  Scope: Cloud Sync, Backend, Shared Accounts"). Onboarding must never ask for an email/login.
- **Calling AI during onboarding before a key is set, or silently using a key.** Violates
  offline-first and "AI proposes, user verifies". The AI step must be skippable and must not
  make a network call to *complete* onboarding. Any key validation must be optional/local.
- **Auto-importing the user's files/photos to pre-populate Objects.** Violates "User Owns Their
  Data" and user-verification. Bulk import may be *offered* later, but never performed silently
  during onboarding.
- **Seeding real-looking data without consent.** Sample data must be opt-in and removable, or it
  pollutes the Life State Engine and erodes trust.
- **Making onboarding a hard blocker with no escape.** Every step except profile name should be
  skippable; forcing choices contradicts "calm, progressive disclosure".
- **Requesting camera/storage up front "just in case".** Request lazily at first document capture;
  only *prime* notifications during onboarding.
- **Hardcoding the domain or object-type list in the onboarding UI.** Would violate the
  Configuration-Driven constraint; pull from the Schema Engine.

---

## 7. Recommended Flow (best direction)

A calm, six-screen, mostly-skippable flow. Screen count is deliberately small; each screen
"answers one question" (per the UI principles). This is the flow rendered in the mockup.

1. **Welcome / Brand** — compass logo, one line: *"The operating system for your admin life."*
   Sub-line: *"Private. Offline-first. Yours."* Single **Get started** button.
   *Rationale:* establishes brand and the offline-first promise immediately.

2. **Your Profile** — text field for a display name (pre-filled `"My Profile"`, editable).
   Explains a Profile keeps one person's life separate; more can be added later.
   *Rationale:* makes the core `Profile` entity intentional. Calls
   `createProfile(name, isPrimary=true)`. **Only mandatory step** (name can default).

3. **What matters to you** — chips/cards for the 12 canonical domains (Career, Education,
   Finance, Health, Home, Identity, Legal, Major Life Events, People, Property, Transport,
   Travel). Multi-select, none required. Selection personalises Library/Home ordering and scopes
   optional starters. Source: `SchemaEngine.getAllDomains()`.
   *Rationale:* the step that turns an empty app into *your* app; fully config-driven.

4. **Smarter with AI (optional)** — segmented control: **Anthropic · Nvidia · Stay offline**.
   Picking a provider reveals an API-key field with a "where do I get this?" hint and a "your key
   is stored encrypted on this device" note. **Stay offline** is equally prominent and default-safe.
   *Rationale:* respects offline-first; reuses `AiProviderFactory` fallback + `EncryptedKeyStorage`.

5. **Stay ahead (permissions priming)** — explains reminders/notifications, then a button that
   triggers the real `POST_NOTIFICATIONS` request. Clear "Not now" escape. Optional line about
   camera being requested later when scanning a document.
   *Rationale:* rationale-before-request; fixes today's cold prompt.

6. **You're set** — confirmation, optional **Add a few examples to explore** (opt-in sample data),
   and **Go to Home**. Sets `hasCompletedOnboarding = true`.
   *Rationale:* clean handoff to Home ("What needs attention?") that is no longer empty if the
   user opted into starters/samples.

Back/Next navigation throughout; every screen after #2 is skippable; state is resumable.

---

## 8. Phased Build Plan

**Phase 1 — Skeleton & gating (P0).**
- Add `hasCompletedOnboarding` to `PreferenceManager`.
- New `features/onboarding` module (or a package under `app`) with an onboarding nav graph.
- Make `LifePilotNavHost` start on `onboarding` when the flag is false, else `HOME`.
- Screens 1 (Welcome) + 2 (Profile) wired to `ProfileRepository`.
- Demote `ensureDefaultProfile()` to fallback.
- Remove eager notification request from `MainActivity`.

**Phase 2 — Personalisation (P1).**
- Screen 3 domain selection from `SchemaEngine.getAllDomains()`; persist selection.
- Use selection to order Home/Library.
- Screen 4 AI step with provider picker + encrypted key; shared with a refactored Settings picker.

**Phase 3 — Priming & delight (P2).**
- Screen 5 notification priming (rationale → real request).
- Screen 6 done + opt-in `SampleDataSeeder` (routed through Life State Engine, removable).
- "Data stays here" trust copy.

**Phase 4 — Polish.**
- Resumability across process death, accessibility pass, animations, unit tests for the
  onboarding `ViewModel` and the gating logic (`features/onboarding/src/test`,
  per the project's test-location table).

---

## 9. Privacy, Permissions & Offline-First Notes

- **Notifications (`POST_NOTIFICATIONS`)** — request only after priming, with a clear "Not now".
  Never block completion on it.
- **Camera / media (`CAMERA`, `READ_MEDIA_*`)** — do **not** request during onboarding; request
  lazily at first document capture. Onboarding may *mention* it, not demand it.
- **AI key** — stored via `EncryptedKeyStorage` (encrypted at rest). Never logged (see Security
  section of `CLAUDE.md`). No network call is required to finish onboarding.
- **Offline-first fallback** — onboarding must fully complete with the device in airplane mode;
  `AiProviderFactory` already returns `OfflineAiProvider` when unconfigured, so the AI step is
  genuinely optional.
- **No data leaves the device** during onboarding — this must be *stated* (trust screen) and
  *true* (no backend calls anywhere in the flow).

---

## 10. Architectural Fit Checklist

| Constraint (`CLAUDE.md` / `MASTER-SPEC.md`) | How this flow complies |
|---|---|
| Offline-first | Completes in airplane mode; AI optional via existing fallback |
| No new root entities | Uses only Profile + existing schema-driven Objects |
| Configuration-driven | Domains/object types pulled from Schema Engine, not hardcoded |
| AI proposes, user verifies | No AI call to complete onboarding; sample data is opt-in |
| User owns data | No silent import; encrypted local key; removable samples |
| Clean layering | UI → onboarding ViewModel → repositories/PreferenceManager |
| Calm, progressive disclosure | 6 short screens, one question each, mostly skippable |
| Future compatibility | Onboarding flag + module leave room for cloud/family later |
</content>
