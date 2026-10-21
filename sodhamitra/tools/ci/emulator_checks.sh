#!/usr/bin/env bash
# Runs INSIDE a booted Android emulator (see .github/workflows/sodhamitra.yml). It installs the real APK, launches it,
# restarts it, runs it with airplane mode on, runs the instrumented tests on the device, and collects screenshots and the
# PDFs that the app itself produced on the device. Nothing here is simulated.
set -uo pipefail
cd "$(dirname "$0")/../../android"
OUT="$(cd .. && pwd)/ci-out"; mkdir -p "$OUT/pdfs"
PKG=org.tribhasa.sodhamitra
fail=0
note() { echo "== $*"; }

note "build APKs"
./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest || exit 1
APK=app/build/outputs/apk/debug/app-debug.apk
TESTAPK=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

note "device"
adb shell getprop ro.build.version.release | tee "$OUT/android-version.txt"
adb shell getprop ro.product.cpu.abi | tee -a "$OUT/android-version.txt"

# A slow emulator can show a "Pixel Launcher isn't responding" dialog over the app. That dialog is not part of the app, but it covers the
# screenshots and swallows touches, so system error dialogs are switched off for the test run.
adb shell settings put global hide_error_dialogs 1 || true
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null 2>&1 || true

note "install the debug APK"
adb install -r "$APK" || exit 1
adb shell pm list packages | grep -q "$PKG" && echo "INSTALLED: $PKG" | tee "$OUT/install.txt" || { echo "NOT INSTALLED"; exit 1; }
adb shell dumpsys package "$PKG" | grep -E "versionName|versionCode|targetSdk|minSdk|android.permission" > "$OUT/package-info.txt"
echo "--- permissions requested (INTERNET must not be listed):"; grep -c "android.permission.INTERNET" "$OUT/package-info.txt" || true

note "launch"
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity" | tee "$OUT/launch.txt"
sleep 8
adb shell pidof "$PKG" | tee "$OUT/pid.txt" || { echo "PROCESS NOT RUNNING after launch"; fail=1; }
adb exec-out screencap -p > "$OUT/01-launch-home.png"

note "restart (force-stop, relaunch)"
adb shell am force-stop "$PKG"; sleep 2
adb shell am start -W -n "$PKG/.MainActivity" > "$OUT/relaunch.txt"; sleep 6
adb shell pidof "$PKG" || { echo "PROCESS NOT RUNNING after relaunch"; fail=1; }
adb exec-out screencap -p > "$OUT/02-relaunch-home.png"

note "airplane mode on: the app must still work"
adb shell cmd connectivity airplane-mode enable || adb shell settings put global airplane_mode_on 1
sleep 3
adb shell am force-stop "$PKG"; adb shell am start -W -n "$PKG/.MainActivity" > "$OUT/offline-launch.txt"; sleep 6
adb shell pidof "$PKG" || { echo "PROCESS NOT RUNNING offline"; fail=1; }
adb exec-out screencap -p > "$OUT/03-offline-home.png"
adb shell dumpsys connectivity | grep -i -m3 "airplane\|NetworkAgentInfo" > "$OUT/offline-state.txt" || true

note "instrumented tests on the device (real SQLite, real PdfDocument)"
adb install -r -t "$TESTAPK" || exit 1
adb shell am instrument -w -r "$PKG.test/androidx.test.runner.AndroidJUnitRunner" | tee "$OUT/instrumentation.txt"
grep -q "FAILURES!!!" "$OUT/instrumentation.txt" && fail=1
grep -E "^OK \(" "$OUT/instrumentation.txt" || fail=1

note "V2: window insets, menu, real edge swipe and screenshots - once with gesture navigation, once with the 3-button bar"
for MODE in gestural threebutton; do
  note "navigation mode: $MODE"
  adb shell cmd overlay enable-exclusive --category "com.android.internal.systemui.navbar.$MODE" | tee -a "$OUT/navmode-$MODE.txt" || true
  sleep 4
  adb shell am force-stop "$PKG"
  adb shell am instrument -w -r -e shotPrefix "$MODE" -e class org.tribhasa.sodhamitra.DeviceInsetsTest "$PKG.test/androidx.test.runner.AndroidJUnitRunner" | tee "$OUT/insets-$MODE.txt"
  grep -q "FAILURES!!!" "$OUT/insets-$MODE.txt" && fail=1
  grep -E "^OK \(" "$OUT/insets-$MODE.txt" || fail=1
  grep -E "INSETS\[" "$OUT/insets-$MODE.txt" | head -20
done
mkdir -p "$OUT/shots"; adb pull "/sdcard/Android/data/$PKG/files/test-shots/." "$OUT/shots/" || true
ls -la "$OUT/shots" || true

note "collect the PDFs written by the app on the device"
adb pull "/sdcard/Android/data/$PKG/files/test-pdfs/." "$OUT/pdfs/" || fail=1
ls -la "$OUT/pdfs"
python3 -m pip install -q pypdf
python3 ../tools/ci/verify_pdfs.py "$OUT/pdfs" | tee "$OUT/pdf-verification.txt" || fail=1
for f in "$OUT"/pdfs/*.pdf; do pdftoppm -r 70 -png -f 1 -l 3 "$f" "${f%.pdf}"; done

note "logcat"
adb logcat -d -b crash > "$OUT/logcat-crash.txt"; adb logcat -d -t 500 > "$OUT/logcat-tail.txt"
grep -c "FATAL EXCEPTION" "$OUT/logcat-crash.txt" "$OUT/logcat-tail.txt" || true

adb shell cmd connectivity airplane-mode disable || true
echo "RESULT: fail=$fail"
exit $fail
