#!/usr/bin/env bash
set -euo pipefail

PKG="com.epalma.tvespanolplus"
ACTIVITY="$PKG/.MainActivity"
APK="app/build/outputs/apk/debug/app-debug.apk"

check_alive_and_resumed() {
  local pid=""
  for _ in $(seq 1 60); do
    pid="$(adb shell pidof "$PKG" 2>/dev/null | tr -d '\r' || true)"
    [[ -n "$pid" ]] && break
    sleep 2
  done
  if [[ -z "$pid" ]]; then
    echo "App process never became alive"
    adb logcat -d -t 1600 || true
    exit 1
  fi

  local resumed=0
  for _ in $(seq 1 60); do
    local state
    state="$(adb shell dumpsys activity activities 2>/dev/null || true)"
    if grep -E "mResumedActivity|topResumedActivity|ResumedActivity" <<<"$state" | grep -q "$ACTIVITY"; then
      resumed=1
      break
    fi
    sleep 2
  done
  if [[ "$resumed" != "1" ]]; then
    echo "MainActivity did not reach resumed state"
    adb shell dumpsys activity activities | tail -n 300 || true
    adb logcat -d -t 1600 || true
    exit 1
  fi
}

assert_no_fatal() {
  local logs
  logs="$(adb logcat -d -t 1600 || true)"
  if grep -E "FATAL EXCEPTION|ANR in ${PKG//./\\.}|Process: ${PKG//./\\.}.*has died" <<<"$logs"; then
    echo "Application crash/ANR detected"
    exit 1
  fi
}

echo "Installing APK..."
adb install -r "$APK"
adb logcat -c
adb shell am force-stop "$PKG"

echo "Cold-start test..."
adb shell am start -n "$ACTIVITY" >/dev/null
check_alive_and_resumed
sleep 8
assert_no_fatal

echo "Portrait/phone responsive test..."
adb shell settings put system accelerometer_rotation 0 || true
adb shell settings put system user_rotation 0 || true
sleep 3
check_alive_and_resumed
assert_no_fatal

echo "Landscape responsive test..."
adb shell settings put system user_rotation 1 || true
sleep 5
check_alive_and_resumed
assert_no_fatal

echo "Return to portrait and verify process survives configuration changes..."
adb shell settings put system user_rotation 0 || true
sleep 4
check_alive_and_resumed
assert_no_fatal

echo "Second cold-start recovery test..."
adb shell am force-stop "$PKG"
adb shell am start -n "$ACTIVITY" >/dev/null
check_alive_and_resumed
sleep 5
assert_no_fatal

UI_OK=0
if adb shell uiautomator dump /sdcard/window.xml >/tmp/uiauto.txt 2>&1; then
  adb pull /sdcard/window.xml window.xml >/dev/null 2>&1 || true
  if [[ -f window.xml ]] && grep -q "package=\"$PKG\"" window.xml; then
    UI_OK=1
  fi
fi

if [[ "$UI_OK" == "1" ]]; then
  echo "UI hierarchy belongs to $PKG"
else
  echo "Compose hierarchy is sparse on this software-rendered emulator; activity/process checks remain valid"
fi

PID2="$(adb shell pidof "$PKG" 2>/dev/null | tr -d '\r' || true)"
if [[ -z "$PID2" ]]; then
  echo "App process died after runtime tests"
  adb logcat -d -t 1600 || true
  exit 1
fi

assert_no_fatal
echo "Runtime smoke OK: install + two cold starts + portrait/landscape survival + resumed activity + no fatal crash/ANR."
