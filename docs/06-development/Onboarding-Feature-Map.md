# Onboarding Feature Map (for easy revert)

The lightweight first-run onboarding has exactly **two paths** — *paste-a-prompt* and *scan-a-document* —
that feed text to the AI, which classifies it into per-domain understanding (`DomainLifeState`) **without
per-item approval**. It is intentionally self-contained so it can be removed or disabled cleanly.

## Two build distributions (already wired)

`app/build.gradle.kts` defines a `distribution` flavor dimension:

| Flavor | Onboarding | applicationId (debug) | Build / install |
|--------|-----------|-----------------------|-----------------|
| `standard` | **on** | `com.lifepilot.app.debug` | `./gradlew :app:installStandardDebug` |
| `stable` | **off** | `com.lifepilot.app.debug.stable` | `./gradlew :app:installStableDebug` |

Both carry every other fix; they install side by side. Prebuilt copies live in `artifacts/`:
`LifePilot-onboarding-debug.apk` and `LifePilot-stable-no-onboarding-debug.apk`.

Onboarding is gated at runtime on `BuildConfig.ONBOARDING_ENABLED` (per flavor) **and** the
`hasCompletedOnboarding` preference.

## Exact files touched

**New (delete to remove the feature):**
- `app/src/main/java/com/lifepilot/app/onboarding/OnboardingViewModel.kt` — AI classification → `upsertDomainLifeState`.
- `app/src/main/java/com/lifepilot/app/onboarding/OnboardingScreen.kt` — the two-path UI + skip.

**Edited (small, revertible):**
- `data/src/main/java/com/lifepilot/data/repository/PreferenceManager.kt` — added `onboardingCompleteKey`,
  `hasCompletedOnboarding: Flow<Boolean>`, and `setOnboardingComplete()`.
- `app/src/main/java/com/lifepilot/app/MainActivity.kt` — the gate inside `LifePilotApp` (the
  `if (!BuildConfig.ONBOARDING_ENABLED) … else { OnboardingScreen / NavHost }` block).
- `app/build.gradle.kts` — added `implementation(libs.mlkit.document.scanner)` and the
  `standard` / `stable` product flavors with `ONBOARDING_ENABLED`.

**Reused unchanged:** `AiProviderFactory`, `DomainRepository.upsertDomainLifeState`, `OcrService`,
`SchemaEngine.getAllDomains`, the ML Kit document scanner.

## To disable / revert

- **Ship without it now:** build/install the `stable` flavor — no code changes needed.
- **Full revert:** delete the two `onboarding/` files, revert the `MainActivity` gate to
  `else { LifePilotNavHost(...) }`, and (optionally) remove the `PreferenceManager` additions, the
  `app/build.gradle.kts` flavor block, and the scanner dependency. Nothing else references the feature.

## Data flow

`paste text` / `scan → OCR text` → `AiProviderFactory.getProvider().complete(...)` → JSON keyed by
domain → `DomainRepository.upsertDomainLifeState(...)` per domain → retrieved as high-priority context
in every future AI prompt. No records are auto-created; only the AI's domain "understanding" is seeded.
