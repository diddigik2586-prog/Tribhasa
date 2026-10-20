#!/usr/bin/env bash
# Collects the two downloadable deliverables into sodhamitra/dist/:
#   Sodhamitra-0.1.0-testing.apk   the installable testing APK (debug build, signed with the committed TESTING key)
#   Sodhamitra-source.zip          the complete source project as committed in git (without dist/ itself)
# Run after:  cd android && ./gradlew :app:assembleDebug
set -euo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
repo="$(cd "$here/.." && pwd)"
ver="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' "$here/android/app/build.gradle.kts" | head -1)"
mkdir -p "$here/dist"
cp "$here/android/app/build/outputs/apk/debug/app-debug.apk" "$here/dist/Sodhamitra-$ver-testing.apk"
(cd "$repo" && git archive --format=zip --prefix=Sodhamitra-source/ -o "$here/dist/Sodhamitra-source.zip" HEAD sodhamitra ':(exclude)sodhamitra/dist')
( cd "$here/dist" && sha256sum Sodhamitra-"$ver"-testing.apk Sodhamitra-source.zip | tee SHA256SUMS.txt )
