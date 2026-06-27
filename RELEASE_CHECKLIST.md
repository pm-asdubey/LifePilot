# LifePilot 1.0 Release Checklist

This document tracks every verification required before the Version 1.0 release.

Each item must have a completion status based on **evidence**, not assumption.

Legend:
- ✅ Verified — evidence provided
- ⬜ Not yet verified — must be completed before release
- ⚠️ Blocked — cannot proceed without external dependency
- ❌ Known issue — documented below with justification

---

## 1. Build Verification

### Environment

| Requirement | Version | Status |
|-------------|---------|--------|
| Java (JDK) | 17 (Temurin) | ⬜ Verify locally |
| Android Gradle Plugin | 8.5.2 | ⬜ Verify on build |
| Kotlin | 2.0.0 | ⬜ Verify on build |
| Android SDK (compileSdk) | 35 | ⬜ Verify on build |
| minSdk | 26 (Android 8.0) | ⬜ Verify device compatibility |
| targetSdk | 35 | ⬜ Verify on build |

### Build Tasks

| Task | Command | Status | Evidence |
|------|---------|--------|----------|
| Clean checkout | `git clone && cd LifePilot` | ⬜ | — |
| Sync Gradle | `./gradlew dependencies` | ⬜ | — |
| Debug build | `./gradlew assembleDebug` | ⬜ | — |
| Release build (unsigned) | `./gradlew assembleRelease` | ⬜ | — |
| Release AAB | `./gradlew bundleRelease` | ⬜ | — |
| Unit tests | `./gradlew testDebugUnitTest` | ⬜ | — |
| Instrumented tests | `./gradlew connectedDebugAndroidTest` | ⬜ | Requires device/emulator |
| Lint | `./gradlew lintDebug` | ⬜ | — |

### Build Artifacts

| Artifact | Expected Location | Size | Status |
|----------|------------------|------|--------|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | — | ⬜ |
| Release APK | `app/build/outputs/apk/release/app-release.apk` | — | ⬜ |
| Release AAB | `app/build/outputs/bundle/release/app-release.aab` | — | ⬜ |

---

## 2. Signing Configuration

| Item | Status | Notes |
|------|--------|-------|
| Keystore created | ⚠️ | Must be created by release owner |
| Keystore stored securely | ⚠️ | Never commit to repository |
| `local.properties` configured | ⚠️ | Add KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD |
| Signed release APK generated | ⬜ | Requires keystore |
| APK verified with `apksigner` | ⬜ | Run after signing |

```bash
# Generate keystore (run once):
keytool -genkeypair -v -keystore lifepilot.jks -keyalg RSA -keysize 2048 \
  -validity 10000 -alias lifepilot

# Add to local.properties (never commit):
KEYSTORE_PATH=/path/to/lifepilot.jks
KEYSTORE_PASSWORD=<password>
KEY_ALIAS=lifepilot
KEY_PASSWORD=<password>

# Build signed release:
./gradlew assembleRelease

# Verify signing:
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

---

## 3. Permissions Audit

| Permission | Required For | Justified |
|------------|-------------|-----------|
| READ_EXTERNAL_STORAGE (≤ API 32) | File picker for documents | ✅ |
| READ_MEDIA_IMAGES | File picker for images (API 33+) | ✅ |
| CAMERA | Document capture via camera | ✅ |
| USE_BIOMETRIC | Biometric lock screen | ✅ |
| INTERNET | AI provider API calls | ✅ |
| RECEIVE_BOOT_COMPLETED | Restart WorkManager on reboot | ✅ |
| POST_NOTIFICATIONS | Reminder notifications | ✅ |
| VIBRATE | Haptic feedback | ✅ |

All permissions verified against actual usage in code.

---

## 4. Database Verification

| Item | Status | Notes |
|------|--------|-------|
| Room database version | ✅ v1 (first release) | No migration required |
| `exportSchema = true` configured | ✅ | Exports to `data/schemas/` |
| Schema JSON file generated and committed | ⬜ | Run `./gradlew :data:kspDebugKotlin` then commit |
| `fallbackToDestructiveMigration` not set | ✅ | Safe for v1; crash on version mismatch is correct |
| All entities have valid primary keys | ✅ | Verified by code review |
| All DAOs have instrumented tests | ✅ | 9 DAOs × tests (65 total test cases) |

---

## 5. User Workflow Verification

Each workflow must be verified on a physical device or emulator.

**Environment Required:** Android device or emulator running API 26+

| Workflow | Steps | Status | Notes |
|----------|-------|--------|-------|
| First launch / Onboarding | Launch app → profile auto-created | ⬜ | — |
| Create object | Home FAB → select type → fill fields → Save | ⬜ | — |
| Edit object | Object detail → Metadata tab → Edit | ⬜ | — |
| Archive object | Object detail → menu → Archive | ⬜ | — |
| Delete object | Object detail → menu → Delete → confirm | ⬜ | — |
| Upload image | Object detail → Documents → Upload → select image | ⬜ | — |
| Upload PDF | Object detail → Documents → Upload → select PDF | ⬜ | — |
| View PDF | Upload PDF → tap document → verify pages render | ⬜ | — |
| OCR extraction | Upload image with text → verify OCR text appears | ⬜ | — |
| AI metadata extraction | Document viewer → Extract → review AI suggestions | ⬜ | Requires AI key |
| Accept AI suggestions | Extraction screen → select fields → Apply | ⬜ | — |
| AI chat | AI tab → type question → verify structured response | ⬜ | Requires AI key |
| AI unconfigured state | No key set → verify setup banner shown | ⬜ | — |
| Timeline entries | Create object → verify timeline entry appears | ⬜ | — |
| Timeline filtering | Timeline → tap source type filter | ⬜ | — |
| Task generation | Create passport → verify tasks generated | ⬜ | — |
| Complete task | Tasks tab → swipe task to complete | ⬜ | — |
| Object relationships | Object detail → Related → Link another object | ⬜ | — |
| Unlink relationship | Related tab → hold → Unlink | ⬜ | — |
| Search objects | Search tab → type query → verify results | ⬜ | — |
| Search by metadata | Search for a value stored in metadata | ⬜ | — |
| Recent searches | Search → run query → go back → verify saved | ⬜ | — |
| Reminder notification | Set expiry date in past → run evaluation → check notification | ⬜ | Requires background processing |
| Export data | Settings → Export → verify JSON file shared | ⬜ | — |
| Import data | Settings → Import → select exported file → verify objects restored | ⬜ | — |
| Profile rename | Settings → Profile → rename → verify persisted | ⬜ | — |
| Biometric lock | Settings → enable biometric → close/reopen app → verify lock | ⬜ | Requires device with biometric |
| Offline AI behaviour | Disable network → open AI chat → verify graceful fallback | ⬜ | — |
| Pull-to-refresh | Home/Library/Timeline → pull down → verify data refreshes | ⬜ | — |
| Multi-select | Library → long-press object → select more → Archive/Delete | ⬜ | — |

---

## 6. Test Execution Summary

### Unit Tests (domain module)

| Suite | Count | Status | Notes |
|-------|-------|--------|-------|
| LifeStateEngineTest | — | ⬜ Execute to confirm |
| RuleEngineTest | — | ⬜ Execute to confirm |
| TaskGeneratorTest | — | ⬜ Execute to confirm |
| Use case tests (×12) | — | ⬜ Execute to confirm |
| **Total** | **30+** | ⬜ | — |

Command: `./gradlew testDebugUnitTest`

### Instrumented Tests (data module)

| Suite | Cases | Status |
|-------|-------|--------|
| ProfileDaoTest | 7 | ⬜ Execute to confirm |
| ObjectDaoTest | 8 | ⬜ Execute to confirm |
| MetadataDaoTest | 7 | ⬜ Execute to confirm |
| TaskDaoTest | 6 | ⬜ Execute to confirm |
| ReminderDaoTest | 5 | ⬜ Execute to confirm |
| DocumentDaoTest | 8 | ⬜ Execute to confirm |
| TimelineDaoTest | 6 | ⬜ Execute to confirm |
| RelationshipDaoTest | 6 | ⬜ Execute to confirm |
| EventDaoTest | 5 | ⬜ Execute to confirm |
| FileStorageManagerTest | 7 | ⬜ Execute to confirm |
| **Total** | **65** | ⬜ | — |

Command: `./gradlew connectedDebugAndroidTest -p data`

### UI Tests (app module)

| Suite | Cases | Status |
|-------|-------|--------|
| NavigationSmokeTest | 5 | ⬜ Execute to confirm |
| CreateObjectSmokeTest | 2 | ⬜ Execute to confirm |
| SearchFlowTest | 4 | ⬜ Execute to confirm |
| DocumentUploadFlowTest | 2 | ⬜ Execute to confirm |
| **Total** | **13** | ⬜ | — |

Command: `./gradlew connectedDebugAndroidTest -p app`

---

## 7. ProGuard / R8 Verification

| Item | Status | Notes |
|------|--------|-------|
| Release build with minification enabled | ⬜ | Must build and run |
| Kotlinx.serialization rules present | ✅ | In proguard-rules.pro |
| Hilt rules present | ✅ | In proguard-rules.pro |
| Room rules present | ✅ | In proguard-rules.pro |
| App launches with release build | ⬜ | Run on device |
| Core flows work with release build | ⬜ | Smoke test required |

---

## 8. Performance Baseline

| Metric | Target | Status | Measured |
|--------|--------|--------|---------|
| Cold start time | < 2s | ⬜ | — |
| Home screen load | < 500ms | ⬜ | — |
| Search response | < 300ms | ⬜ | — |
| PDF render (1 page) | < 2s | ⬜ | — |
| Memory usage (steady state) | < 150MB | ⬜ | — |

---

## 9. Known Issues and Deferred Items

| ID | Severity | Description | Decision |
|----|----------|-------------|----------|
| KI-001 | Low | `TimelineCard` in object detail is not tappable (no navigation target) | Deferred to 1.1 — tap does nothing but no user expectation set |
| KI-002 | Low | `TaskCard` in object detail is not tappable (no task detail screen) | Deferred to 1.1 — completion via swipe is sufficient for V1 |
| KI-003 | Low | `android:largeHeap="true"` enabled for PDF rendering | Justified — rendering multi-page PDFs requires additional heap |
| KI-004 | Low | Instrumented tests not run in CI (no emulator) | Add `runs-on: ubuntu-latest` with AVD action for 1.1 |
| KI-005 | Medium | Room schema JSON not committed — generated on first build | Generate schema on first build, commit before 1.0 GA |
| KI-006 | Low | AI chat caps context at 30 objects | Documented in ARCHITECTURE.md; acceptable for V1 |

---

## 10. Release Blockers

Before marking 1.0 as released, ALL of the following must be resolved:

- [ ] Debug APK successfully built and installed on device
- [ ] Release APK successfully built (signed)
- [ ] Release AAB successfully built
- [ ] Unit tests executed and passing
- [ ] At least one smoke test run on device for core workflows
- [ ] Room schema file generated and committed
- [ ] Version code and name correct (versionCode=1, versionName=1.0.0) ✅
- [ ] No P0/P1 bugs outstanding
- [ ] CHANGELOG.md finalized with 1.0.0 release entry
- [ ] Play Store listing assets prepared

---

## Environment Blocker (current)

**The following verifications cannot be completed in the current shell environment:**

```
Attempted: java -version
Result: Unable to locate a Java Runtime

Attempted: which gradle
Result: gradle not found

Attempted: ls ~/.android/sdk
Result: No such file or directory
```

**To complete release verification, the following are required:**
- JDK 17 (Temurin distribution recommended)
- Android SDK with API 35 installed
- Android Studio or command-line tools
- A physical device or emulator running API 26+

**Commands to execute once environment is available:**
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest --continue

# Generate Room schema
./gradlew :data:kspDebugKotlin

# Build release (unsigned)
./gradlew assembleRelease

# Build release AAB (unsigned)
./gradlew bundleRelease

# Run instrumented tests (device required)
./gradlew connectedDebugAndroidTest
```
