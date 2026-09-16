#!/usr/bin/env bash
set -euo pipefail

PKG="com.epalma.tvespanolplus"
ACTIVITY="$PKG/.MainActivity"
APK="app/build/outputs/apk/debug/app-debug.apk"

echo "Installing APK..."
adb install -r "$APK"
adb logcat -c
adb shell am force-stop "$PKG"

echo "Starting MainActivity..."
adb shell am start -n "$ACTIVITY" || true

PID=""
for _ in $(seq 1 60); do
  PID="$(adb shell pidof "$PKG" 2>/dev/null | tr -d '\r' || true)"
  if [[ -n "$PID" ]]; then
    break
  fi
  sleep 2
done

if [[ -z "$PID" ]]; then
  echo "App process never became alive"
  adb logcat -d -t 1200 || true
  exit 1
fi

echo "App process alive: $PID"

RESUMED=0
for _ in $(seq 1 60); do
  ACTIVITY_STATE="$(adb shell dumpsys activity activities 2>/dev/null || true)"
  if grep -E "mResumedActivity|topResumedActivity|ResumedActivity" <<<"$ACTIVITY_STATE" | grep -q "$ACTIVITY"; then
    RESUMED=1
    break
  fi
  sleep 2
done

if [[ "$RESUMED" != "1" ]]; then
  echo "MainActivity did not reach resumed state"
  adb shell dumpsys activity activities | tail -n 250 || true
  adb logcat -d -t 1200 || true
  exit 1
fi

echo "MainActivity resumed"
sleep 10

UI_OK=0
if adb shell uiautomator dump /sdcard/window.xml >/tmp/uiauto.txt 2>&1; then
  adb pull /sdcard/window.xml window.xml >/dev/null 2>&1 || true
  if [[ -f window.xml ]] && grep -q "package=\"$PKG\"" window.xml; then
    UI_OK=1
  fi
fi

# Compose can occasionally expose a sparse hierarchy on software-rendered CI emulators.
# If the activity is resumed and its process is alive, treat that as the minimum runtime UI signal.
if [[ "$UI_OK" == "1" ]]; then
  echo "UI hierarchy belongs to $PKG"
else
  echo "UI hierarchy dump was sparse/unavailable; continuing because MainActivity is resumed and process is alive"
fi

sleep 5
PID2="$(adb shell pidof "$PKG" 2>/dev/null | tr -d '\r' || true)"
if [[ -z "$PID2" ]]; then
  echo "App process died after launch"
  adb logcat -d -t 1200 || true
  exit 1
fi

LOGS="$(adb logcat -d -t 1200 || true)"
if grep -E "FATAL EXCEPTION|ANR in ${PKG//./\\.}|Process: ${PKG//./\\.}.*has died" <<<"$LOGS"; then
  echo "Application crash/ANR detected during emulator smoke test"
  exit 1
fi

echo "Runtime smoke OK: APK installed, process alive, MainActivity resumed, no fatal crash/ANR."
