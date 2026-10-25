# Grammar Deck concept list (A1–B1)

> **Status:** A1 shipped as `grammar.json` (draft-2026.10.3); A2 and B1 authored and wired in.
>
> The Grammar Deck is a separate deck below the Vocabulary Deck on Home. It contains one
> reference card per concept, grouped by category. **Levels are separate decks** (revision
> 2026-10-22): the A1 deck shows only A1 concepts, the A2 deck only A2, and the B1 deck only B1 —
> no cumulative mixing. B2 can be added later without changing the structure.
>
> Cards are Compose-rendered cheat-sheet snippets, not PNGs. The grid item shows the concept
> title and a compact table/diagram preview; opening it presents the same snippet enlarged to
> fill the screen. No flip, reveal, rating, FSRS state, or study queue.

## A1 candidate set

This is a deliberately bounded starting set. The level labels are practical curriculum groupings,
not a claim that every course introduces each topic at exactly the same point. Keep the complete
case/article table available as a reference even where a learner's A1 course only expects active use
of some cases.

### Sentence structure

| ID | Concept | One-snippet content |
|---|---|---|
| `a1.word-order.main-clause` | Main clauses: verb in position 2 | A sentence strip illustrating subject-first and fronted-time/place orders, with the finite verb fixed in slot 2. |
| `a1.word-order.questions` | Yes/no and W-questions | Two short word-order patterns: verb-first questions and W-word + verb. |
| `a1.word-order.coordinating-conjunctions` | Joining main clauses | `und`, `aber`, `oder`, `denn` with the reminder that normal main-clause verb position remains. |
| `a1.word-order.negation` | `nicht` or `kein`? | A compact contrast: `kein` negates a noun phrase; `nicht` negates other material / the clause, with simple examples. |
| `a1.pattern.es-gibt` | `es gibt` + accusative | Fixed pattern plus one singular and one plural example, highlighting the accusative article. |

### Nouns, articles, and cases

| ID | Concept | One-snippet content |
|---|---|---|
| `a1.cases.case-roles` | The cases at a glance | One row per case: case name, common role/question, and a short example. A1 shows nominative, accusative, and dative; the genitive row is added with the A2 content (decision below). |
| `a1.articles.definite-nom-acc-dat` | Definite articles across cases | A single matrix: nominative/accusative/dative × masculine/feminine/neuter/plural (`der/die/das/die`, etc.). No genitive column at A1. |
| `a1.articles.indefinite-nom-acc-dat` | `ein`-words: case forms | A matrix for `ein` across the same three cases, plus a note that German has no indefinite plural article; include `kein`/possessive endings only if the snippet remains legible. |
| `a1.articles.gender-cues` | Clues to noun gender | High-confidence common suffix/meaning clues, explicitly labelled as tendencies with exceptions—not deterministic rules. |
| `a1.nouns.plurals` | Common plural patterns | A small pattern grid (`-e`, `-(e)n`, `-er`, `-s`, no ending) with sample nouns and an umlaut marker; note that plural is often learned with the noun. |

### Pronouns and possessives

| ID | Concept | One-snippet content |
|---|---|---|
| `a1.pronouns.personal-nominative` | Personal pronouns: subject forms | `ich, du, er/sie/es, wir, ihr, sie, Sie`, with person/number labels. |
| `a1.pronouns.personal-accusative` | Personal pronouns: accusative | Nominative-to-accusative pairs (`ich → mich`, `du → dich`, …) in a compact table. |
| `a1.pronouns.personal-dative` | Personal pronouns: dative | Nominative-to-dative pairs (`ich → mir`, `du → dir`, …), with one short example. |
| `a1.pronouns.interrogative` | Interrogative pronouns (W-words) | The A1 W-words (`wer/wen/wem`, `was`, `wann`, `wo/woher/wohin`, `wie`, `warum`) with what each asks for; note the case in `wer → wen → wem`. Added after the Lingolia coverage check. |
| `a1.pronouns.possessive` | Possessive determiners | `mein/dein/sein/ihr/unser/euer/Ihr` with a compact ending pattern and one article-like example; avoid trying to show every case if it makes the thumbnail unreadable. |

### Verbs

| ID | Concept | One-snippet content |
|---|---|---|
| `a1.verbs.present-regular` | Regular verbs in the present | Verb stem + endings in a six-person conjugation table, using one regular example. |
| `a1.verbs.sein-haben` | `sein` and `haben` | Side-by-side present-tense conjugation tables. |
| `a1.verbs.present-stem-change` | Common present-tense stem changes | `e → i/ie` and `a → ä` examples in `du`/`er-sie-es`; show unchanged forms alongside. |
| `a1.verbs.modal` | Modal verbs + infinitive | Common modals in a compact form table plus the sentence frame: conjugated modal in position 2, infinitive at the end. |
| `a1.verbs.separable` | Separable verbs | Prefix separation in a main clause, plus infinitive/perfect forms in a small before/after example. |
| `a1.verbs.imperative` | Giving a simple command | `du`, `ihr`, and formal `Sie` imperative patterns with one example each. |
| `a1.verbs.perfekt-frame` | Perfekt: sentence frame | Auxiliary in position 2 + past participle at the end; a pair of short examples. |
| `a1.verbs.perfekt-auxiliary` | Perfekt: `haben` or `sein`? | Beginner-level rule of thumb and common examples; label it as a rule of thumb, not an exhaustive account. |
| `a1.verbs.participle` | Building the past participle | Regular `ge-…-t`, common `ge-…-en`, `-ieren` without `ge-`, and separable/inseparable prefix patterns. |

### Adjectives and adverbs

| ID | Concept | One-snippet content |
|---|---|---|
| `a1.adjectives.predicative` | Adjectives and adverbs: one form | Adjective after `sein`/`werden`/`bleiben` and the same form used as an adverb with a verb; note that declined adjectives before a noun come at A2. Added after the Lingolia coverage check. |

### Prepositions and everyday patterns

| ID | Concept | One-snippet content |
|---|---|---|
| `a1.prepositions.accusative` | Common accusative prepositions | The common A1 set (`für`, `ohne`, `um`, `durch`, `gegen`) with one tiny example each or a memorable grouping. |
| `a1.prepositions.dative` | Common dative prepositions | `aus`, `bei`, `mit`, `nach`, `seit`, `von`, `zu` with one compact example/pattern. |
| `a1.prepositions.time` | Common time prepositions | `um` (clock time), `am` (days/dates), `im` (months/seasons), and `von … bis` in a small grid. |
| `a1.prepositions.contractions` | Preposition + article contractions | Common forms such as `im`, `am`, `zum`, `zur`, `ins`, `ans`, with their expanded forms. |

**Total: 29 A1 concepts** (27 original + 2 added after the Lingolia coverage check, 2026-10-22).
Combine or split concepts only if the snippet remains readable as a thumbnail and useful at
full-screen size.

## Lingolia coverage check (2026-10-22)

Checked against the topic tree at `deutsch.lingolia.com/en/grammar` (used as a coverage
checklist only — no text copied):

- **Covered by the A1 set:** Präsens (regular/stem-change/sein-haben), Perfekt (frame/auxiliary/
  participle), modal verbs, imperative, separable verbs, articles + noun declension, plurals,
  personal pronouns (nom/acc/dat), possessives, negation, questions, main clauses, conjunctions
  (coordinating), prepositions (accusative/dative/time), es gibt.
- **Added from the check:** interrogative pronouns (W-words), predicative adjectives/adverbs.
- **Deliberately later (matches Lingolia's grouping, mapped to our levels):** reflexive verbs and
  pronouns (A2), adjective declension (A2), comparative/superlative (A2), dependent clauses and
  subordinating conjunctions (A2), Präteritum + sein/haben Präteritum (A2), two-way prepositions
  (A2), dative verbs (A2), inseparable verbs as a topic (A2), relative clauses (B1), passive (B1),
  Konjunktiv (B1), indirect speech (B1), Futur (A2/B1), Plusquamperfekt (B1).

## A2 set (14 concepts, authored 2026-10-22)

| ID | Concept | One-snippet content |
|---|---|---|
| `a2.cases.genitive` | The genitive case | Genitive article matrix (`des/der/des/der`, `eines/einer/eines`), noun `-s/-es`, possession, `eines Tages`. |
| `a2.cases.dative-accusative` | Verbs with dative + accusative objects | `geben/zeigen/schenken/…` with the person-first rule and the pronoun exception (`Ich gebe es ihm`). |
| `a2.nouns.n-declension` | Weak masculine nouns | `der Junge → den Jungen` pattern table with exceptions (`das Herz`). |
| `a2.verbs.praeteritum` | Präteritum: sein, haben & modals | `war/hatte` conjugation plus `konnte/musste/wollte/durfte/sollte`. |
| `a2.verbs.dative-verbs` | Verbs that take the dative | Core set (`helfen, danken, gefallen, gehören, …`) with one example each. |
| `a2.verbs.reflexive` | Reflexive verbs and pronouns | Accusative reflexive pronouns, common verbs, dative reflexive (`Ich putze mir die Zähne`). |
| `a2.verbs.inseparable` | Inseparable verbs | Prefix list (`be-, ver-, er-, …`), no `ge-` in the participle, verb never splits. |
| `a2.word-order.subordinate-clauses` | Subordinate clauses | Verb-final rule + conjunction list (`weil, dass, wenn, obwohl, …`). |
| `a2.word-order.als-wenn` | als or wenn? | Single past event vs repeated/present/future; `wann` for questions. |
| `a2.adjectives.declension-der-words` | Adjective endings after der-words | Nom/Akk/Dat × gender ending matrix (`der -e / den -en / dem -en …`). |
| `a2.adjectives.declension-ein-words` | Adjective endings after ein-words | Same matrix for `ein/kein/mein` + zero-article note (`guter Wein`). |
| `a2.adjectives.comparative-superlative` | Comparative and superlative | Regular + irregular forms (`gut → besser → am besten`), `als`/`so … wie`. |
| `a2.prepositions.two-way` | Two-way prepositions | `wo? → dative` vs `wohin? → accusative` with the motion/position verb rule. |
| `a2.prepositions.time-more` | More time prepositions | `vor, nach, ab, gegen, während` with examples. |

## B1 set (9 concepts, authored 2026-10-22)

| ID | Concept | One-snippet content |
|---|---|---|
| `b1.word-order.relative-clauses` | Relative clauses | Relative pronoun case matrix + examples incl. dative (`Die Frau, der ich geholfen habe`). |
| `b1.word-order.temporal-clauses` | Time clauses | `nachdem/bevor/während/bis/seitdem` with the `nachdem` tense rule. |
| `b1.word-order.infinitive-clauses` | Infinitive clauses | `um … zu / ohne … zu / statt … zu` with separable `anzurufen` note. |
| `b1.verbs.passive` | The passive | `wird/wurde/ist … worden` patterns + `von`/`durch` agents. |
| `b1.verbs.plusquamperfekt` | Plusquamperfekt | `hatte/war + Partizip II`, past-before-the-past, `nachdem` example. |
| `b1.verbs.konjunktiv-ii` | Konjunktiv II | `würde + infinitive`, `hätte/wäre/könnte`, polite requests. |
| `b1.verbs.futur` | Futur I | `werden + infinitive`; present-with-time-word alternative; assumption use. |
| `b1.verbs.verbs-with-prepositions` | Verbs with fixed prepositions | `warten auf / sich freuen auf / teilnehmen an …` with case per preposition. |
| `b1.prepositions.genitive` | Genitive prepositions | `wegen/trotz/während/statt/aufgrund` + genitive, colloquial dative note. |

**Total: 52 concepts — 29 A1 + 14 A2 + 9 B1.** B2 remains future work.

## Defer to B2 (initial candidates)

These are not in the A1 set above; add them when building A2/B1 content and confirm against the
chosen syllabus before assigning a level:

- **A2 candidates:** adjective endings; dative + accusative objects; common dative verbs; subordinate
  clauses with `weil`/`dass`; comparative/superlative; weak masculine nouns (`n`-declension); more
  time/place prepositions; Präteritum of common modal verbs.
  **Moved from A1 by decision (2026-10-22):** the genitive row/column in the case and article
  tables; Präteritum of `sein`/`haben`; two-way prepositions (`wo?`/`wohin?`).
- **B1 candidates:** relative clauses; passive voice; Konjunktiv II; genitive use and genitive
  prepositions; broader subordinate-clause/conjunction patterns; adjective declension in fuller
  detail.

## UI/content constraints for authoring

- One concept maps to exactly one grid item and one full-screen detail view.
- Each item has a short title visible above/alongside its thumbnail. The snippet itself is one
  compact visual unit: table, sentence pattern, or small diagram—not a page of prose.
- Store structured rows, labels, examples, and notes as data; draw them with Compose. The thumbnail
  and enlarged view must use the same content and renderer.
- Keep essential text in the snippet, not only in the title. Use a small note for exceptions where
  omitting it would teach a false rule.
- Do not copy text or artwork from the Cheatography sheet. Use it as a topic checklist only. Author
  explanations and examples originally, and record/share required attribution for any adapted
  CC BY-SA material. Kaikki remains the open source for per-lemma forms, not the source for general
  grammar rules.

## Decisions on the A1 set (2026-10-22)

1. **Set size:** the 27-concept A1 list was accepted as-is, then extended to 29 after the
   Lingolia coverage check (interrogative pronouns, predicative adjectives).
2. **Genitive:** excluded from A1. The case-roles card and both article matrices show
   nominative/accusative/dative only. With levels being separate decks, the genitive is its own
   A2 concept (`a2.cases.genitive`) rather than rows added to the A1 tables.
3. **Two-way prepositions (`wo?`/`wohin?`) and Präteritum of `sein`/`haben`:** A2, not A1.
   They are removed from the A1 set and recorded in the A2 candidates list.
4. **Level separation (revision 2026-10-22):** each level's deck shows only its own concepts
   (A1 ≠ A1+A2). Supersedes the cumulative filtering described in the original D59.
