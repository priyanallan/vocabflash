# CONTEXT.md

Domain glossary for the German vocabulary flashcard app. Definitions only — no implementation
details, no spec. Terms are added here as they crystallise.

## Language

**Entry** (Vocabulary Entry):
A single German lemma together with its linguistic attributes (part of speech, article/gender,
plural, verb principal parts, translations, level).
_Avoid_: Word, term, item

**Card**:
A single study prompt generated from one Entry for one study mode and direction
(e.g. German→English recognition, English→German production, gender drill). Many Cards may derive
from one Entry.
_Avoid_: Flashcard (when meaning a generated prompt), question

**Field**:
A named piece of data on an Entry (e.g. article, plural, an English translation). Fields are
grouped into core, German-specific, and enrichment tiers.
_Avoid_: Attribute, property, column

**Study Mode**:
The kind of retrieval a Card asks for — recognition, production, or gender/article drill.
_Avoid_: Exercise type, drill type

**Review**:
A single act of a learner answering one Card and rating how well they recalled it.
_Avoid_: Answer, attempt

**Rating**:
The learner's self-assessment after a Review (Again / Hard / Good / Easy), which drives scheduling.
_Avoid_: Grade, score, mark

**Level**:
A CEFR proficiency band (A1, A2, B1, B2) attached to an Entry and used to select what is studied.
_Avoid_: Tier, difficulty, grade

**Schedule State**:
The per-Card memory state that determines when the Card is next due (difficulty, stability,
retrievability under FSRS).
_Avoid_: Progress, interval (alone)

**Study Session**:
A bounded run of Reviews the learner completes in one sitting.
_Avoid_: Practice, drill, lesson

**Dataset**:
The bundled, versioned collection of Entries shipped with the app.
_Avoid_: Wordlist (reserve for the external published Goethe/telc sources), content, database

**Deck**:
A level-based study collection of Cards the learner studies together (e.g. the A1 Deck). Surfaced on
the home screen, each Deck showing an at-a-glance Difficulty indicator.
_Avoid_: Level (a Level is a CEFR band; a Deck is a study collection built from one or more Levels),
course, folder

**Review Deck**:
The collection of Cards the learner has marked as failed/unknown (rated *Again*), kept for focused
re-practice. Surfaced by the "Review" menu item.
_Avoid_: Review (that is the *act* of answering a Card), mistakes, trouble words

**Test**:
A mode that samples random Entries from the currently selected Level(s) for a one-off check, distinct
from scheduled study.
_Avoid_: Quiz, exam

**Difficulty**:
An at-a-glance indication of how hard a Deck currently is for the learner, shown at the bottom of each
Deck. Derived from performance, not authored.
_Avoid_: Level, grade, rating

**Day**:
The local calendar day (midnight to midnight, device time zone) that bounds the daily new-Card
allowance. Distributed as a `YYYY-MM-DD` key. Due Reviews are independent of the Day.
_Avoid_: Session, sitting

**Dictionary**:
The bundled, openly-licensed offline bilingual reference (DE→EN / EN→DE) used to fill gaps in an Entry
— missing article, plural, verb forms, or extra examples — and to support lookup. Distinct from the
Dataset (which defines *what is studied*; the Dictionary *enriches* it).
_Avoid_: Wordlist, lexicon, vocabulary

**Grammar Concept**:
A single reference unit of German grammar — one rule, table, or pattern (e.g. definite articles
across cases, dative prepositions) authored as structured data and shown as one cheat-sheet snippet.
Not derived from an Entry and not scheduled.
_Avoid_: Grammar card (reserve "Card" for study prompts), rule, topic

**Grammar Deck**:
The browse-only collection of Grammar Concepts, shown below the Vocabulary Deck on Home and
level-filtered like vocabulary. It has no ratings, Schedule State, or queue.
_Avoid_: Reference (too generic), cheatsheet

**Snippet**:
The rendered visual content of one Grammar Concept — a table, sentence pattern, or small diagram —
drawn from data by one shared renderer for both thumbnail and full-screen views.
_Avoid_: Image (snippets are not bitmap assets), card
