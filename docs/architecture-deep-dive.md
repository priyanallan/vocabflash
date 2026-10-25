# Architecture, patterns & scaling to new data sources

A deeper companion to `architecture-overview.md`: how the app is structured, which patterns are in
play, and what it would take to plug in additional vocabulary sources.

## 1. Shape: a layered, dependency-inverted core

```
        ┌───────────────────────────────────────────────────────────┐
        │ UI (Compose)   App.kt · ui/ · theme/                      │
        │   knows: domain types, repository                        │
        ├───────────────────────────────────────────────────────────┤
        │ DOMAIN (pure Kotlin)   Model · Fsrs · Queue · Progress    │
        │   knows: nothing else. No Compose, no DB, no platform.    │
        ├───────────────────────────────────────────────────────────┤
        │ DATA   db/ProgressRepository (SQLDelight) · data/ loader   │
        │   knows: domain types, SqlDriver, JSON                     │
        ├───────────────────────────────────────────────────────────┤
        │ PLATFORM   expect/actual: Speech · DatabaseDriverFactory   │
        │            · PlatformBackHandler                          │
        └───────────────────────────────────────────────────────────┘
```

Dependencies point **downwards only**. The domain has zero imports from any other layer, which is why
FSRS, the daily queue, spacing, deck difficulty and the example filter are all unit-testable without
Android, iOS, a database or a clock.

**Source sets** express the platform split: `commonMain` (everything above), `androidMain` /
`iosMain` (only the `actual` implementations and entry points). `commonMain` cannot compile a call to
a platform API, so portability is enforced by the compiler rather than by discipline.

**Build-time is a separate architecture.** `tools/dataset-builder` is a standalone JVM program, not a
library the app links. It is an ETL pipeline: *import word list → enrich from sources → emit a
versioned artifact*. The app only ever sees the artifact (`dataset.json`), never a source.

## 2. Design patterns actually in use

| Pattern | Where | Why it matters |
|---|---|---|
| **Layered architecture** | `ui → domain`, `data → domain` | The core can be tested and reasoned about in isolation. |
| **Functional core / imperative shell** | `Fsrs`, `buildStudyQueue`, `spaceOut`, `deckProgress`, `readableExamples` are pure functions; Compose is the shell | Same inputs → same outputs; no hidden state. |
| **Value objects** | `Entry`, `Card`, `StudyCard`, `ScheduleState`, `Rating` (immutable `data class`) | State transitions are `copy(...)`, so history can't be mutated in place. |
| **Repository** | `db/ProgressRepository` over SQLDelight | Hides SQL; the UI never sees queries or cursors. |
| **DTO + Mapper (anti-corruption layer)** | `data/DatasetDto.kt` → `DatasetLoader.toDomain()` | The wire format can evolve without touching the domain. Unknown keys are ignored; missing enrichment is tolerated. |
| **Adapter** | Python converters (`pdf_to_tsv`, `tatoeba_to_pairs`, `odenet_to_relations`, `tatoeba_case_examples`) | Each messy external format is normalised to a small canonical TSV. |
| **Pipeline / staged ETL** | `Pipeline.kt`: import → enrich → emit | Stages are independently testable; each source is optional. |
| **Strategy (by enum)** | `StudyMode` decides prompt/answer rendering; `Rating` drives FSRS; `CardState` drives transitions | Behaviour is selected by data, not by subclasses. |
| **Bridge / platform abstraction** | `expect`/`actual`: `Speech`, `DatabaseDriverFactory`, `PlatformBackHandler` | The common API is declared once, implemented per platform. |
| **Abstract factory** | `DatabaseDriverFactory.createDriver()` | The common code asks for a driver without knowing the platform. |
| **Manual dependency injection** | `MainActivity` / `MainViewController` construct the driver + repository and pass it into `App(repository)` | No framework (deliberate, D27); wiring is visible at the entry point. |
| **State hoisting + unidirectional flow** | `AppRoot` owns navigation and settings state; screens receive data + callbacks | Screens stay dumb; a single place owns state. |
| **Schema migration** | SQLDelight `1.sqm`, `2.sqm` (schema v3) | Existing installs upgrade instead of crashing. |
| **Provenance flags** | `sources: { goethe, kaikki, tatoeba, odenet, ud }` per Entry | Every field can be traced to a source — the basis for merging and for licensing. |

Deliberately **absent**: DI framework, navigation library, ORM, and any runtime network layer.

## 3. Why it scales to new data sources (the seams that already exist)

1. **The app is source-agnostic.** It consumes one versioned JSON artifact. Nothing in `composeApp`
   knows that Goethe, Kaikki, Tatoeba or OdeNet exist — only that an `Entry` may have optional fields.
2. **Enrichment is optional at every level.** DTO fields default to empty; the loader tolerates
   missing keys; the UI omits empty sections. A dataset from a completely different source loads fine
   as long as it has `id`, `lemma`, `level`.
3. **Stable IDs independent of any source.** `stableId(level, lemma, pos)` (normalised lemma + level)
   survives switching word lists — so study progress keyed by `entry_id` doesn't break.
4. **The pipeline's input contract is tiny.** A "word source" only has to produce `lemma` + `level`
   (+ optional article/pos). Everything else is enrichment.
5. **Provenance is recorded per Entry**, so merging several sources with precedence is a data
   decision, not a code rewrite.
6. **Licensing is data-driven.** The manifest and Attribution screen list sources; the Goethe
   dependency is isolated to one input file, so switching to an open list is a pipeline-input change
   (ADR-0002's escape hatch).
7. **Repository seam** (intended, D26/ADR-0003): persistence is behind one class, so a remote/synced
   implementation is a contained change — *see the caveat below*.

### The recipe for adding a source

1. Write a converter/adapter that normalises the source into TSV/JSONL (e.g. Anki `.apkg` → TSV).
2. Add a `Source` stage in the pipeline that reads it into the canonical model.
3. Decide the **precedence rule** per field (e.g. Goethe wins on level, Kaikki on inflection,
   Tatoeba on examples).
4. Regenerate `dataset.json`; the app needs **no change** if you only fill existing fields.
5. Add the licence to the manifest + Attribution screen, and a fixture test for the parser.

## 4. Where it will strain (honest assessment)

| Strain | Why | Suggested refactor |
|---|---|---|
| `Pipeline.kt` is a long orchestration function with `if (source != null)` per source | Adding 5 more sources makes it a god-function | Extract `interface EnrichmentStage { fun apply(entries, config): entries }`, register stages in a list, iterate |
| No `Source`/`WordListSource` abstraction | Each input format is handled ad hoc | `interface WordListSource { fun read(path): Sequence<RawEntry> }` with adapters (TSV, Anki, API) |
| Field precedence is implicit (`?:` chains in `Enricher`) | With 4 sources, "who wins" is hard to audit | Explicit `MergePolicy` per field, tested |
| `ProgressRepository` is a **concrete class**, not an interface | ADR-0003 promises a seam; the interface doesn't exist yet | Extract `interface ProgressStore` + `SqlDelightProgressStore`; inject the interface |
| Domain is implicitly German (article/plural/verb forms) | Adding another language needs grammatical information that isn't universal | Introduce `LanguageProfile` / split `NounInfo`, `VerbInfo` out of `Entry` |
| Cards are generated in the UI (`studyCards`) and not persisted | Card identity is `entryId + mode` strings; renaming a mode silently orphans progress | Persist a stable `mode` code with a migration, or generate Cards in the domain with a versioned template id |
| Screens take `List<Entry>` + `repository` directly | Fine at this size; grows awkward with more screens/state | A `StudySessionState` holder (plain class, still no framework) |
| No golden-dataset test | A converter regression is only caught by eyeballing | Snapshot test: run the pipeline on fixtures and diff the emitted JSON |

## 5. Recommended order before adding sources

1. `ProgressStore` interface (unlocks sync and testability).
2. `EnrichmentStage` registry + explicit `MergePolicy` (unlocks N sources).
3. `WordListSource` adapters (unlocks non-TSV inputs such as Anki or remote lists).
4. Golden-dataset snapshot test (protects all of the above).
5. Only then: language profile, if a second language is genuinely planned.
