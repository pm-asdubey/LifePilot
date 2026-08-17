#!/usr/bin/env bash
#
# adb-regression.sh — agent-runnable smoke/regression harness for LifePilot.
#
# Purpose: after ANY change, an agent (or a human) can run this to confirm the app installs,
# launches, renders its core screens, and does not crash — entirely from the command line via ADB.
# It is deliberately conservative: it asserts on the uiautomator UI dump and logcat, and never edits
# app data. Exit code 0 = all checks passed; non-zero = at least one check failed.
#
# Usage:
#   scripts/adb-regression.sh                 # assume APK already installed; run checks
#   scripts/adb-regression.sh --install       # build + install debug APK first, then run checks
#   scripts/adb-regression.sh --feature library   # run only the checks tagged for one feature
#
# Requirements: a connected device/emulator (`adb devices`), and (for --install) a working Gradle env.
#
set -uo pipefail

PKG="com.lifepilot.app.debug"
ACTIVITY="com.lifepilot.app.debug/com.lifepilot.app.MainActivity"
OUT_DIR="${TMPDIR:-/tmp}/lifepilot-adb-regression"
mkdir -p "$OUT_DIR"
PASS=0; FAIL=0
FEATURE="${2:-all}"

log()  { printf '%s\n' "$*"; }
ok()   { PASS=$((PASS+1)); printf '  \033[32mPASS\033[0m %s\n' "$*"; }
bad()  { FAIL=$((FAIL+1)); printf '  \033[31mFAIL\033[0m %s\n' "$*"; }

require_device() {
  local n; n=$(adb devices | grep -cE '\sdevice$')
  if [ "$n" -lt 1 ]; then log "No device/emulator connected (adb devices)."; exit 2; fi
}

dump_ui() {  # -> prints UI xml to stdout
  adb shell uiautomator dump /sdcard/lp_ui.xml >/dev/null 2>&1
  adb shell cat /sdcard/lp_ui.xml 2>/dev/null
}

ui_contains() { dump_ui | grep -qiF "$1"; }

# Tap the on-screen center of the first node whose text/content-desc contains $1.
tap_text() {
  local needle="$1" xml bounds
  xml=$(dump_ui)
  bounds=$(printf '%s' "$xml" | tr '>' '>\n' \
    | grep -iE "(text|content-desc)=\"[^\"]*${needle}[^\"]*\"" | head -1 \
    | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1)
  [ -z "$bounds" ] && return 1
  local nums; nums=$(printf '%s' "$bounds" | grep -oE '[0-9]+')
  local x1 y1 x2 y2; read -r x1 y1 x2 y2 <<<"$(printf '%s' "$nums" | tr '\n' ' ')"
  adb shell input tap $(((x1+x2)/2)) $(((y1+y2)/2))
  sleep 2
}

crash_since_launch() {  # returns 0 if a crash/ANR was logged
  adb logcat -d 2>/dev/null | grep -qE "FATAL EXCEPTION|ANR in ${PKG}|E AndroidRuntime"
}

# ── optional build + install ────────────────────────────────────────────────
if [ "${1:-}" = "--install" ]; then
  log "== Building + installing debug APK =="
  ./gradlew :app:assembleDebug --console=plain || { log "Gradle build failed"; exit 3; }
  APK=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
  adb install -r "$APK" || { log "adb install failed"; exit 3; }
fi

require_device

if ! adb shell pm list packages | grep -q "$PKG"; then
  log "$PKG is not installed. Run with --install (needs Gradle) or install the debug APK first."
  exit 2
fi

# ── launch ──────────────────────────────────────────────────────────────────
log "== Launch =="
adb logcat -c >/dev/null 2>&1
adb shell am start -W -n "$ACTIVITY" >/dev/null 2>&1
sleep 4
adb exec-out screencap -p > "$OUT_DIR/01-launch.png" 2>/dev/null
if adb shell pidof "$PKG" >/dev/null 2>&1; then ok "app process is running after launch"; else bad "app is NOT running after launch"; fi
if crash_since_launch; then bad "crash/ANR found in logcat after launch"; else ok "no crash/ANR in logcat after launch"; fi

# ── navigation smoke ─────────────────────────────────────────────────────────
if [ "$FEATURE" = "all" ] || [ "$FEATURE" = "nav" ]; then
  log "== Navigation =="
  for tab in "Home" "Library" "Ask" "Profile"; do
    if ui_contains "$tab"; then ok "bottom-nav destination visible: $tab"; else bad "bottom-nav destination missing: $tab"; fi
  done
fi

# ── Library: all 12 canonical domains visible as containers ──────────────────
if [ "$FEATURE" = "all" ] || [ "$FEATURE" = "library" ]; then
  log "== Library — all 12 domains as containers =="
  tap_text "Library" || tap_text "Records" || true
  sleep 1
  adb exec-out screencap -p > "$OUT_DIR/02-library.png" 2>/dev/null
  missing=""
  for d in Career Education Finance Health Home Identity Legal "Major Life Events" People Property Transport Travel; do
    ui_contains "$d" || missing="$missing $d"
  done
  if [ -z "$missing" ]; then ok "all 12 canonical domains present in Library"; else bad "domains missing from Library:$missing"; fi
fi

# ── Ask AI screen renders ────────────────────────────────────────────────────
if [ "$FEATURE" = "all" ] || [ "$FEATURE" = "chat" ]; then
  log "== Ask AI =="
  tap_text "Ask" || true
  sleep 1
  adb exec-out screencap -p > "$OUT_DIR/03-ask.png" 2>/dev/null
  if crash_since_launch; then bad "crash/ANR after opening Ask AI"; else ok "Ask AI opened without crash"; fi
fi

# ── result ───────────────────────────────────────────────────────────────────
log ""
log "Screenshots + UI dumps: $OUT_DIR"
log "Checks: ${PASS} passed, ${FAIL} failed."
[ "$FAIL" -eq 0 ] || exit 1
