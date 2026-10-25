# Requirements: German Vocabulary Flashcards (A1–B2)

> Status: **Discovery complete** — full requirement set captured via structured grilling.
> This file is the single source of truth for requirements.
> Related docs: `CONTEXT.md` (glossary), `research-cefr-wordlists.md` (wordlist licensing research),
> `research-dictionary-ipa-tts.md` (dictionary/IPA/TTS sources), `research-design-resources.md`
> (UI direction), `research-grammar-sources.md` (grammar content sources), `docs/adr/` (decision records).

> **Public repository note (2026-10-25):** this file records the original private-app
> requirement set. For the public GitHub repository, D3/D12/D18 (Goethe Wortliste) are
> superseded: the bundled dataset is built only from openly licensed sources and levels
> are approximate. See `docs/adr/0007-public-repo-open-data.md` and `NOTICE`.

## 1. Vision

**Two goals, in priority order:**

1. **Primary — a learning vehicle.** The owner is using this project to *learn Compose Multiplatform
   (KMP) and mobile development*. Where a choice exists between "the fastest shipped result" and
   "the path that teaches the most," favour learning. Prefer hand-rolling core logic over pulling in
   a library that hides the mechanics.
2. **Secondary — a genuinely useful German vocabulary app.** A private, offline-only mobile app
   (Android + iOS) for building German vocabulary across CEFR levels **A1–B2**, for the owner — a
   self-directed learner and language-course student — who studies in short sessions while travelling
   or at a dedicated study time. No accounts, no backend, no publishing. Built on the official
   Goethe-Institut Wortliste.

**Non-goals:** publishing the app, monetisation, multi-user support, backend/sync, exam-certification
fidelity.

---

## 2. Decisions Log

### Round 1 — Foundations

**D1. Platform — Kotlin Multiplatform (Compose Multiplatform), Android + iOS.**
Native mobile app for both platforms via a shared Kotlin/Compose codebase. No web/PWA.

**D2. Target user — the owner: self-learner *and* language-course student.**
Studies in short sessions, often while travelling. Low-friction daily habit is the priority.
Exam-specific modes are out of scope.

**D3. Content source — Goethe-Institut Wortliste (A1, A2, B1).**
Official CEFR wordlists extracted from the Goethe PDFs
(e.g. `goethe.de/pro/relaunch/prf/de/Goethe-Zertifikat_B1_Wortliste.pdf`). Legal for this project
because the app is private and unpublished (see D18); the lists are all-rights-reserved and must
never be redistributed. B2 has no free standalone PDF — deferred with D10.

**D4. Accounts — none. Fully offline, single-user.**
No login, no backend, no sync. All progress lives on-device and is exportable/importable.

**D5. Definition language — DE→EN default; DE→DE as optional B1/B2 enrichment.**
Definitions are stored **per language**, so DE→DE can be enabled later as data, not a rewrite.

### Round 2 — Content Model & Study Experience

**D6. Scheduling mechanic — FSRS.**
Free Spaced Repetition Scheduler, hidden behind simple rating buttons (Again / Hard / Good / Easy).

**D7. Entry model — tiered fields.**
- *Core (always)*: word/lemma, part of speech, CEFR level, translation(s).
- *German-specific (required where applicable)*: article + gender + plural for nouns; principal parts
  (Infinitiv / Präteritum / Perfekt) + separable flag for verbs.
- *Enrichment (optional)*: German definition, example sentence, audio, topic tag.
Extensible so enrichment can be filled in over time without schema churn.

**D8. Study modes for v1 — recognition, production, gender/article drill.**
Each mode is a Card template from the same Entry; one Entry yields multiple Cards.
Typing/spelling is next; cloze and listening deferred (need enrichment data).

**D9. Audio — in scope (revised Round 7).** Native OS Speech Synthesis (TTS), offline, 0 MB storage.
Nouns spoken with their article (*"der Baum"*). See D42.

**D10. Level coverage — A1–B1 first; B2 added later.**
Ship level *structure* for all bands, seed the Dataset A1–B1. No level empty/broken at launch.

**D11. Dataset delivery — bundled in the app.**
Versioned, plain, regenerable data file shipped with the app; no download server. Importable/exportable.

### Round 3 — Sourcing, Cards, Session UX

**D12. Content source (final) — Goethe Wortliste PDFs A1/A2/B1** (see D3/D18).

**D13. Card generation — one Card per (Entry × active Study Mode), up front.**
Each Card holds its own independent FSRS Schedule State. Enabling/disabling a mode only touches its
own Cards.

**D14. Daily session — 20 new Cards/day + all due Reviews.** Learner-adjustable in Settings (5–100,
steps of 5). Implemented as a real queue (previously the whole deck was walked each session).

**D43. Day boundary — local calendar day (midnight).** The new-Card allowance resets at local
midnight, keyed to `YYYY-MM-DD` in the device's time zone. Due Reviews are *not* tied to the boundary:
they appear whenever their FSRS interval elapses. (The alternative — a rolling 24h window — was
rejected as less predictable.)

**D44. Study progress indicator — counter + bar, top-right.** The study screen shows a
**"N left"** chip in the top-right corner plus a slim progress bar for the day's queue
(`answered / session size`). When the queue is exhausted the screen shows a *"Done for today"*
state instead of looping. *(Supersedes the no-counter rule in D34.)*

### Round 9 — Level model, richer content, navigation

**D45. Level selection — cumulative "max level" slider** *(replaces D39's per-level toggles)*.
Choosing B1 enables A1 + A2 + B1 (`level.ordinal <= maxLevel.ordinal`). Stored once as `max_level`;
the old `enabled_levels` key is read once for migration, then ignored.
*Open sub-decision: single deck for the chosen max level, vs a category view (theme → words).*

**D46. Examples — up to 10 per Entry, from Tatoeba (CC BY 2.0 FR).** Chosen to cover different
contexts/word forms rather than 10 near-duplicates; Kaikki's own examples stay as a fallback.

**D47. Related words — from OdeNet, the Open German WordNet (CC BY-SA 4.0).** Synsets/hypernyms/
hyponyms/co-hyponyms give semantic neighbours (Ordner → Akte, Schrank). Kaikki's `related`/
`hypernyms` fields are too sparse to rely on.

**D48. Undo last rating.** Restores the previous Card and its Schedule State.

**D49. Spacing rule — no two Cards of the same Entry adjacent.** The modes of one Entry
(DE→EN, EN→DE, gender drill) are separated by at least 3 other Cards in the queue.

**D50. Study navigation — position-based progress, swipe both ways, system back goes Home.**
The bar reflects position in the queue (`(index+1) / size`), swiping left/right moves to the
next/previous Card, and the system back gesture returns to the Decks screen before the app exits.

*Rejected: "swipe up = flag" — "Again" already flags a Card into the Review Deck (D36), so the
gesture adds nothing.*

### Round 10 — Final card / study design (grilled)

**D51. Word Details sheet** (from the mock): header **WORD DETAILS** + the word + an X to close, then
**Related words**, **examples**, and **case-usage panels** (AKKUSATIV / DATIV with an example sentence
and a short gloss each). One scroll, not tabs.

**D52. Editable ratings — no Undo button.** A rating is stored immediately and shown as *selected* on
its Card; swiping back to a Card shows its chosen rating and lets the learner **change** it. Changing
re-derives the Schedule State from the Card's *pre-rating* state, so the result is deterministic.

**D53. Navigation — swipe only, no arrows.** Swipe **left = next**, **right = previous**, clamped (no
wrapping). Progress bar is **position-based** (`(index+1)/size`) so it is visible as soon as a deck
opens; the counter shows Cards still unrated.

**D54. Single Deck (supersedes the per-level deck list).** The home screen shows **one** Deck for the
chosen max level, containing every Entry up to and including it. Deck counts, Difficulty and the rest
of the UI are unchanged. *(A category view — themes like Essen/Urlaub with counts — is deferred: it
needs a topic source we do not have yet.)*

**D55. Long words auto-shrink** to fit the card (and drop letter-spacing beyond ~12 characters)
instead of truncating.

### Round 11 — Batch 2 content contract

**D56. Case usage — prefer UD German GSD** (CC BY-SA 4.0). Case-tagged dependency trees supply real
AKKUSATIV/DATIV example sentences per lemma. Where UD has no sentence for a word, the panel is simply
omitted (no noisy fallback).

**D57. Dataset schema additions** (all optional; the app degrades gracefully when absent):
```json
"examples": [ { "de": "...", "en": "...", "source": "kaikki|tatoeba" } ],
"related":  [ { "word": "Akte", "relation": "similar|broader|narrower" } ],
"caseUsage":[ { "case": "accusative|dative", "sentence": "...", "gloss": "..." } ],
"verb":     { "infinitive": "...", "praeteritum": "...", "partizipII": "...",
              "presentDu": "...", "presentEr": "...", "presentIhr": "...", "partizipI": "..." }
```
Sources for these: **Tatoeba** (examples, CC BY 2.0 FR), **OdeNet** (related, CC BY-SA 4.0),
**UD German GSD** (case usage, CC BY-SA 4.0), **Kaikki** (verb forms, CC BY-SA 4.0).

### Round 12 — Grammar Deck

**D58. Grammar Deck — a separate, browse-only reference deck below the Vocabulary Deck.**
Opening it shows a **grid of Grammar Concepts** with the concept title above each snippet; tapping
opens the snippet **enlarged full-screen**. No flip/reveal, no ratings, no FSRS, no review queue —
it is deliberately outside the spaced-repetition loop (see ADR-0006).

**D59. One Grammar Concept = one card.** Each concept is a single cheat-sheet-style snippet
(table, sentence pattern, or small note). Concepts are grouped by category and level-filtered
cumulatively like vocabulary (`level <= max_level`; B1 ⇒ A1 + A2 + B1 concepts). The A1 set is the
27 concepts in `docs/grammar-concept-list.md`; genitive, two-way prepositions and
Präteritum of `sein`/`haben` are deferred to A2 by decision.

**D60. Snippets are rendered from data, never PNG assets.** `grammar.json` (versioned, bundled,
like the Dataset) holds structured tables/labels/examples/notes; one Compose renderer draws both
the grid thumbnail and the full-screen view, styled as a cheat-sheet card (crisp at any size,
dark mode, searchable later).

**D61. Grammar content — hand-authored from open sources.** Outline from Wikibooks German
(CC BY-SA), cross-checked against Wikipedia German declension articles (CC BY-SA), seeded by
DeutschFokus/german-learning-toolkit (MIT). mein-deutschbuch.de, the Cheatography A1 sheet and
unlicensed repos are topic checklists only — never copied. Attribution screen gains
Wikibooks/Wikipedia. (Research: `research-grammar-sources.md`.)

**D62. Per-lemma inflection from Kaikki.** `dataset-builder` is extended to emit each noun's full
case table (and verb conjugation) from Kaikki `de-ndecl`/`de-conj` tagged forms (verified
2026-10-22). Word Details gains the word's own declension and deep-links from its case panels
(D51) into the matching Grammar Concept. Backfill bonus: dative-plural `-n`, genitive forms.

**D63. Grammar scope — A1 content first, structure for A1–B1.** `grammar.json` schema is
level-agnostic; B2 later is appended data, no schema change.

**D15. Onboarding — single screen: level picker + "study all my levels" toggle.**
Level selection only filters which Entries become Cards; freely changeable. Placement quiz deferred.

**D16. Reminders — none.** No notifications in scope.

**D17. Progress stats — words seen, words got correct, words needing repetition.** Counts, not
dashboards. Surfaced by the "My Progress" menu item. *(Revised per Round 6.)*

### Round 4 — Distribution, UX, Polish

**D18. Distribution — private, local, never published.** ⚠️ Constraint that unlocks D3/D12:
using the copyright-protected Goethe lists is lawful only because the app is not distributed.
If this ever becomes public, the Dataset must switch to an openly-licensed source (see
`research-cefr-wordlists.md`: Kaikki CC BY-SA + frequency/CEFR levelling).

**D19. Backup — manual JSON export/import only.** Includes Schedule State + Dataset version so
restores are faithful. No automatic backup, no OS-share in v1.

**D20. Browse & search — yes, accessed from within a Deck.** Word list + search + Entry detail view,
outside review. *(Revised: no top-level Browse menu item; the list lives inside each Deck.)*

**D21. UI language — English only.** No localisation pipeline in v1. Strings may be externalised for
future-proofing at no extra cost.

**D22. Accessibility — deferred, not in v1.** Revisit (font scaling, screen-reader, colour-blind-safe
grading cues) if the app is ever published.

**D23. Visual design — clean, calm, content-first** (pending final selection; see design resources in
`research-design-resources.md` / conversation).

### Round 5 — Project purpose & learning-led trade-offs

**D24. Project purpose — learning Compose Multiplatform first, app second.**
Where "fastest shipped result" conflicts with "most educational path," favour learning. Hand-roll
core logic (e.g. the FSRS scheduler) rather than use a library that hides the mechanics. Non-goals:
publishing, monetisation, multi-user, backend/sync.
*This supersedes any earlier implication that shipping polish is the primary aim.*

**D25. FSRS implementation route — implement FSRS-6 from spec in `commonMain`.**
Hand-rolled in shared Kotlin as a learning exercise (per D24): ~200–400 LOC of pure, deterministic
functions, 21 parameters, validated against reference vectors (e.g. `R(S,S)=0.9`). Vendor the MIT
`FSRS-Kotlin` only if time-boxed; native `fsrs-rs` bindings rejected as overkill. Storage fields per
Card: `difficulty`, `stability`, `state`, `due`, `last_review`, `reps`, `lapses`.

**D26. Forward-compatibility seam — no sync now, but keep the door open.**
No accounts/sync in the current private, copyrighted-content world (D4 confirmed). However, if
openly-licensed content is later adopted (see `research-cefr-wordlists.md`), the owner may add
auth + cross-device sync. Requirement: architect so this is a *contained* change, not a rewrite —
- Dataset is source-agnostic and versioned; Entry CEFR level is data, not hard-coded, so an official
  or open list can override it.
- Entries have stable IDs independent of any source list.
- All persistence sits behind repository interfaces (local implementation today, remote later).
- Schedule State and progress are serialisable (already needed for D19 export).
This is a deliberate seam, not speculative complexity: it costs almost nothing now.

**D27. Learning priorities (highest → lowest).**
`a` Compose UI & state → `b` KMP shared logic & expect/actual → `f` iOS interop → `h` Gradle/KMP
project structure → `d` DI → `e` navigation → `c` persistence (SQLDelight) → `g` testing.
Consequences: target iOS from early on (not bolted on at the end); build small hand-rolled DI and
navigation rather than reach for frameworks; keep tests proportionate/light (testing is last, so
don't gold-plate it) though the FSRS scheduler still gets reference-vector tests since it's pure and
high-risk.

**D28 (provisional). Repo as teaching artifact — yes.**
ADRs (below), a short README, and light unit tests document what was learned and why. *Assumed unless
corrected; testing kept minimal per D27.*

### Round 6 — UI/UX (Klarr design)

**D29. Palette — light: pastel gradient (coral/peach → lavender/periwinkle) + off-white card + purple
accent; dark: clean deep neutral, minimal colour.** (Refines D23.)

**D30. Typography — Poppins**, heavy display weight with wide letter-spacing for the word, regular
for UI/body.

**D31. Rating controls — four colour-coded pill buttons (Again · Hard · Good · Easy) showing predicted
next intervals.**

**D32. Streak widget — weekly Mon–Sun dot row.**

**D33. Navigation — top app bar with an overflow (⋮) dropdown menu.** Menu items:
**My Progress · Review · Test · Settings**. The default home screen is the **Decks** list. The study
flow is entered from a Deck. *(Revised: there is no "My Decks" menu item — Decks are the home content.)*

**D34. Study-screen chrome — clean card: no timer, no gear** (revised by D44).

**D35. Review flow — reveal-then-rate**, with intervals on the rating buttons. The reveal shows
translation(s), plural / verb principal parts, **the article if available**, and an example sentence
only where enrichment exists.

**D36. Rating *Again* routes the Card into the Review Deck** for focused re-practice.

**D37. Test mode — random Entries from the currently selected Level(s)**, as a one-off check distinct
from scheduled study.

**D38. Deck Difficulty indicator — shown at the bottom of each Deck**, derived from the learner's
performance, not authored.

**D39. Settings — level toggle A1 / A2 / B1 / all combined** (extends D15). Filters which Decks and
Test sources are active.

### Round 7 — Offline assets, dictionary & audio

**D40. Offline bilingual dictionary — DE→EN and EN→DE, bundled.**
Source: **Kaikki / Wiktextract German extract** (CC BY-SA 4.0), trimmed to the A1–B1 Entries and the
needed fields; stored as **SQLite / JSONL**, footprint a few MB (raw 294 MB). Used as an *instant local
search index* to (a) backfill missing article / plural / verb forms and (b) supply extra example
sentences for Entries. No network calls. Surfaces **both** on the Entry detail screen and as a search
field inside a Deck. (FreeDict/Ding rejected: GPL/AGPL and weaker data; dict.cc/LEO/Linguee/DeepL:
proprietary, no redistribution.)

**D41. Phonetics — IPA pronunciation guides, bundled.**
Primary source: **Kaikki `sounds[]`** IPA (already bundled with the dictionary); **WikiPron** TSV
(~2 MB, 60k German pairs) as an optional supplement if coverage is thin. Displayed on the card / Entry
detail as a pronunciation guide, **only when available** — never show an empty row.

**D42. Audio — native OS Speech Synthesis (TTS), offline, 0 MB.**
Generates offline German audio playback on demand; nouns spoken with their article. No bundled audio
files, no network. Caveat: the platform TTS may require the German voice to be installed (one-time).
Optional upgrade: **Piper** (engine MIT, `de_DE` thorsten voice **CC0**), fully bundled and permissive,
at the cost of ~20–60 MB — fall back to system TTS.

---

## 3. Open Questions / Backlog

- **B2 content** — no free standalone Goethe B2 list; decide source when B2 is started (D10).
- **Audio** — deferred (D9); pick TTS or recorded audio when implemented.
- **Typing/spelling mode** — next study mode after v1 modes ship (D8).
- **Placement quiz, cloze, listening** — deferred (need enrichment data).
- **Accessibility** — revisit if published (D22).
- **Grammar Deck implementation** — author the 27 A1 `grammar.json` concepts (D58–D59), build the
  Grammar grid/detail screens, extend `dataset-builder` with Kaikki declension/conjugation
  extraction and the Word Details deep-link (D62). A2/B1 content afterwards (D63).
---

## 4. Glossary

The domain glossary lives in `CONTEXT.md`.

---

## 5. UI/UX Design

> Status: **Mostly locked** — final details being confirmed.

**Design reference:** [Klarr — Anki-Style Flashcard App (Figma Make)](https://www.figma.com/make/YBGtFv052eue2ri4yiar4j/Anki-Style-Flashcard-App?t=75VdSxqVjNmnDbC5-1). Two screenshots provided 2026-10-06 (front + revealed).

### Design language
- **Working product name:** *Vocab Flash* (the design reference is called *Klarr*).
- **Palette (D29):** *Light* = soft pastel gradient background (coral/peach → lavender/periwinkle), off-white card with a faint gradient glow, **purple accent**. *Dark* = clean, deep neutral, minimal colour.
- **Typography (D30):** **Poppins**. Heavy display weight with wide letter-spacing for the word; regular weight for UI/body.
- **Card:** large rounded off-white card, centred; an uppercase letter-spaced **micro-label** at top (topic/level); the word big; an example sentence with its gloss; translation.
- **Rating row (D31):** four colour-coded pill buttons — **Again · Hard · Good · Easy** — each with its predicted next interval.
- **Streak widget (D32):** *"7 day streak"* with a Mon–Sun dot row.

### Navigation & information architecture (mobile adaptation)
- **Navigation (D33):** a **top app bar with an overflow (⋮) dropdown menu** (not the reference's
  persistent sidebar). Menu items: **My Progress · Review · Test · Settings**.
- **Home (default screen):** the **Decks** list — one Deck per level (A1, A2, B1), each showing a
  **Difficulty** indicator at the bottom. A Deck opens the study flow.
- **Study flow:** the Klarr card (reveal-then-rate). Entered from a Deck.
- **My Progress:** words seen, words got correct, words needing repetition (D17).
- **Review:** the **Review Deck** — Cards rated *Again* / marked unknown (D36).
- **Test:** random Entries from the selected level(s) (D37).
- **Settings:** level toggle A1 / A2 / B1 / all combined (D39), theme, export/import.

### Study-screen chrome (D34)
Clean card — **no timer, no numeric counter, no gear**. A **slim progress bar appears only while a card
is swiping**.

### Review flow (D35)
**Reveal-then-rate:** front = prompt; reveal the answer; rate Again/Hard/Good/Easy with intervals shown.
Rating **Again** sends the Card to the Review Deck (D36).

### Card content (from the reveal screenshot)
- **Recognition (DE→EN):** front = word with article; reveal = translation(s), **article if available**,
  plural / verb principal parts, **IPA pronunciation** (D41), and an example sentence **only where
  enrichment exists**.
- **Audio:** a TTS play control (D42) reads the word aloud (nouns with their article).
- **Micro-label** shows topic or level.

*Source screenshots to be stored in `design/`.*

---

## 6. Offline Assets & Tech Stack

Recommended tech stack for a lightweight offline mobile app (from user's table, 2026-10-06):

| Asset Type | Primary Source Format | Storage Footprint | Primary Mobile Use Case |
|---|---|---|---|
| **Dictionary Text** | SQLite / JSONL (**Kaikki** / Wiktextract) | few MB (trimmed) | Instant local offline search index |
| **Phonetics** | IPA Strings (**Kaikki `sounds[]`**, WikiPron optional) | ~2 MB | Displays pronunciation guides on screen |
| **Audio Voice** | Native OS Speech Synthesis (**TTS**); Piper optional | 0 MB (Piper: 20–60 MB) | Generates offline German audio playback |

**Notes**
- All three are **bundled / on-device**; no network access (consistent with D4).
- **Dictionary source: Kaikki / Wiktextract German** (CC BY-SA 4.0) — also supplies **examples** and
  **IPA**, so a separate IPA source is usually unnecessary. FreeDict/Ding (GPL/AGPL) and
  dict.cc/LEO/Linguee/DeepL (proprietary, no redistribution) were rejected.
- **IPA:** Kaikki `sounds[]` primarily; WikiPron (CC BY-SA, ~2 MB) optional supplement.
- **Audio:** native OS TTS (0 MB); optional fully-bundled **Piper** (engine MIT, voice CC0) if voice
  quality/offline reliability matters.
- Open sources require an **attribution screen**; CC BY-SA is share-alike. The Goethe Dataset
  (ADR-0002) remains separate and private-only.
- Entry model gains/wires up: `ipa`, and enrichment fields fed from the dictionary (D7).
- **Runtime APIs are non-viable** (offline rule); APIs are used at build time only.

---

## 7. Dataset Pipeline (Goethe → Kaikki)

A **separate, JVM-only Gradle module** `tools/dataset-builder` (build-time only; its output is bundled
into the app). Not part of the app runtime.

```
Fetcher          fetch + cache Kaikki de-extract.jsonl.gz and the Goethe PDFs (idempotent)
GoetheExtractor  Goethe PDFs → word + level (PDFBox); normalise, strip numbering, dedupe
KaikkiIndex      stream the JSONL (never load ~294 MB); keep only lemmas in the Goethe set
Matcher          exact → case-fold → strip-article → fuzzy; record confidence + unmatched
Merger           Entry = Goethe level + Kaikki enrichment, enforcing the D7 field tiers
Emitter          dataset.json + dataset-manifest.json (version, counts, unmatched, licences)
```

- **PDF library: Apache PDFBox** (Apache-2.0) — pure JVM, purpose-built text extraction with
  position/layout APIs. **PDFium rejected**: it is a *rendering* engine (native, per-platform); its
  KMP wrappers are for *displaying* PDFs, which the app does not need.
- Matching happens at **build time**, never while studying. Every unmatched word is reported, not
  silently dropped.
- Output: **JSON resource** bundled for v1 (parsed at startup); migrate to a prepopulated SQLite DB
  only if it grows. IDs are stable, derived from the normalised lemma + level (D26).
- **Privacy/licensing:** the Goethe lists are copyrighted (ADR-0002) — extracted content must **never**
  be committed or redistributed. Kaikki/WikiPron attribution screen required.

*Implementation in progress as a background task (`tools/dataset-builder/`).*
