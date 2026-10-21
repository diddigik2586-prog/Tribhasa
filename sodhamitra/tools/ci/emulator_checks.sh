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

note "RELEASE APK smoke test: install the signed release APK that is committed in dist/, start it, check that it is not debuggable"
REL=../dist/Sodhamitra_Release.apk
if [ -f "$REL" ]; then
  adb uninstall "$PKG" > /dev/null 2>&1 || true
  if adb install "$REL"; then
    adb logcat -c
    adb shell am start -W -n "$PKG/.MainActivity" | tee "$OUT/release-launch.txt"; sleep 8
    adb shell pidof "$PKG" | tee "$OUT/release-pid.txt" || { echo "RELEASE APK IS NOT RUNNING"; fail=1; }
    adb exec-out screencap -p > "$OUT/04-release-home.png"
    adb shell dumpsys package "$PKG" | grep -E "versionName|versionCode|pkgFlags|flags=" | head -5 | tee "$OUT/release-package-info.txt"
    grep -q "DEBUGGABLE" "$OUT/release-package-info.txt" && { echo "THE RELEASE APK IS DEBUGGABLE"; fail=1; }
    adb shell dumpsys package "$PKG" | grep -c "android.permission.INTERNET" | tee "$OUT/release-internet-permission-count.txt" || true
    adb logcat -d -b crash | grep -c "FATAL EXCEPTION" | tee "$OUT/release-crash-count.txt" || true
    adb shell am force-stop "$PKG"
  else
    echo "THE RELEASE APK DOES NOT INSTALL"; fail=1
  fi
  adb uninstall "$PKG" > /dev/null 2>&1 || true
  adb install -r "$APK" || exit 1
else
  echo "no release APK in dist/ (skipped)"
fi

note "UPDATE CHECK: install the previous version (0.1.0), give it data, install this build over it, check the data"
BASE=../tools/ci/baseline/Sodhamitra-0.1.0-testing.apk
adb uninstall "$PKG" > /dev/null 2>&1 || true
if adb install "$BASE"; then
  adb shell am start -W -n "$PKG/.MainActivity" > /dev/null; sleep 6; adb shell am force-stop "$PKG"
  python3 ../tools/ci/make_v1_database.py app/schemas/org.tribhasa.sodhamitra.data.AppDatabase/1.json "$OUT/v1-seed.db" || fail=1
  adb push "$OUT/v1-seed.db" /data/local/tmp/v1-seed.db
  adb shell "run-as $PKG mkdir -p databases; run-as $PKG rm -f databases/sodhamitra.db databases/sodhamitra.db-wal databases/sodhamitra.db-shm; run-as $PKG cp /data/local/tmp/v1-seed.db databases/sodhamitra.db; run-as $PKG ls -la databases" | tee "$OUT/upgrade-v1-files.txt"
  adb install -r "$APK" || fail=1                       # the new build over the old one: same package name, same signing key
  adb shell dumpsys package "$PKG" | grep -E "versionName|versionCode" | head -3 | tee "$OUT/upgrade-version.txt"
  adb install -r -t "$TESTAPK" || fail=1
  adb shell am instrument -w -r -e class org.tribhasa.sodhamitra.UpgradeKeepsDataTest "$PKG.test/androidx.test.runner.AndroidJUnitRunner" | tee "$OUT/upgrade.txt"
  grep -q "FAILURES!!!" "$OUT/upgrade.txt" && fail=1
  grep -E "^OK \(" "$OUT/upgrade.txt" || fail=1
else
  echo "could not install the baseline APK"; fail=1
fi
adb uninstall "$PKG" > /dev/null 2>&1 || true
adb install -r "$APK" || exit 1
adb install -r -t "$TESTAPK" || exit 1
adb shell settings put global hide_error_dialogs 1 || true

note "instrumented tests on the device (real SQLite, real PdfDocument)"
adb install -r -t "$TESTAPK" || exit 1
# (UpgradeKeepsDataTest needs the records of the update check and has its own run above)
adb shell am instrument -w -r -e notClass org.tribhasa.sodhamitra.UpgradeKeepsDataTest "$PKG.test/androidx.test.runner.AndroidJUnitRunner" | tee "$OUT/instrumentation.txt"
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
  adb shell cmd overlay list | grep -i "navbar" | tee "$OUT/overlays-$MODE.txt" || true
done
mkdir -p "$OUT/shots"; adb pull "/sdcard/Android/data/$PKG/files/test-shots/." "$OUT/shots/" || true
ls -la "$OUT/shots" || true
for f in "$OUT"/shots/*-insets.log; do echo "--- $f"; cat "$f"; done

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
