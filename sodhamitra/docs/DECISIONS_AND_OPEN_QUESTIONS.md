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
