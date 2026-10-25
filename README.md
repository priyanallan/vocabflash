# Vocab Flash

An offline-first German vocabulary app for Android and iOS — CEFR A1–B2 structure, FSRS spaced
repetition, Enriched word details, offline pronunciation, and a browse-only grammar deck. Built with
Kotlin Multiplatform and Compose Multiplatform. No accounts, no backend, no tracking.

> **Learning project.** Vocab Flash exists first as a vehicle for learning Compose Multiplatform,
> Kotlin Multiplatform, mobile architecture and spaced-repetition scheduling. Where a choice existed
> between "fastest shipped result" and "the path that teaches the most", it deliberately takes the
> learning path (see [`docs/adr/0004-learning-first-purpose.md`](docs/adr/0004-learning-first-purpose.md)).
> That is why the FSRS-6 scheduler, the navigation stack and the dependency wiring are hand-rolled
> rather than pulled from libraries.

## Highlights

- **Spaced repetition, implemented from the FSRS-6 spec** — a pure, deterministic scheduler in
  `commonMain`, validated against reference vectors, hidden behind the usual Again/Hard/Good/Easy
  buttons with predicted intervals.
- **Vocabulary built from open data** — 4,500+ German entries with articles/gender, plurals, verb
  principal parts, IPA, English translations, Tatoeba example sentences, OdeNet related words and
  case-usage examples. All bundled, versioned JSON — no runtime APIs.
- **Three study modes per word** — German→English recognition, English→German production and an
  article/gender drill; each mode has its own independent schedule.
- **Daily queue with a real allowance** — new cards are capped per local day (adjustable), due
  reviews are unlimited, and the session is resumable.
- **Word Details sheet** — related words, up to 10 level-appropriate examples, and accusative/dative
  usage panels, plus offline TTS that speaks nouns with their article.
- **Grammar deck** — 52 hand-authored cheat-sheet concepts (A1 29 · A2 14 · B1 9) rendered from
  structured JSON: a level-filtered grid, section filter, and a full-screen snippet per concept.
  No bitmap assets, no reveal/rating flow — it is a reference, not a review queue.
- **Local-first persistence** — SQLDelight-backed progress with an exportable/importable state; no
  network permission anywhere.
- **KMP end to end** — shared domain, data and Compose UI; `expect/actual` only for platform TTS,
  database driver and back handling.

## Tech stack

| Layer | Choice |
|---|---|
| Language / UI | Kotlin 2.4, Compose Multiplatform 1.11 (`androidx.compose`) |
| Platforms | Android (min SDK 24) and iOS |
| Scheduling | Hand-rolled FSRS-6 (`domain/Fsrs.kt`) |
| Persistence | SQLDelight, local only |
| Data | Versioned JSON in Compose resources (`dataset.json`, `grammar.json`) |
| Build | Gradle 9.5, AGP 9.1, version catalog |
| Speech | Native OS TTS (`TextToSpeech` / `AVSpeechSynthesizer`) |

## Architecture at a glance

```
composeApp/src/
  commonMain/kotlin/com/priyanallan/vocabflash/
    App.kt                    app shell, hand-rolled Screen stack, top-bar menu
    domain/                   Entry/Card/Deck model, FSRS-6, queue building, grammar model
    data/                     dataset + grammar JSON DTOs and loaders, sample data
    db/                       ProgressRepository over SQLDelight
    ui/                       Deck list, study flow, word details, grammar grid/snippet, settings
    theme/                    Klarr-inspired theme (pastel gradient, Poppins)
    tts/  platform/           expect declarations
  androidMain/  iosMain/      actual implementations (TTS, SQLDelight driver, back handling)
  commonTest/                 FSRS vectors, queueing, progress, example filtering
  androidUnitTest/            repository + grammar.json parse guard
tools/dataset-builder/        build-time JVM pipeline (not shipped in the app)
docs/                         ADRs, architecture notes, research
```

Design decisions are recorded as ADRs in [`docs/adr/`](docs/adr/): FSRS-6 hand-rolled, local-first
with seams, offline dictionary/TTS, learning-first trade-offs, and the grammar deck.

## Data sources and licensing of content

The app code is **MIT** (see [`LICENSE`](LICENSE)). The **bundled word data is share-alike** and is
licensed separately — see [`NOTICE`](NOTICE) for the full attribution text.

| Content | Source | Licence |
|---|---|---|
| Level-tagged headwords (A1–B1) | [`ph98/german-cefr-wordlist`](https://github.com/ph98/german-cefr-wordlist) (frequency-anchored CEFR estimates) | MIT |
| Definitions, gender, plurals, verb forms, IPA, examples | [Kaikki / Wiktextract German extract](https://kaikki.org/dictionary/German/) (Wiktionary) | CC BY-SA 4.0 |
| Example sentences | [Tatoeba](https://tatoeba.org) deu–eng pairs | CC BY 2.0 FR |
| Related words | [OdeNet](https://github.com/hdaSprachtechnologie/odenet) (Open German WordNet) | CC BY-SA 4.0 |
| Case-usage examples | Tatoeba-derived; UD German GSD fallback | CC BY 2.0 FR / CC BY-SA 4.0 |
| Grammar concepts | Hand-authored from openly licensed references (Wikibooks, Wikipedia) | see `grammar.json` source refs |
| Fonts | Poppins | SIL OFL 1.1 |

Levels are **approximate, frequency-derived**, not official CEFR certifications. No copyrighted
exam wordlist is included in this repository; the builder accepts any level-tagged list you have the
right to use (see [`tools/dataset-builder/README.md`](tools/dataset-builder/README.md)).

The dataset is rebuilt with:

```bash
./gradlew -p tools/dataset-builder run --args="--wordlist wordlist.tsv \
  --kaikki de-extract.jsonl.gz --out out --tatoeba tatoeba.tsv --odenet odenet.tsv \
  --simple-case caseexamples.tsv --ud ud.conllu --dataset-version 2026.10-public.1"
```

## Build & run

Prerequisites: **JDK 17+** (21 recommended), Android SDK for Android builds; macOS + Xcode for iOS.

```bash
export JAVA_HOME=~/.local/share/jdks/jdk-21.0.12.1+1   # adjust to your JDK

./gradlew :composeApp:assembleDebug        # build the Android APK
./gradlew :composeApp:installDebug         # install on a device/emulator
./gradlew :composeApp:testDebugUnitTest    # unit tests
```

APK output: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

iOS: open the project in Xcode (iOS target is built on macOS) and call `MainViewController()` from
a SwiftUI host.

## Roadmap

- B2 vocabulary and grammar content.
- Per-word declension/conjugation tables generated from Kaikki (already available in the extract).
- Typing/spelling study mode; optional listening mode.
- Accessibility pass (font scaling, screen-reader labels, colour-blind-safe cues).

## Acknowledgements

Built on the shoulders of open data and open source: Wiktionary/Kaikki, Tatoeba, OdeNet,
Universal Dependencies, FrequencyWords, Wikibooks/Wikipedia, and the Compose Multiplatform team.
