# Decisions made, assumptions, and questions for the author

Everything here is something the specification did not fix. Nothing was invented as a *fact* about the author; where a
choice had to be made to build a working app it is listed so it can be confirmed or changed.

## 1. The project that "already existed"

The repository contained only the earlier **Tribhāṣā** translation app (`.github/workflows/Tribhasa-0.6-source-r2.zip`).
It has no Śodhamitra code, no manuscript form, no database and no biography text. It was left untouched, and Śodhamitra was
built as a new project in `sodhamitra/`. The only thing carried over is the **logo**, and that comes from the image supplied
with the request, not from the vector approximation inside the old zip (its colours differ slightly from the supplied artwork).

## 2. Assets used exactly as supplied

* **Logo** — the supplied PNG, cropped to the rounded square (the white canvas margin removed and made transparent). Not
  redrawn. The launcher icon is the same artwork on the logo's own indigo (#1D1673) background. *No monochrome (themed)
  launcher icon is provided: that would need a redrawn silhouette, which the specification forbids.*
* **UPI QR** — cut out of the supplied Kotak 811 screenshot with its white quiet zone. It was decoded to check it
  (`upi://pay?pa=8970500450@kotakbank&pn=DIDDIGI VAMSI KRISHNA`), and that matches the UPI ID you supplied.
* **Bank details** — account number 1348857820, IFSC KKBK0000425, home branch MYSORE, UPI ID 8970500450@kotakbank.
  Two entries were *derived*, please confirm: **account holder "DIDDIGI VAMSI KRISHNA"** (taken from the name printed on the
  supplied QR screen) and **bank name "Kotak Mahindra Bank"** (from the IFSC prefix KKBK and the Kotak QR screen).
* **Privacy note:** these details are now part of the source files in this GitHub repository. If the repository is public,
  the account number is public. They are only shown in the app on the Support page, as requested.

## 3. Fields the specification did not list (please confirm or correct)

* **Bibliography / citation Book:** Title (mandatory), Author, Editor, Edition, Publisher, Place of Publication, Year.
* **Bibliography / citation Article:** Title (mandatory), Author(s), Journal / Periodical, Volume / Issue, Year, Pages,
  **ISSN (mandatory)**, Indexed in Scopus (tick; "Scopus details" appears only when ticked), Impact Factor.
  ISSN is only required to be non-empty; its format is not enforced.
* **Citation:** the source (new, or chosen from the project's stored sources) + Page / Folio + Contextual note. The same
  source is stored once however often it is cited (an identical title/author/edition/year is reused automatically).
* **Material "Other":** choosing *Other* shows a small "Specify the other material" box. It belongs to field 10, so the
  form still has exactly fourteen fields in the approved order.
* **"Edited / Not Edited"** is a two-choice selector (tap the chosen one again to clear it).
* **Export from the Editing / Translation Planner:** the specification requires landscape PDFs from this section but does
  not describe the export screen. An **Export** button was added to the Future Editing List (choose Manuscript and/or Book)
  and to the Future Trans List (Book).
* **Future Trans List** has only the Book heading, as specified.
* **PDF layout:** portrait PDFs show each manuscript as its own small *field | value* table (fourteen fields cannot be read in
  one portrait row); landscape PDFs show the Future Editing List manuscripts as one wide table. Books and articles are
  ordinary tables everywhere. The profile's Sex is included in the PDF header because the specification asks for "available
  profile details".
* **Navigation inside lists:** tap opens/edits, long-press opens Edit / Delete (/ Export for projects). Projects and chapters
  are created in a full-screen form rather than a pop-up (more reliable with on-screen keyboards).
* **No re-ordering** of chapters/points and no search — not requested.

## 4. Translations and the biography

* All ten languages were written by the assistant that built the app. They are consistent and complete (the build fails if
  any string is missing) and were checked on screen for script and layout, **but they have not been reviewed by a native
  speaker or scholar.** Particular care is advised for Sanskrit and for the terms **Stakeholder** (ಪಾಲುದಾರ, हितधारकः, భాగస్వామి,
  हितधारक, பங்குதாரர், অংশীদার, હિતધારક, പങ്കാളി, ହିତାଧିକାରୀ) and **Completeness and Condition / Catalogue Reference / Remarks**.
  Change any of them in `tools/i18n/tr_*.py` and regenerate.
* **About Dr. DVK** contains only what you supplied: Sanskrit scholar, manuscriptologist and researcher; M.A. and Ph.D. (the
  subjects/universities were not supplied, so none are stated); Research Assistant at the Oriental Research Institute,
  University of Mysore, coordinating several major projects; Founding Director of Shimshumara Shodha Pratishthanam, Mysuru;
  Granthapuraskāra (ग्रन्थपुरस्कार), 2020, Karnataka Samskrit University. The Veda Peetha Prashasti (2024) and IRO 2025
  recognitions are not mentioned. Institution names stay in Latin script in every language (official native-script spellings
  were not supplied). The sentence on research interests is deliberately general ("Sanskrit studies and manuscriptology").
  **Send a verified list of research interests, publications and awards if you want them added.**

## 5. Data, backup and limits

* Data lives in a private on-device database. **There is no export/import of the data itself** (not requested). Uninstalling
  the app, or clearing its storage, deletes the data. Android's *cloud* backup is switched off on purpose (no data may leave
  the device); direct phone-to-phone transfer during device setup is still allowed. A local backup/restore file could be
  added if you approve it.
* **Unlimited text:** the app imposes no limit. Descriptions are stored in linked chunk records (about 200,000 characters
  each) because Android cannot read a single database row larger than about 2 MB. Editing a description of many megabytes in
  the on-screen text box can be slow on a phone; storing, showing and exporting it is not limited.
* Typed text that has not been saved is lost if Android closes the app in the background.

## 6. Identity of the app

* **Application id `org.tribhasa.sodhamitra`** is a placeholder chosen to sit beside `org.tribhasa.app`. It cannot be changed
  after publication on Google Play, so confirm it (or give another) *before* declaring the project FINAL.
* The testing APK is signed with a committed **testing** key (see README). The Play release key will be created after FINAL.
* Display name (launcher): "Śodhamitra — Research Planner".

## 7. Left for after FINAL (not started, as instructed)

Release signing, AAB, store listing, screenshots for the store, privacy policy, Data-safety and content-rating forms, target
API / testing-track requirements. One preparation is already in place: the Android bundle is configured not to split by
language, so the in-app language choice keeps working on a Play install.

---

# V2 (version 0.2.0): decisions, assumptions and questions

Everything below is something the V2 specification left open, or a place where two instructions met. The V2 specification wins
over earlier instructions.

## What was changed, what was deliberately not

* The existing project was changed in place. The **package id** (`org.tribhasa.sodhamitra`), the **database** (Room schema version 1,
  no migration, no schema change) and the **navigation routes / internal section names** are unchanged. A V1 database is opened as it is.
* Only `versionName 0.2.0` / `versionCode 2` were raised so that the new build installs over the old one (same package, same testing key).
* No printing or PDF-export feature was added. The existing PDF export is unchanged except for its accent colours (below).

## Colours (section 3)

* Maroon `#800000` (primary, bars, buttons, Homepage cards, creator footer, headings), Warm Ivory `#FFF6D8` (cards, dialogs, menus, text on
  maroon), Very Light Green `#F0F7E9` (every screen background). Blush tints derived from maroon are used for selected chips and table headers.
* The app has **one** colour scheme: the V1 dark scheme was removed, system dark mode does not re-colour it (`forceDarkAllowed=false`), and the
  old dark/indigo resource overrides were deleted. The supplied **logo** keeps its own colours (it must not be redrawn).
* **PDF**: headings and table-header tints in exported PDFs now use maroon / its tint / ivory instead of the old indigo and lavender; the paper stays white.
  This is the only change to the PDF output. If you prefer the old PDF colours, say so.
* Contrast ratios of every text/background pair are checked by an automated test (all at least 4.5 : 1).

## Homepage (sections 4–7)

* The three cards are large rounded **rectangles** (16 dp corner, never a pill), maroon with ivory text and a round ivory arrow badge.
* The Homepage label is **"PhD Research Plan"**; the screen it opens is still titled "PhD Research" (internal name unchanged); the label is also used in the menu.
  Translations of "PhD Research Plan" in the nine other languages are new and unreviewed.
* The old Settings button is replaced by a **hamburger icon at the top right** of the (maroon) Homepage bar; it opens a **right-side menu**.
  The menu lists the three sections, Profile, Language, About App, About Dr. DVK, Support Śodhamitra and Feedback. **There is no "Settings" entry** (removed at the
  author's request): the six options that the Settings page held are in the menu itself, so the page is no longer reachable (its code was left in place).
* The menu closes with its X, a tap on the dimmed area, or Back. A swipe that starts within 28 dp of the right edge and moves left opens it **on the Homepage only**
  (the handler is not composed on any other screen). It only *observes* touches until the gesture is recognised, so scrolling, buttons and text fields are unaffected.
* **Limit to know:** with Android's *gesture navigation* the system reserves the screen edges for "Back". The app asks Android to leave a 200 dp band at mid-height on the
  right edge to the app (`systemGestureExclusion`); outside that band a swipe from the very edge may be taken by the system. The hamburger icon always works.
* **Creator footer:** "Created by Dr. Diddigi Vamsi Krishna" in a full-width maroon bar on the Hero (splash) page and the Homepage only. *Assumption:* "Hero page" is the
  launch (splash) screen. The existing line "©Dr. Diddigi Vamsi Krishna" under the brand block was **kept** (V2 says to preserve the existing creator attribution), so the
  Homepage now shows both. Tell me if you want the © line removed from those two pages.
* The maroon bar also sits behind the status bar on every screen, so the status-bar icons are always light; the menu panel is maroon where it runs under the
  status and navigation bars so those icons stay readable.

## Information pages (sections 8–11)

* **About Dr. DVK** shows the approved biography exactly as supplied (heading, roles line, five paragraphs, final © line), centred. It is **not translated**:
  the approved text is English and a translation would be a paraphrase, so it reads the same in all ten interface languages. If you want approved translations, send them.
  The earlier drafted biography (and its translations) was removed.
* **About App**: not a single character of the approved text was changed (an automated test compares all ten languages with the V1 text); only the alignment changed.
  The licence texts (long legal documents) keep their conventional left alignment; their file-name headings are centred.
* **Support**: heading and text centred; the QR image is centred, square and scales down on narrow phones. While looking at it I found two thin dark/crimson
  lines at its top and bottom edge: they were slivers of the neighbouring screen that were cut out together with the QR code in V1. They were whitened; the QR itself is
  pixel-identical and decodes to the same payload (`upi://pay?pa=8970500450@kotakbank&pn=DIDDIGI VAMSI KRISHNA`). Payment details and buttons are unchanged.

## Wording (sections 12–13), Profile (14), planner (15)

* Button **"+ Add Title of Thesis"** (PhD) and **"+ Add Title of Book"** (Individual Researcher / Author); the "+" of the old button was kept. The form titles, empty-state text,
  long-press hint and delete confirmation of those two sections say "thesis title" / "book title" accordingly. Nothing else was renamed.
* **Profile**: no saved profile → the editable form; saved → read-only details (every label, a dash for empty values) with **Edit** (pencil) at the top right.
  A profile counts as saved when its row exists, which also covers V1 profiles. Edit → same form with the saved values; Save → back to the details; Cancel → discards;
  Back or the top-bar arrow with unsaved edits asks "Discard changes?" first. A newly chosen photograph is only kept on Save (V1 stored it at once).
* **Editing Plan / Translation Plan** = the existing *Future Editing List* and *Future Trans List* (their names were not changed). A tap on a row does nothing;
  long-press shows Edit / Delete; the helper line now reads "Swipe sideways to see all columns. Long-press a row to edit or delete it." (the "Tap a row to edit it" part is gone).
  The list screens of the other sections keep their tap behaviour.

## Questions for you

1. Keep the "©Dr. Diddigi Vamsi Krishna" line under the logo on the Hero page and Homepage in addition to the new footer? (kept for now)
2. Is the PDF colour change (maroon headings instead of indigo) wanted? (done for consistency)
3. Should the biography get approved translations in the other nine languages? (English only for now)
4. The nine non-English translations of the new labels were written by the assistant and checked by independent review, but not by a native speaker.
