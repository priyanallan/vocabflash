# Research: Grammar Content Sources (A1–B1)

> Feature: **Grammar Deck** — a categorised grid of grammar concepts (one concept = one
> snippet/card: "definite articles by case", "dative prepositions", …), level-filtered like
> vocabulary (B1 ⇒ A1 + A2 + B1), browse-and-recall only (no FSRS, no reveal flip).
>
> Status: **Research complete** (2026-10-22). Companion to `research-cefr-wordlists.md` and
> `research-dictionary-ipa-tts.md`. Open question: none blocking; pick sources when the ADR is written.

## 1. What the deck actually needs

Two distinct kinds of content, with different sourcing strategies:

1. **General rule tables** — the deck's concepts: definite/indefinite articles across all four
   cases, preposition sets, word order, verb conjugation patterns, plural patterns, weak
   masculine nouns, etc. Small, static, curated. ~25–35 concepts for A1, more for A2/B1.
2. **Per-lemma inflection** — the *actual* full declension/conjugation of a specific word
   (der Baum → dem Baum(e), des Hauses → den Häusern). Needed for the "grammar hook" in
   Word Details (case panels → full case table for *that noun*), not for the deck grid.

## 2. Sources evaluated

| Source | Licence | What it gives | Fit |
|---|---|---|---|
| **Kaikki German extract** (already in pipeline) | CC BY-SA 4.0 | **Structured per-lemma declension & conjugation.** Verified: every noun entry carries a `de-ndecl` table (nominative/accusative/dative/genitive × singular/plural, with `tags` like `["dative","singular"]`); verbs carry `de-conj` (person × tense × indicative/subjunctive × imperative, participles, auxiliary, preterite). Same 294 MB file the dataset-builder already streams. | ★★★ — **no new download, no new licence.** Powers kind 2 fully. |
| **Wikibooks German** (`en.wikibooks.org/wiki/German`) | CC BY-SA 3.0 / GFDL dual | Full grammar tables: articles, pronouns, adjective declension, verbs, word order. | ★★★ — primary outline source for kind 1; text can be adapted with attribution. |
| **Wikipedia "German declension" / de "Deutsche Deklination"** | CC BY-SA 4.0 | Authoritative complete case tables, weak-noun (n-Deklination) lists, adjective endings. | ★★☆ — reference for correctness of hand-authored tables; attribution screen entry. |
| **DeutschFokus/german-learning-toolkit** (GitHub) | **MIT** | Markdown grammar per level, A1 so far: `accusative.md`, `articles.md`, `dative.md`, `present-tense.md`, `word-order.md`. Beginner-friendly one-liners + small tables + examples. | ★★☆ — only 5 A1 topics and thin tables, but MIT (most permissive). Good *seed/outline*; content must be re-authored to our snippet format anyway. |
| **mein-deutschbuch.de** | **All rights reserved.** Impressum: private, non-commercial copies permitted; any other use needs written consent. | Extremely comprehensive German grammar topic tree (Wortarten → Kasus → Nebensätze…), simple explanations, A1–B2 exercises. | ★☆☆ — best *topic map* on the web, but content may not be copied. Use only as a coverage checklist (like we use Goethe PDFs, per ADR-0002 private-only logic). If the app ever gets published, nothing from here may ship. |
| **Cheatography "German Grammar A1" (edzillion)** | CC-licensed page hosting; sheet author's rights unclear | Topic outline: word order, subordinating/adverbial conjunctions, gender rules by suffix, n-declension, prepositions, dative verbs, separable prefixes, Perfekt rules. | ★☆☆ — inspiration/topic reference only (already agreed). Contains errors ("Ich habe es erklart"). Not a source of text. |
| **dudesam/german-cheat-sheet** (GitHub) | **None** (no LICENSE file) | A1–B1 grammar quick-reference layout. | ✗ — unlicensed = all rights reserved; look at structure only, copy nothing. |
| **Deutsche Welle / Lingolia / deutschwunder PDFs** | Proprietary | Polished grammar charts & exercises. | ✗ — no reuse rights; not even private-copy permission is stated. |
| **verbformen.com / Verbix** | Proprietary | Per-word declension/conjugation tables. | ✗ redundant — Kaikki covers this openly. |

## 3. Verified: Kaikki carries full inflection (probe, 2026-10-22)

Streamed a range of `kaikki.org/dictionary/German/kaikki.org-dictionary-German.jsonl` and confirmed:

```
Haus → Haus [nominative singular], Hauses [genitive singular], Haus/Hause [dative singular],
       Haus [accusative singular], Häuser [definite nominative plural], Häusern [definite dative plural] …
       (template `de-ndecl`)
gehen → gehe/gehst/geht/gehen/geht [present], ging [preterite], gegangen [past participle],
        sein [auxiliary], gehe/geh [imperative], ginge [subjunctive-ii] …
        (template `de-conj`)
```

Every noun/verb Entry therefore gets a **complete, per-word case/conjugation table for free**
at build time, from a source we already fetch and already must attribute. Plural, genitive and
dative-plural forms also backfill gaps in the Entry model (D7) — e.g. dative plural `-n`.

## 4. Recommendation

- **Kind 1 (rule tables/concepts):** hand-author our own `grammar.json` snippets. Outline taken
  from **Wikibooks German (CC BY-SA)**, cross-checked against **Wikipedia (CC BY-SA)**, seeded by
  **DeutschFokus (MIT)** where it overlaps; coverage checked against the mein-deutschbuch /
  cheatography topic lists without copying text. Attribution screen gains Wikibooks/Wikipedia.
- **Kind 2 (per-lemma inflection):** generate from **Kaikki** in `dataset-builder` — add a
  `declension` field (4 cases × 2 numbers) for nouns and a `conjugation` expansion for verbs.
  This also answers the earlier "link from Word Details" idea: the case panel deep-links to the
  Grammar concept, and Word Details gains the noun's *own* full table.
- **Licensing posture:** consistent with ADR-0002 (private-only) — but grammar content from
  open sources makes this feature *publishable*, unlike the Goethe Dataset.

## 5. Open items

- Exact A1 concept list (≈25–35) to be drafted against Wikibooks + cheatography outline.
- B1 topics (Passiv, Konjunktiv II, Relativsätze, Genitiv) only needed when B1 content ships.
- Adjective declension (3 types) is the hardest table to fit on one snippet — may split into
  2–3 concepts ("after der-words / after ein-words / after zero article").
