#!/usr/bin/env bash
# Collects the two release deliverables into sodhamitra/dist/:
#   Sodhamitra_Release.apk   the signed RELEASE APK  (build it first with the private release key:  cd android && ./gradlew :app:assembleRelease)
#   Sodhamitra_Source.zip    the complete source project as committed in git: no keys, no passwords, no build output, no dist/
# The private release key is NEVER part of either file (see android/release-signing.properties, which is git-ignored).
set -euo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
repo="$(cd "$here/.." && pwd)"
sdk="${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' "$here/android/local.properties" 2>/dev/null)}"
apksigner="$(ls -d "$sdk"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)"
rel="$here/android/app/build/outputs/apk/release/app-release.apk"
[ -f "$rel" ] || { echo "No signed release APK: run './gradlew :app:assembleRelease' with the release key configured first (an unsigned app-release-unsigned.apk is not installable)."; exit 1; }
"$apksigner" verify --verbose --print-certs "$rel" | grep -E "^Verifies|Verified using v[123] .*true|certificate SHA-256" || { echo "the release APK does not verify"; exit 1; }
"$apksigner" verify "$rel" >/dev/null
mkdir -p "$here/dist"
cp "$rel" "$here/dist/Sodhamitra_Release.apk"

readme="$(mktemp)"
cat > "$readme" <<'TXT'
Śodhamitra — Research Planner : complete source project
=======================================================
Open  sodhamitra/README.md  first. In short:

  Needs: JDK 17 or newer, the Android SDK (platform 36, build-tools 36.0.0), internet access for the first Gradle run.
  Testing build :  cd sodhamitra/android && ./gradlew :app:assembleDebug
                   -> app/build/outputs/apk/debug/app-debug.apk   (signed with the PUBLIC testing key android/testing.keystore)
  Release build :  needs the PRIVATE release key, which is not in this archive. Put it outside the project and describe it in
                   sodhamitra/android/release-signing.properties (see README.md, "Release build"), then
                   ./gradlew :app:assembleRelease
                   -> app/build/outputs/apk/release/app-release.apk  (without the key the result is an UNSIGNED, uninstallable APK)
  Tests         :  ./gradlew :app:testDebugUnitTest

This archive contains no private signing key, no password and no build output.
TXT
(cd "$repo" && git archive --format=zip --prefix=Sodhamitra_Source/ --add-file="$readme" -o "$here/dist/Sodhamitra_Source.zip" HEAD \
  sodhamitra .github/workflows/sodhamitra.yml ':(exclude)sodhamitra/dist' ':(exclude)sodhamitra/tools/ci/baseline/*.apk')
# the archive carries the file under the temporary name: put it in place as README_FIRST.txt
python3 - "$here/dist/Sodhamitra_Source.zip" "$(basename "$readme")" <<'PY'
import sys, zipfile, shutil, os
src, tmpname = sys.argv[1], sys.argv[2]
out = src + ".tmp"
with zipfile.ZipFile(src) as zin, zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename == "Sodhamitra_Source/" + tmpname: item.filename = "Sodhamitra_Source/README_FIRST.txt"
        zout.writestr(item, data)
shutil.move(out, src)
PY
rm -f "$readme"
# the source ZIP must never contain key material
if unzip -Z1 "$here/dist/Sodhamitra_Source.zip" | grep -Ei '\.jks$|\.p12$|\.pfx$|release-signing|RELEASE_KEY|keystore\.properties|\.idsig$'; then
  echo "KEY MATERIAL FOUND IN THE SOURCE ZIP - removed"; rm -f "$here/dist/Sodhamitra_Source.zip"; exit 1
fi
( cd "$here/dist" && sha256sum Sodhamitra_Release.apk Sodhamitra_Source.zip Sodhamitra-0.2.0-testing.apk | tee SHA256SUMS.txt )
