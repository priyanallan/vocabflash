# Add a browse-only Grammar Deck: rendered snippets, hand-authored from open sources

The learner needs a place to **look at a grammar concept and recall it** — the four cases,
article tables, preposition sets, word order — without the friction of study cards. The app adds a
**Grammar Deck**, separate from the Vocabulary Deck:

- **Home placement:** below the Vocabulary Deck. Opening it shows a **grid of Grammar Concepts**,
  grouped by category, each a cheat-sheet-style **snippet** with the concept title above it.
- **One concept = one card.** Tapping a grid item opens the same snippet **enlarged to fill the
  screen**. No flip, no tap-to-reveal, no ratings, no FSRS Schedule State, no review queue — the
  deck is a reference, deliberately outside the spaced-repetition loop.
- **Level filtering** follows the vocabulary model: a concept is shown when its CEFR level is
  `<= max_level` (B1 ⇒ A1 + A2 + B1 concepts). B2 can be appended later as data, without schema
  change.
- **Rendered, not photographed.** Snippets are drawn by Compose from structured data
  (`grammar.json`): tables, sentence patterns, small notes. There are **no PNG assets** — one
  renderer serves both the thumbnail and the full-screen view, so everything stays crisp,
  themeable (dark mode) and searchable. The renderer is styled to *look* like a cheat-sheet card.
- **Content sourcing** (see `research-grammar-sources.md`): the A1 set (27 concepts,
  `docs/grammar-concept-list.md`) is **hand-authored** — outline from **Wikibooks German
  (CC BY-SA)**, cross-checked against **Wikipedia's German declension articles (CC BY-SA)**,
  seeded by **DeutschFokus/german-learning-toolkit (MIT)** where they overlap. Text and examples
  are written originally; mein-deutschbuch.de, the Cheatography sheet and unlicensed repos are used
  as topic checklists only, never copied. Attribution gains Wikibooks/Wikipedia entries.
- **Per-lemma inflection comes from Kaikki**, the source already in the pipeline (CC BY-SA 4.0,
  verified 2026-10-22: `de-ndecl` and `de-conj` tagged forms). `dataset-builder` is extended to
  emit a noun's full case table (and verb conjugation) per Entry, so **Word Details** can show the
  word's own declension and deep-link from its case panels into the matching Grammar Concept.

This keeps the deck cheap to build and honest about what it is: recall support, not instruction.
Open sources mean, unlike the Goethe Dataset (ADR-0002), the Grammar Deck would be safe to keep if
the app were ever published.

## Consequences
- **New bundled data file:** `grammar.json` (versioned, like the Dataset), with `GrammarConcept`
  ids scoped per level (`a1.…`, `a2.…`, `b1.…`); a schema/parse test guards it.
- **New screen(s):** Grammar grid + concept detail in the hand-rolled `Screen` stack; Grammar is
  reachable from Home and via deep-link from Word Details (D51's case panels).
- **`dataset-builder` grows a `declension`/`conjugation` extraction step** from Kaikki forms;
  Entry gains optional per-word case forms (feeding Word Details, not the Grammar grid).
- **Deferred to A2 content, by decision:** genitive in the case/article tables, two-way
  prepositions (`wo?/wohin?`), and Präteritum of `sein`/`haben`.
- **No scheduling changes:** FSRS, Study Modes and queues are untouched.
- **Authoring is manual:** the 27 A1 concepts are written by hand into `grammar.json`; no scraper.

## Considered Options
- **Grammar cards inside the FSRS queue** (tables atomised into Q/A cards) — rejected for now:
  card explosion, and grammar is better recalled in use; can be layered on later per-concept.
- **Bundled PNG snippets** (pre-rendered cheat-sheet images) — rejected: blurry when enlarged,
  dark-mode double work, unsearchable, and ~30+ images per level to maintain.
- **Copying mein-deutschbuch.de or the Cheatography sheet** — rejected: not openly licensed;
  topic-map use only.
- **Scraping a general rule dataset at build time** — rejected: no structured open source exists
  for *rules* (Kaikki covers inflection, not grammar exposition); hand-authoring 27 snippets is
  smaller than any pipeline.
