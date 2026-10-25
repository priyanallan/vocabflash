# Research: Availability & Licensing of Official German CEFR Vocabulary Lists

_Compiled 2026-10-05 for vocab-flash. Counts marked "~N (parsed)" are approximations from parsing the PDF text; published lists include multi-word entries and inflected forms, so exact numbers vary._

## 1. Official exam wordlists

### Goethe-Institut "Wortliste"
Published free as PDF, one per level, at `goethe.de/pro/relaunch/prf/en/Goethe-Zertifikat_<level>_Wortliste.pdf`:

| Level | URL | Approx. entries | Copyright notice in PDF |
|-------|-----|-----------------|-------------------------|
| A1 (Fit in Deutsch 1) | .../Goethe-Zertifikat_A1_Wortliste.pdf | ~650 | "© Goethe-Institut 2024, 2. Auflage" |
| A2 | .../Goethe-Zertifikat_A2_Wortliste.pdf | ~1,300 | "© 2016 Goethe-Institut ... Das Werk und seine Teile sind urheberrechtlich geschützt. Jede Verwertung in anderen als den gesetzlich zugelassenen Fällen bedarf ... der vorherigen schriftlichen Einwilligung des Goethe-Instituts." |
| B1 | .../Goethe-Zertifikat_B1_Wortliste.pdf | ~2,400 | "© 2016 Goethe-Institut und ÖSD ... urheberrechtlich geschützt ... schriftlichen Einwilligung" (joint Goethe + ÖSD + Universität Freiburg/CH) |
| B2 | **No standalone free PDF** at that path (HTTP 404). B2 vocabulary is embedded in the print publication *Prüfungsziele, Testbeschreibung* (Hueber/Klett) | ~4,000–4,500 | All rights reserved (print work) |

- **Format:** PDF (alphabetical word entries, thematic groups; some levels add translations in a separate column).
- **Licence / redistribution:** ❌ **All rights reserved.** The Goethe texts explicitly require *prior written consent* for any use beyond statutory exceptions. Downloadable ≠ redistributable. Bundling the list in an app is **not** permitted without a written licence from Goethe-Institut. B1 also requires ÖSD and Uni Freiburg sign-off (joint copyright).

### telc gGmbH "Wortschatzlisten"
telc does **not** publish an official exam wordlist equivalent to Goethe's; what is openly downloadable are coursebook vocabulary lists for the *Einfach gut!* (Integration A1–B1), *Einfach besser 100/400/500* (Berufssprache B1–B2) and *Auf jeden Fall!* series.

| File | URL (telc.net) | Approx. entries | Notes |
|------|----------------|-----------------|-------|
| Einfach gut! A1 alphabetisch | .../Einfach_gut/Wortschatzlisten/Einfach_gut_A1_Wortschatzliste_alphabetisch.pdf | ~640 (parsed 642) | Lektion-tagged |
| Einfach gut! A2 alphabetisch | .../Einfach_gut_A2_Wortschatzliste_alphabetisch.pdf | ~630 (parsed 632) | |
| Einfach gut! B1 alphabetisch | .../Einfach_gut_B1_Wortschatzliste_alphabetisch.pdf | ~590 (parsed 587) | |
| Einfach besser 400 (B2) | .../Einfach_besser_100_400_500/...Deutsch.pdf | ~ (B2 Berufssprache) | Definitions + examples |
| Auf jeden Fall! B1.1 | .../Auf_jeden_Fall/... (multilingual DE/PS etc.) | — | translation columns |

- **Format:** PDF (alphabetical, some with English/other-language columns).
- **Licence / redistribution:** ❌ telc *Impressum*: content is under German copyright; "Die Vervielfältigung, Bearbeitung, Verbreitung und jede Art der Verwertung ... bedürfen der schriftlichen Zustimmung"; "Downloads und Kopien ... nur für den privaten, nicht kommerziellen Gebrauch." → **Not redistributable.**

## 2. Openly-licensed alternatives

| Name | Publisher | Format | Approx. coverage | Licence / redistribution | Caveats |
|------|-----------|--------|------------------|--------------------------|---------|
| **Kaikki.org / Wiktextract German extract** | kaikki.org (from en.wiktionary dump, wiktextract by Tatu Ylonen) | JSONL, ~1.0 GB; browsable HTML | 352,527 distinct word forms; nouns 155k, verbs 144k, adjectives 312k (sense counts); includes inflections, POS, IPA, English glosses | ✅ **CC BY-SA 4.0 + GFDL** (inherits Wiktionary). Redistributable with attribution + share-alike. | No CEFR levels; English-language glosses; many inflected forms; may include obscure/proper nouns needing filtering |
| **hermitdave/FrequencyWords** | Hermit Dave (OpenSubtitles corpus) | TXT frequency lists | German top ~50k+ lemmas by frequency | ✅ **MIT** — commercial OK | Frequency, not CEFR; subtitle bias; needs lemma cleanup |
| **ph98/german-cefr-wordlist** | community (GitHub) | CSV/JSON + pipeline | Frequency-anchored German lemmas w/ CEFR A1–C2 tags | ✅ Code MIT; dataset derivative of MIT FrequencyWords → commercially usable | CEFR levels are **LLM/frequency-estimated**, not authoritative; verify quality |
| **Leipzig Corpora Collection (LCC)** | Universität Leipzig (Wortschatz) | TXT/CSV frequency & co-occurrence lists | Millions of types from news/web corpora | ✅ **CC BY 4.0** (attribution required) | Raw frequency, not CEFR-tagged; attribution + version citation required |
| **DeReWo (lemma/word-form lists)** | IDS Mannheim | TXT lists + documentation | ~300k lemmas, ~1.7M word forms | ⚠️ **CC BY-NC** (non-commercial) and "referencing or passing on without the corresponding documentation is not allowed" | NC blocks monetisation; must ship documentation |
| **DWDS word lists** | BBAW | — | — | ❌ **CC BY-ND** (no derivatives) | Unsuitable for an app that reuses/derives |
| Einlang / LanGeek / MindDory / onewholearns | third-party sites | web/PDF | CEFR-tagged, 1,000–16,000/level | ❌ Proprietary, "all rights reserved" | Reuse likely infringes; **do not** scrape/bundle |

## 3. Recommendation

1. **Do NOT bundle the Goethe (or telc) PDFs.** Both are explicitly "all rights reserved"; Goethe requires prior written consent, and the B1 list is jointly owned by Goethe + ÖSD + Universität Freiburg. If exact telc/Goethe exam fidelity is a hard requirement, you must **request a written licence** from the publisher — budget for that as a legal dependency in requirements.md.
2. **For a safely redistributable app, build the vocabulary layer from open data:**
   - **Headwords + senses/glosses:** Kaikki.org German extract (CC BY-SA 4.0) — attribute Wiktionary and license your derived word data under CC BY-SA (share-alike). This is the cleanest source for definitions, gender/POS and IPA.
   - **CEFR leveling:** use `ph98/german-cefr-wordlist` (MIT, commercially usable) and/or derive levels yourself from a permissive frequency list (FrequencyWords MIT, or Leipzig Corpora CC BY 4.0). Treat levels as *approximate*, not official.
   - Keep app **code** under your own license; isolate CC BY-SA word data and attribute it in an "About/Licenses" screen.
3. **Avoid** DWDS (ND) and DeReWo (NC) for anything redistributed or monetised; LCC/FrequencyWords are the safest frequency bases.
4. **Practical position for requirements.md:** the telc/Goethe lists can be *referenced for scope and ordering*, but the shipped data must come from CC BY-SA / MIT / CC BY sources, with an attribution screen and, if using Kaikki/Wiktionary, a share-alike note on the word data.
