# Śodhamitra — Research Planner

An offline-first Android application for scholars: plan, organise and document research on your own phone.
Three sections — **PhD Research**, **Individual Researcher / Author**, **Editing / Translation Planner** — with an
outline builder, research notes and citations, a bibliography, a shared manuscript form, future editing and translation
lists, ten interface languages, a profile, and PDF export that is created on the device.

© Dr. Diddigi Vamsi Krishna

> **Version 0.2.0 (V2).** Approved maroon / ivory / light-green palette, three large Homepage cards ("PhD Research Plan", "Individual Researcher / Author",
> "Editing / Translation Planner"), a hamburger menu that opens from the right, the creator footer on the Hero page and Homepage, the approved biography
> verbatim, a read-only Profile view with Edit, "Add Title of Thesis" / "Add Title of Book", and explicit (long-press) editing in the planner lists.
> The package id and the database are unchanged, so it installs over 0.1.0 and keeps the data. What was decided where the specification was open is in
> `docs/DECISIONS_AND_OPEN_QUESTIONS.md` (section "V2"); what was tested is in `docs/IMPLEMENTATION_AND_TEST_REPORT.md`.

> **Where this lives.** The repository already held the earlier *Tribhāṣā* translation app (as `Tribhasa-0.6-source-r2.zip`).
> No Śodhamitra code existed, so this is the first implementation of Śodhamitra. The Tribhāṣā files were not touched.
> The Tribhāṣā logo supplied for Śodhamitra is used exactly as supplied (cropped, not redrawn).

## What is in this folder

| Path | What |
|---|---|
| `android/` | The complete Gradle / Android Studio project (Kotlin, Jetpack Compose, Material 3, Room). |
| `android/app/src/main/res/font/` | Bundled fonts: Ubuntu (Regular, Medium, Bold) and Noto Sans for Kannada, Devanagari (Sanskrit, Hindi), Telugu, Tamil, Bengali, Gujarati, Malayalam, Odia, plus a Latin-extension face for the dotted IAST letters. |
| `android/app/src/main/assets/licenses/` | The font licence texts (also shown inside the app: Settings → About App). |
| `android/app/src/main/res/values*/strings.xml` | The ten bundled interface languages (generated, see below). |
| `tools/i18n/` | The master translation tables and the generator that writes the ten `strings.xml` files and refuses to run if any string is missing. |
| `tools/ci/` | The emulator script, the PDF verifier and the update check (`baseline/` holds the 0.1.0 APK, `make_v1_database.py` makes a version-1 database) used by the GitHub Actions workflow. |
| `assets-original/` | The logo and the UPI screenshot exactly as supplied, kept for reference (not packaged). |
| `docs/` | Implementation and test report, decisions and open questions. |
| `../.github/workflows/sodhamitra.yml` | Builds the APK, runs the tests, then installs and launches the APK on an Android emulator. |

## Build the testing APK

```
cd android
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Needs JDK 17 or newer and the Android SDK (platform 36, build-tools 36.0.0; put its path in `android/local.properties` as `sdk.dir=...`).
The testing APK is a **debug** build (`debuggable=true`) signed with the **testing** key `android/testing.keystore`. That key is committed on
purpose and its password is in `app/build.gradle.kts`: it is public and is **not** the release key. A testing build can never update a release build.

## Release build (signed, not debuggable)

The release APK is signed with a **private release key that is not in this project, not in GitHub and not in the source ZIP**.

1. Keep the key file (for example `sodhamitra-release.jks`) in a private place **outside** the project folder.
2. Create the text file `android/release-signing.properties` (it is listed in `.gitignore`; never commit it):

   ```
   storeFile=/full/path/to/sodhamitra-release.jks
   storePassword=...
   keyAlias=sodhamitra-release
   keyPassword=...
   ```

   (Instead of the file, the environment variables `SODHAMITRA_RELEASE_STORE_FILE`, `SODHAMITRA_RELEASE_STORE_PASSWORD`,
   `SODHAMITRA_RELEASE_KEY_ALIAS`, `SODHAMITRA_RELEASE_KEY_PASSWORD` can be used.)
3. Build and verify:

   ```
   cd android
   ./gradlew :app:assembleRelease
   # → app/build/outputs/apk/release/app-release.apk
   apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
   ```

Without step 2 the build still succeeds but produces `app-release-unsigned.apk`, which Android refuses to install. Signing schemes used: v1, v2 and v3.

**The release key must be kept for ever.** Android only installs an update over an installed app if the update is signed with the same key. If the key is
lost, later versions can only be installed after uninstalling the old one (which deletes the app's data). Keep at least two private copies and never publish the key or its password.
A release APK cannot be installed over a testing build (different keys): uninstall the testing build first, after exporting what you need as PDF.

## Rebuilding from the source ZIP

Unzip `Sodhamitra_Source.zip`, open the folder `sodhamitra/android` and run the commands above. Everything needed to build and test is inside; the only things that are
deliberately missing are the private release key and its password.

## Run the tests

```
cd android
./gradlew :app:testDebugUnitTest                 # logic, translations, database, screens, PDF layout (JVM, Robolectric)
./gradlew :app:connectedDebugAndroidTest         # on a connected device / emulator: real SQLite, real PdfDocument, real screens
```

## Change a translation

Edit the tables in `tools/i18n/tr_*.py` (one line per string, ten languages per line) and run
`python3 tools/i18n/generate_strings.py`. Do not edit the generated `strings.xml` files by hand.

## Offline, privacy

* The app declares **no permissions at all** — in particular no `INTERNET` permission — so it cannot reach the network.
* No account, analytics, advertising, cloud sync, remote AI or paid service. Fonts, translations, logo and payment details
  are inside the APK.
* Research data is kept in a private on-device database. Android's cloud backup of the app's data is switched off.
* Feedback opens an email *draft* (recipient and subject filled in); nothing is ever sent without the user pressing Send
  in their own email app. PDFs are written to private cache and then saved or shared only when the user chooses.

## Structure (MVVM)

`data/` Room entities, DAO, repository · `domain/` field lists and validation (Stakeholder → Place rule), record ↔ form
mapping · `ui/` Compose screens, one navigation stack in `AppViewModel` · `pdf/` the PDF layout engine (A4, tables that flow
across pages, footer and page numbers on every page) · `i18n/` language store.

Third-party code: AndroidX (Compose, Room, Lifecycle, Activity, Core), Kotlin coroutines — all Apache-2.0.
Fonts: Ubuntu (Ubuntu Font Licence 1.0), Noto Sans family (SIL Open Font License 1.1).
