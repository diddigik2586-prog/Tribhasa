# Śodhamitra — Research Planner · implementation and test report

Version **0.1.0 (testing build)** · built 20–21 October 2026 · package `org.tribhasa.sodhamitra` · minSdk 26 · targetSdk 36

This report separates four things: **verified on a real Android device (emulator)**, **verified by automated tests on a
computer**, **checked by looking at screenshots**, and **not done / not verifiable here**. Nothing is marked verified unless
it was actually run.

## 1. Deliverables

| Deliverable | Where |
|---|---|
| Complete source project (Gradle / Android Studio) | `sodhamitra/android/` in the repository (also `sodhamitra/dist/Sodhamitra-source.zip`) |
| Installable testing APK (debug build, testing key) | `sodhamitra/dist/Sodhamitra-0.1.0-testing.apk` (also a GitHub Actions artifact on every run) |
| Reproducible build and tests | `.github/workflows/sodhamitra.yml` |
| Decisions, assumptions and open questions | `sodhamitra/docs/DECISIONS_AND_OPEN_QUESTIONS.md` |

The APK is a **debug** build signed with a committed **testing** key. It is not a Play Store release and not the Play key.

## 2. What was verified on a real Android device

GitHub Actions run on an **Android 14 (API 34) x86_64 emulator with hardware acceleration** — see the workflow run for this
branch. This is an emulator, **not a physical phone**.

| Check | Result |
|---|---|
| The APK installs (`adb install`, package visible in `pm list packages`) | **Passed** |
| The app launches (cold start 1.4 s) and stays running; relaunch after force-stop | **Passed** |
| The app runs with **airplane mode on** (screenshot shows the airplane icon and the working Home screen) | **Passed** |
| The package requests **no `INTERNET` permission** (0 matches) | **Passed** |
| No crash in the system crash log during the run (0 `FATAL EXCEPTION`) | **Passed** |
| On-device tests (`DeviceTest`, `DeviceUiTest`: 4 tests, 0 failures): data survives closing and reopening the database; text of **4 MB (ASCII) and ≈10 MB (Kannada)** is stored and read back unchanged on Android's real SQLite; real PDFs are written; Stakeholder → Place rule refuses to save on the real screens | **Passed** |
| Real PDFs written by Android's own `PdfDocument` and checked afterwards with an independent tool: `editing-landscape.pdf` **5 pages, A4 landscape**; `phd-portrait.pdf` **40 pages, A4 portrait**; on **every page**: the exact footer lines "Śodhamitra — Research Planner" and "Created by © Dr. Diddigi Vamsi Krishna" and "Page i of N" | **Passed** |

## 3. Automated tests on a computer (JVM, Robolectric + Compose UI tests)

All run with the real application code and real bundled fonts (not mocks of the screens).

| Area | Tests | What they establish |
|---|---|---|
| Translation tables | `StringsTest` (7) | Ten languages bundled; every string present in every language; non-English strings are really in their script; approved English wording of all labels; "Script" = "ಲಿಪಿ" in Kannada; exact footer / copyright / feedback strings; the biography contains only the supplied facts and none of the excluded awards |
| Script and font selection | `ScriptRunsTest` (7) | Each text run is assigned the right bundled font (Ubuntu for English, the matching Noto face for each Indian script, a Latin-extension face for the dotted IAST letters Ubuntu lacks) |
| Forms and validation | `FormsTest` (9) | The manuscript form has exactly the 14 approved fields in order; the Book form of both lists has the 14 approved fields in order; only Title is mandatory; **Stakeholder filled → Place mandatory; cleared → optional again; title-only saves**; ISSN mandatory for articles; Scopus details / translation languages appear only when their box is ticked |
| Database | `RepositoryTest` (8), `TextChunksTest` (3), `FlowStressTest` (1) | Chapters, points and descriptions are linked but independent records; deleting a project/chapter/point removes exactly what it owns; descriptions of ≈3 MB round-trip; a source cited many times is stored once; editing updates in place; data survives closing and reopening the database file |
| Screens and workflows | `AppFlowTest` (23) | Home order (logo, Śodhamitra, Research Planner, ©Dr. Diddigi Vamsi Krishna, exactly three sections + Settings); Settings has exactly six items in order with the footer; Feedback opens a prefilled **draft** (SENDTO, address and subject) and never sends; "no email app" dialog with a copyable address; Support page shows the supplied details and copies the UPI ID; About pages show the copyright once; projects, outline, chapters, expandable points (one at a time, only own description), edit / cancel / delete with confirmation; bibliography categories with their own **+ Add**; ISSN rule; manuscript form identical in all three places; Stakeholder/Place rule on create and edit; independent Published / Unpublished / To-be-translated boxes; Book forms of Future Editing List and Future Trans List identical (incl. Translation Available reveal); planner tables; research notes and citations without duplicate sources; export with nothing to export makes no PDF; planner export is A4 landscape; language switching keeps user content untouched |
| All ten languages | `LanguagesTest` (10) | For every language: Home, Settings, About, Support, Future Editing List with user content in three scripts, the 14-field manuscript form in order, and the localised validation message; user content stays byte-for-byte as stored; **80 screenshots** written |
| Repeated launches / concurrent flows | `ListStressTest` (1), `FlowStressTest` (1), `CollectStressTest` (1) | 12 launches of a list screen with stored rows — every table shows its rows; 40 rounds of concurrently started database flows; 25 rounds of two flows collected by a screen. These were added after an intermittent "No records yet" display was found and fixed (see section 8) |
| PDF layout | `PdfLayoutTest` (7) | Portrait/landscape page sizes; long text and tables flow over many pages; a single row taller than a page splits across pages; **nothing is ever drawn into the reserved footer band**; footer present on every page; first-page profile header with photograph; works with an empty profile; empty selection → no document |

Final local run: **77 automated tests, 0 failed** (details in `TEST_RESULTS.md`).

## 4. Checked by looking at screenshots (not by an automated judge)

The ten-language screenshots produced by `LanguagesTest` and the PDF pages were looked at: scripts shape correctly with the
bundled fonts, nothing is clipped on a 411 dp wide phone layout, Latin words and Devanagari award names inside Kannada text use
their own fonts, tables and footer in the PDFs do not overlap. The emulator screenshot confirms the same on a real Android
rendering stack. **Native-speaker review of the wording is still outstanding** (see decisions document).

## 5. Implemented as specified (summary)

Offline-first native app (Kotlin, Compose, Material 3, Room, MVVM) · three main sections only · PhD Research (projects with
Research Title, long-press Edit / Delete / Export, Thesis Outline with chapters, points, expandable descriptions, Bibliography
with Books / MSS / Articles) · Individual Researcher / Author (Book Title projects, Book Outline, Research Notes with nested
References / Citations) · Editing / Translation Planner (Future Editing List: Manuscript + Book; Future Trans List: Book) ·
shared 14-field manuscript form · identical 14-field Book forms · Stakeholder/Place rule everywhere · PDF export, local, with
page numbers, exact footer, A4 portrait / landscape, profile header with photograph · Profile (all optional) · ten bundled
interface languages · Ubuntu for English, Noto Sans faces for the other scripts, licences included and readable in the app ·
Settings with the six items in order and the copyright footer · About App · About Dr. DVK · Support page with the supplied QR
code and bank details · Feedback draft · genuine logo · **no permissions, no network code**.

## 6. Not done, not verifiable here, or limited

* **Not tested on a physical phone** (only an emulator). Touch feel, real keyboards, real Indic input methods, photo picker,
  hand-written text, and the system "share / save" dialogs were not exercised on a device.
* **Profile photograph from the gallery** (photo picker) is implemented but could not be driven automatically; the
  photograph-in-PDF path is tested with a generated image.
* **Feedback with a real email app** was tested at the intent level only (recipient, subject, draft action) — no email app was
  installed on the emulator to complete a draft.
* **Translations** have not been reviewed by native speakers (see decisions document).
* **No monochrome themed launcher icon** (would require redrawing the logo, which is not allowed).
* **No backup / restore of research data** and no cloud backup by design; uninstalling deletes the data.
* **Release / Google Play preparation** deliberately not started (waits for the author's FINAL).
* Lint: 0 errors, a few advisory warnings (newer library versions, missing monochrome icon, KTX suggestions).
* Editing a description of many megabytes in the on-screen text box may be slow; storing and exporting is not limited.

## 7. Final local run

See `TEST_RESULTS.md` next to this file for the exact test counts of the final build, generated by the same commands as CI.

## 8. A defect found and fixed during testing

While running the all-languages tour, one list screen occasionally showed "No records yet." for the manuscripts table although
the data was stored and had reached the screen (the neighbouring Book table showed its row). Cause: the list screens read their
database state through delegated properties *inside* the `LazyColumn` content lambda, so the lambda never changed identity and
the list relied on inner change tracking; in addition each recomposition created a new database `Flow`. Fix: every screen now
creates each `Flow` once (`rememberFlow`) and reads the collected value into a plain local before building the list, so the list
is rebuilt whenever its data changes. After the fix 8 consecutive loops of the 10-language tour plus 12 repeated launches (≈ 90
list launches) passed with no failure, and both CI runs on the fixed code passed the device tests.
