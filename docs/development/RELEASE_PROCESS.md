# LifePilot Release Process

## Overview

This document describes the end-to-end release process for LifePilot development builds.

The intended workflow after one-time setup is:

```
Edit code  →  git commit  →  git push origin release/x.y.z
                                      ↓
                           GitHub Actions builds signed APK
                                      ↓
                           GitHub Release created automatically
                                      ↓
                           App checks GitHub on next launch
                                      ↓
                           Settings shows "Version x.y.z available"
                                      ↓
                           Tap "Download Update" → browser opens
                                      ↓
                           Download APK → Android installs it
```

---

## Repository Setup (one time only)

See the [One-Time Setup Checklist](#one-time-setup-checklist) at the bottom of this document.

---

## Versioning Strategy

All version information lives in one place: `app/build.gradle.kts`.

```kotlin
val appVersionCode = 1       // Increment by 1 on every release (never decrement)
val appVersionName = "1.0.0" // Follows semver: MAJOR.MINOR.PATCH
```

**Rules:**
- `versionCode` must increase monotonically. Android rejects installs where the new APK has a lower or equal `versionCode` than the installed app.
- `versionName` must follow `MAJOR.MINOR.PATCH` exactly. The GitHub Release tag is `v{versionName}` (e.g. `v1.0.0`).
- Both values must be updated in the same commit that triggers the release.

**Typical increment patterns:**

| Change Type | Example |
|-------------|---------|
| Bug fix | `1.0.0` → `1.0.1` |
| New feature | `1.0.0` → `1.1.0` |
| Breaking change | `1.0.0` → `2.0.0` |

---

## Triggering a Release

A release is triggered automatically when you push to a `release/**` branch:

```bash
git checkout -b release/1.0.1
# Update versionCode and versionName in app/build.gradle.kts
git add app/build.gradle.kts
git commit -m "chore: bump version to 1.0.1"
git push origin release/1.0.1
```

GitHub Actions will:
1. Run unit tests
2. Build a signed release APK
3. Create a GitHub Release tagged `v1.0.1`
4. Attach the APK

You can also trigger a release manually from the **Actions** tab → **Release** → **Run workflow**.

---

## Signing

### Key concepts
- The keystore file is **never committed** to the repository.
- Signing credentials are stored as **GitHub Secrets** (encrypted at rest).
- In CI, the keystore is decoded from `KEYSTORE_BASE64` at build time and deleted after.
- Locally, credentials can be set in `local.properties` (which is gitignored).

### Required GitHub Secrets

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | Password for the keystore |
| `KEY_ALIAS` | Alias of the signing key inside the keystore |
| `KEY_PASSWORD` | Password for the key |

### Generating the keystore (one time only)

```bash
keytool -genkey -v \
  -keystore lifepilot-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias lifepilot
```

Keep this file in a safe location (password manager or secure local folder). Do not commit it.

### Encoding the keystore for GitHub Secrets

```bash
base64 -i lifepilot-release.jks | pbcopy  # macOS
# Paste the output as the value of the KEYSTORE_BASE64 secret
```

### Local signing (optional)

Add to `local.properties` (already in `.gitignore`):

```properties
KEYSTORE_PATH=/absolute/path/to/lifepilot-release.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=lifepilot
KEY_PASSWORD=your_key_password
```

Then build locally with:

```bash
./gradlew assembleRelease -Pgithub.repo=yourname/lifepilot
```

---

## GitHub Repository Configuration

### Setting the repository slug

The app uses `GITHUB_REPO` (format `owner/repo`) to construct the GitHub API URL for update checks.

In `local.properties`:
```properties
github.repo=yourname/lifepilot
```

In CI, this is automatically set to `${{ github.repository }}` by the workflow — no manual configuration needed.

### Required repository settings

1. **Actions permissions**: Settings → Actions → General → Allow all actions
2. **Workflow permissions**: Settings → Actions → General → Workflow permissions → Read and write permissions
3. **GitHub Secrets**: Settings → Secrets and variables → Actions → New repository secret

---

## Update Checking

The app checks for updates automatically on each launch using `UpdateCheckWorker` (WorkManager).

**Behaviour:**
- The check runs once per launch, but only contacts GitHub if 24+ hours have elapsed since the last successful check.
- Results are persisted in DataStore and survive app restarts.
- Network failures are silent — the app never crashes due to a failed update check.
- If `CONFIGURE_ME/lifepilot` appears as the repo (not yet configured), the check is skipped.

**User flow when update is available:**
1. Open Settings → "APP UPDATE" section shows "Version X.X.X is available"
2. Tap "Download Update" → GitHub Release page opens in browser
3. Download the APK from the release assets
4. Android prompts to install — accept

**Manual check:** Tap "Check now" in Settings → APP UPDATE.

---

## GitHub Actions Workflow Summary

### `ci.yml` (existing)
- Triggers on: push to `main`, `develop`, `release/**`; PRs to `main`, `release/**`
- Builds debug APK, runs unit tests, runs lint
- Does NOT create releases

### `release.yml` (new)
- Triggers on: push to `release/**`, manual `workflow_dispatch`
- Builds signed release APK
- Creates GitHub Release with tag, notes, commit SHA, build timestamp
- Attaches APK as release asset

---

## Developer Workflow

### Day-to-day development

Work on `develop` or feature branches. Push freely — CI builds and tests, but does not publish releases.

### Publishing a release

```bash
# 1. Update version in app/build.gradle.kts
#    appVersionCode = <previous + 1>
#    appVersionName = "x.y.z"

# 2. Commit
git add app/build.gradle.kts
git commit -m "chore: bump version to x.y.z"

# 3. Push to a release branch — this triggers the release workflow
git checkout -b release/x.y.z
git push origin release/x.y.z

# 4. Monitor Actions tab — release is created within ~5 minutes
```

---

## Future Migration to Google Play Internal Testing

When ready to migrate from GitHub Releases to Google Play Internal Testing:

1. Set up Google Play service account credentials in GitHub Secrets
2. Add `bundleRelease` step to the workflow (builds AAB instead of APK)
3. Use `r0adkll/upload-google-play` action to upload to Internal Testing track
4. Remove or disable the `UpdateCheckWorker` (or let it coexist with Play Store updates)
5. Update `UpdateRepository` to point to Play Store version info API

The current architecture keeps networking isolated behind `UpdateRepository`, so swapping the source is a single-file change.

---

## One-Time Setup Checklist

Complete these steps once before using the release pipeline.

### 1. Generate signing keystore
```bash
keytool -genkey -v \
  -keystore lifepilot-release.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias lifepilot
```
Store `lifepilot-release.jks` and its passwords in a password manager.

### 2. Encode keystore as base64
```bash
base64 -i lifepilot-release.jks | pbcopy
```

### 3. Add GitHub Secrets
Go to: **GitHub repo → Settings → Secrets and variables → Actions**

| Secret name | Value |
|-------------|-------|
| `KEYSTORE_BASE64` | Paste from step 2 |
| `KEYSTORE_PASSWORD` | Keystore password from step 1 |
| `KEY_ALIAS` | `lifepilot` (or whatever alias you chose) |
| `KEY_PASSWORD` | Key password from step 1 |

### 4. Enable Actions write permissions
Go to: **GitHub repo → Settings → Actions → General → Workflow permissions**
Select: **Read and write permissions**

### 5. Configure local.properties
Add to `local.properties` (never commit this file):
```properties
github.repo=yourname/lifepilot
```

### 6. Verify setup
Push any commit to a `release/**` branch and confirm:
- Actions tab shows the Release workflow running
- Tests pass
- APK is built and attached to a GitHub Release

Everything else is automated from this point forward.

---

## Known Limitations

- **APK install size**: Release APK with R8 minification is significantly smaller than debug, but still requires ~50MB+ free space.
- **"Unknown sources" permission**: The user must enable "Install unknown apps" in Android Settings the first time. Subsequent updates via the same installer do not require this.
- **GitHub API rate limit**: Unauthenticated GitHub API calls are limited to 60 requests/hour per IP. The 24-hour check interval keeps usage well within limits. On a shared NAT, rate limiting is theoretically possible but extremely unlikely for a single-user dev setup.
- **Tag collisions**: If you push the same `versionName` twice, the `softprops/action-gh-release` action will overwrite the existing release. Always increment `versionCode` and `versionName` before releasing.
- **No delta updates**: Each update requires downloading the full APK (~20-50MB). This is a development distribution system — not intended for large user populations.
