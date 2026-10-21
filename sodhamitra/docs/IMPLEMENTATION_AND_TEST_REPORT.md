# Śodhamitra — Research Planner · implementation and test report (version 0.2.0, "V2")

Package `org.tribhasa.sodhamitra` · minSdk 26 · targetSdk 36 · versionName 0.2.0 / versionCode 2 · database schema version 1 (unchanged).

This report separates four things: **verified on an Android emulator by the GitHub Actions run**, **verified by automated tests on a computer**,
**checked by looking at screenshots**, and **not done / not verifiable here**. Nothing is marked verified unless it was actually run.

## 1. Deliverables

| Deliverable | Where |
|---|---|
| Installable testing APK (debug build, testing key) | `sodhamitra/dist/Sodhamitra-0.2.0-testing.apk` |
| Complete source project | `sodhamitra/android/` (also `sodhamitra/dist/Sodhamitra-source.zip`) |
| Reproducible build, tests, emulator checks | `.github/workflows/sodhamitra.yml`, `sodhamitra/tools/ci/` |
| Decisions, assumptions, open questions | `sodhamitra/docs/DECISIONS_AND_OPEN_QUESTIONS.md` (section "V2") |

The APK is a **debug** build signed with the committed **testing** key (the same key as 0.1.0, so it installs over 0.1.0 and keeps the data). It is not a Play release.

## 2. What the specification asked and what was done

| V2 section | Done | How it was checked |
|---|---|---|
| 3 Palette | Maroon `#800000`, Warm Ivory `#FFF6D8`, Very Light Green `#F0F7E9` everywhere; one scheme, no dark variant; old indigo overrides removed; PDF accents follow the palette | `ThemeAndHeroTest` (values + contrast ≥ 4.5 : 1 for 13 text/background pairs), `DarkModeKeepsPaletteTest`, pixel checks of the Homepage, screenshots |
| 4 Cards | Three large rectangular cards (16 dp corner, equal sizes, ivory arrow badge), exact labels, fully clickable | `HomeV2Test` (order, size, not a pill, colours by pixel, every card opens its destination), `NarrowScreenTest` (320 dp), screenshots in 10 languages, emulator screenshots |
| 5 Label | Homepage says "PhD Research Plan"; the opened section is still "PhD Research", route `Projects(PHD)` unchanged | `HomeV2Test` (checks the route object) |
| 6 Menu | Hamburger icon top right opens a right-side menu; closes with X, outside tap, Back; Homepage-only right-edge swipe | `HomeV2Test` (open, three ways to close, items navigate, no Settings entry, swipe opens, other swipes do not, swipe does nothing on 7 other screens); **emulator: a real system `input swipe` from the edge opened the menu on the Homepage and did nothing on another screen, in default, gesture-navigation and 3-button modes** |
| 7 Footer | "Created by Dr. Diddigi Vamsi Krishna" in a full-width maroon bar on the Hero page and Homepage; centred; clear of the navigation bar | `HomeV2Test`, `ThemeAndHeroTest` (bounds, centring, pixel colours), emulator `DeviceInsetsTest` (bar reaches the window bottom, text above the navigation bar, both navigation modes) |
| 8 Biography | Verbatim, centred, fixed (not translated) text | `StringsTest` + `InformationPagesTest` compare every paragraph with the supplied text character by character and check the alignment of each paragraph |
| 9 About App | Text byte-identical to version 0.1.0 in all ten languages; centred | `StringsTest.about_app_text_is_unchanged` (140 strings against a snapshot taken before V2), `InformationPagesTest` |
| 10/11 Alignment, Support | About Dr. DVK, About App, Support centred (title bars too); QR centred, square, never clipped; payment details and copy buttons unchanged | `InformationPagesTest`, `NarrowScreenTest`, emulator screenshots and bounds checks |
| 12/13 Wording | "+ Add Title of Thesis" (PhD) / "+ Add Title of Book" (Individual), also form title, empty state, hint, delete text | `AppFlowTest`, `HomeV2Test` |
| 14 Profile | Form → read-only details with Edit (pencil, top right) → form with values → Save / Cancel; Back asks before discarding; persists | `ProfileFlowTest` (7 scenarios incl. a new activity after "restart"), emulator screenshots |
| 15 Planner | No "Tap to edit" text; tap does nothing; long-press → Edit / Delete | `PlannerExplicitEditTest` (manuscript table, both book tables) |
| 16 Data safety | No schema change; package id unchanged; no new permission | Room schema file unchanged; `package-info.txt` of the emulator run (no `INTERNET`); **update check**, see 3 |

## 3. Verified on an Android emulator (GitHub Actions, Android 14 x86_64)

* The APK installs, launches, restarts and runs with airplane mode on; no `INTERNET` permission; no crash in the crash log.
* On-device tests (real SQLite, real `PdfDocument`, real screens): all pass; the real PDFs are checked with an independent tool (5 landscape and 40 portrait A4 pages, footer and page numbers on every page).
* **Window insets on the real system** (`DeviceInsetsTest`, run with gesture navigation and with the 3-button bar): status bar, hamburger icon, cards, footer, menu, forms with their Save/Cancel bar, Profile, About Dr. DVK and Support are inside the system bars; the footer bar reaches the bottom of the window while its text stays above the navigation bar; the menu panel is maroon under both system bars.
* **Real edge swipe** (a system-level `input swipe` from 2 px inside the right edge): opens the menu on the Homepage, does not open anything on another screen.
* **Update check:** the 0.1.0 APK is installed, given a version-1 database (projects, chapter, point with description, manuscript, planner book, profile), the 0.2.0 APK is installed over it, and `UpgradeKeepsDataTest` finds every record and shows it in the new screens.
  This check is the step "UPDATE CHECK" of `tools/ci/emulator_checks.sh`; it runs on every push, and its result for the delivered build is given in the delivery message.
* Screenshots of the real device are attached to the CI run (artifact `Sodhamitra-emulator-evidence`).

## 4. Automated tests on a computer (JVM, Robolectric + Compose UI tests)

All run with the real application code and the real bundled fonts. 113 tests, 0 failed (details in `TEST_RESULTS.md`). The ten-language screen tour,
the database, forms, PDF layout and stress tests of 0.1.0 all still pass (one assertion about the Homepage and the Settings location was updated for V2; none was weakened).

## 5. Found by the independent review and fixed

The change set was reviewed by eight independent read-only reviewers with a second skeptical check of each finding. Real findings that were fixed: the menu panel hid the
light status/navigation icons (also seen on the real emulator); old dark-mode window resources; the Profile form ignored the on-screen keyboard; the Hero page in landscape;
the dimmed menu area had no accessibility action; the edge zone ignored right-hand system bars; Odia / Bengali / Gujarati / Kannada wording consistency of the new labels.
Findings that were refuted (already fixed, or preferences) are not listed. Some second-opinion checks could not run because of a usage limit during the review; the
findings of those reviewers were read and judged directly.

## 6. Not done, not verifiable here, or limited

* **No physical phone**: touch feel, real keyboards and Indic input methods, the photo picker and system dialogs were not exercised on a device. The swipe was exercised on an emulator through Android's real input pipeline, not with a finger.
* **Gesture navigation:** the edge swipe works in the 200 dp band that the app reserves at the right edge; elsewhere along the edge the system may treat it as "Back". The hamburger icon always works.
* **Translations** of the new labels (nine languages) have not been reviewed by native speakers.
* The biography is shown in English in every language (approved text, not translated).
* **Profile photograph from the gallery** is implemented but could not be driven automatically; **feedback** with a real email app was tested at the intent level only.
* **Release / Google Play preparation** deliberately not started (waits for the author's FINAL).
