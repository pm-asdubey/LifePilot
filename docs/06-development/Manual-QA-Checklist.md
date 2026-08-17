# Manual QA Checklist

**Location:** `docs/06-development/Manual-QA-Checklist.md`

---

# Purpose

This is a pre-release, on-device checklist for the product owner.

Work through it on a real Android device (not just an emulator) before shipping a build.
Tick each box only after you personally observe the expected result on the device.

Each item is written as: **what to do → expected result.**

---

# Why manual testing is still needed

Automated tests give us a strong safety net, but they cannot see the app.

The current automated suite covers roughly **246 JVM unit test cases** plus instrumented
DAO and navigation smoke tests. Those verify business logic, database behavior, parsing,
and that screens can be reached without crashing.

They do **not** cover:

* **Real camera hardware** — permission dialogs, capture, focus, OCR on a real photo.
* **Real AI responses** — actual network calls, model output quality, malformed replies.
* **Notification delivery** — whether a reminder actually fires and appears in the tray.
* **Visual polish** — layout, spacing, colors, dark mode, truncation, the new logo/splash.
* **Multi-screen gestures** — back navigation, process death, rotation, backgrounding.

Everything in those five categories must be checked by a human, on a device, every release.

---

# Testing tiers — who checks what

Every change falls into one of three verification tiers. Know which one covers your change so nothing
slips through, and so the agent knows what it can verify **on its own** vs. what it must hand to you.

| Tier | Who runs it | When | Covers |
|------|-------------|------|--------|
| **A. Unit + instrumented** | CI (`./gradlew test`) automatically | every push/PR | business logic, parsing, DB behaviour, "screen reachable without crashing" |
| **B. ADB UI regression** | **the agent, via `scripts/adb-regression.sh`** | whenever asked, after a change | app installs, launches, renders core screens, key UI elements present, no crash/ANR |
| **C. Human-on-device** | you | before each release | camera hardware, real AI output, notification delivery, visual polish, gestures |

Tier B is the new automation layer: the agent can drive a connected device over ADB to catch obvious
regressions (a screen that no longer renders, a missing nav tab, a Library that lost its domains, a
launch crash) **without you touching the phone**. It does not replace Tier C — it front-runs it.

---

# Automated regression via ADB (agent-runnable)

**Script:** `scripts/adb-regression.sh` — run it after any change to smoke-test the running app.

```bash
# Build + install the debug APK, then run all checks:
scripts/adb-regression.sh --install

# App already installed — just run checks:
scripts/adb-regression.sh

# Only one feature's checks:
scripts/adb-regression.sh --feature library   # nav | library | chat
```

Requires a connected device/emulator (`adb devices`). Debug package id is `com.lifepilot.app.debug`.
The script exits non-zero if any check fails, writes screenshots + UI dumps to a temp dir, and asserts
on the `uiautomator` UI hierarchy and `logcat` (never edits app data).

**What the agent can verify automatically (per feature):**

| Feature | Automated ADB check (Tier B) | Still needs a human (Tier C) |
|---------|------------------------------|------------------------------|
| Launch / stability | process alive after launch; no `FATAL EXCEPTION`/ANR in logcat | splash animation, logo crispness |
| Navigation | all bottom-nav destinations present in the UI dump | gesture feel, transitions |
| Library (12 domains) | all 12 canonical domain names present as containers | "+" per-domain opens the correct scoped sheet; scroll/expand feel |
| Ask AI | screen opens without crash | real AI answer quality, streaming |
| Camera | (launch only — capture needs hardware) | permission dialog, capture, OCR on a real photo |
| Notifications | (schedule can be asserted in unit tests) | actual tray delivery + tap-through |

**Extending the harness:** when you build a feature, add a check to `scripts/adb-regression.sh` that
navigates to it (`tap_text "<label>"`) and asserts an expected element is in the UI dump
(`ui_contains "<text>"`). Keep each check to "does the screen render its key element and not crash" —
deep behavioural assertions belong in unit/instrumented tests (Tier A). Then this checklist's Tier B
column grows with the app, and "regression check whenever asked" stays a one-command operation.

---

# Test environment setup

* [ ] Install the release-candidate build on a physical device (mid-range if possible).
* [ ] Use a device where you can revoke and re-grant permissions (Settings → Apps).
* [ ] Have a valid AI provider API key ready (to test online AI) and know how to remove it (to test offline fallback).
* [ ] Have at least one real document/photo to scan (e.g. an ID card, bill, or letter).
* [ ] Fresh install recommended for first-run checks (clear app data or reinstall).

---

# 1. App launch, logo, and navigation

* [ ] Cold-launch the app → **new compass launcher icon** appears on the home screen / app drawer (not the old/default icon).
* [ ] During launch → **splash screen shows the new compass logo**, then transitions into Home.
* [ ] Bottom navigation shows five tabs: **Home, Library, Search, Ask AI, Profile** → tapping each switches screens without crashing.
* [ ] Tapping the currently-selected tab again does not duplicate or corrupt the screen.
* [ ] Press system Back from a top-level tab → returns to Home (or exits cleanly from Home) without a blank screen.
* [ ] Rotate the device on each main screen → layout adapts and no crash occurs.

---

# 2. Library — all 12 domains as containers (NEW this session)

* [ ] Open **Library** → all **12 canonical life domains appear as expandable sections, even when empty**:
      Career, Education, Finance, Health, Home, Identity, Legal, Major Life Events, People, Property, Transport, Travel.
* [ ] Confirm the count is exactly 12 and none are hidden just because they have no records.
* [ ] Each domain header shows a **"+" button** → tapping it opens a create sheet **scoped to that domain**, titled **"Add to &lt;domain&gt;"** (e.g. "Add to Finance").
* [ ] An **empty domain** shows the message **"No records yet — tap + to add one."**
* [ ] On a **fresh install / first run**, an **onboarding banner** appears at the top of Library.
* [ ] The **filter chips** at the top still work → selecting a chip filters the list; "All" restores everything.
* [ ] Expand a domain that has records → records are listed under it.
* [ ] **Tap a record → its Object screen opens** and shows the record's details.
* [ ] Add a record via a domain's "+" → the new record lands in that same domain, not another.

---

# 3. Camera flow in AI chat (FIXED this session)

Do these in **Home → Ask AI chat**, using the **attach** button.

* [ ] Tap **attach** → a menu with **Camera, Scan, Photos, Files** appears.
* [ ] **First use of Camera** → the app **requests camera permission at runtime** (system dialog appears). This must happen before the camera opens.
* [ ] **Deny** the permission → a **toast/message is shown and the app does NOT crash**. You return to the chat safely.
* [ ] After denying, **Scan, Photos, and Files still work** (they must not be blocked by the camera denial).
* [ ] **Grant** the permission → camera opens → **capture a photo** → the photo is **OCR'd** and an **AI record proposal appears** for you to verify.
* [ ] **Process-death / backgrounding:** while the camera is open (or immediately after capture), **background the app** (Home button) or **rotate the device**, then return → the **captured photo is still processed** (proposal still appears). It must not be silently lost.
* [ ] **Empty / garbage AI response:** if the AI returns nothing useful or malformed output, the **"thinking" spinner stops** (does not spin forever) and the chat shows a graceful message or no proposal.
* [ ] Approve a proposal → a record is created and appears in Library under the right domain.
* [ ] Dismiss a proposal → nothing is saved.

---

# 4. Projects (Planner, per decisions.md)

* [ ] Ask the AI to make a plan that creates a **project** → the created project shows a **domain-appropriate emoji** (derived from its domain), **not** the generic 🎯.
* [ ] If the AI provided a **target date**, the project shows **"days left"** (countdown). If no date was given, no bogus date is shown.
* [ ] Confirm **Projects live in the Planner** (Planner has **Tasks** and **Projects** tabs). Projects are **NOT** shown in Library.
* [ ] Open a project that has **linked records** → the links are visible.
* [ ] **Delete the project → its linked records are NOT deleted** (verify each linked record still exists in Library afterward). This is critical.
* [ ] Switch between the Tasks and Projects tabs → each shows the correct content, no leakage between tabs.

---

# 5. Core flows regression sweep

## 5.1 Create a record manually

* [ ] Use the Library "+" (or a domain "+") → fill the create sheet → save → record appears in the correct domain and opens correctly.
* [ ] Required-field validation behaves (cannot save an obviously invalid/empty record if fields are required).

## 5.2 Upload a document → OCR → verify → Object

* [ ] Upload/scan a document → OCR runs → **extracted metadata is proposed for verification**.
* [ ] Edit a proposed field, then confirm → verified values are stored on the resulting **Object**.
* [ ] The **original document remains accessible** from the Object (user owns their data).
* [ ] A **timeline entry** is created for the upload/creation.

## 5.3 AI chat Q&A

* [ ] With a **valid API key in Settings**, ask a question about your data → answer is grounded in your records (not hallucinated).
* [ ] **Remove the API key** (or go offline) → AI chat shows the **offline fallback** gracefully, no crash, no infinite spinner.
* [ ] Send a long/awkward message → UI stays responsive and scrolls correctly.

## 5.4 Search

* [ ] Search for a known record title → it appears in results → tapping it opens the Object.
* [ ] Search with no matches → a clean "no results" state (no crash, no stale results).

## 5.5 Timeline

* [ ] Open Timeline → recent events (uploads, creations, status changes) appear in order.
* [ ] Tap a timeline entry → navigates to the related Object where applicable.

## 5.6 Reminders / notifications

* [ ] Create or trigger a reminder → a **notification is actually delivered** to the system tray at the expected time.
* [ ] Tap the notification → it opens the relevant screen in the app.
* [ ] Confirm notification permission is requested (Android 13+) and denial is handled gracefully.

## 5.7 Settings

* [ ] Enter/replace the **AI provider key** → it saves and AI chat starts working online.
* [ ] **Export** data → a file is produced.
* [ ] **Import** that file into a fresh install (or after clearing data) → records are restored correctly.
* [ ] Toggle any theme/appearance options → applied consistently across screens.

---

# 6. Visual & polish pass (eyeball each screen)

* [ ] Home, Library, Search, Ask AI, Profile, Object, Planner, Timeline all render with correct spacing and no clipped text.
* [ ] **Dark mode** looks correct on every screen (if supported).
* [ ] Long titles/descriptions truncate with ellipsis rather than overflowing.
* [ ] Empty states are friendly and correctly worded.
* [ ] Loading spinners appear and then **disappear** (no permanently stuck spinners anywhere).

---

# Known low-priority issues not yet fixed

These are **acceptable for release** and are documented so QA does not re-file them:

* **Orphaned camera capture temp files** accumulate in app storage over time. This is
  **harmless** (uses only local app storage, cleared on app data clear) and is scheduled
  for a later cleanup pass. Do not block release on this.
* **Attached-document context persists across follow-up questions** in AI chat until the
  proposal is **approved or dismissed**. This is **intentional** — it lets the user ask
  follow-up questions about the same attachment. Once the proposal is resolved, the
  context clears.

---

# Sign-off

* [ ] All sections above have been walked through on a physical device.
* [ ] No crashes observed.
* [ ] Any newly discovered issues have been filed with device model, Android version, and steps to reproduce.

Tester: ____________________  Device / OS: ____________________  Build: ____________________  Date: ____________________
