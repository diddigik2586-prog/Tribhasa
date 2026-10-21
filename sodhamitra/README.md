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

Needs JDK 17 or newer and the Android SDK (platform 36, build-tools 36.0.0). The debug APK is signed with a **testing** key
that is committed on purpose (`android/testing.keystore`), so every testing build — on a computer or from GitHub Actions —
is signed identically and a newer test build installs over an older one without losing data. It is **not** the Google Play
key; that key is made only after the project is declared FINAL and is never committed.

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
