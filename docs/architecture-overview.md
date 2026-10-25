# Architecture overview — a guided tour for a KMP beginner

This document explains how the app is put together, in plain language, with pointers to the real
files. It is a teaching artifact, not a spec (see `requirements.md`).

## 1. The big idea: one codebase, two platforms

**Kotlin Multiplatform (KMP)** means: you write most of your app once in Kotlin, and the compiler
produces two different kinds of app from it:

- **Android** — normal Kotlin running on the JVM/Dalvik.
- **iOS** — the same Kotlin compiled to native code (Kotlin/Native).

**Compose Multiplatform** does the same for the UI: one set of Compose screens renders on both
platforms.

How the source tree expresses this:

```
composeApp/src/
  commonMain/     ← SHARED. The app's brain and its UI. ~90% of the code.
  androidMain/    ← Android-only glue (MainActivity, Android SQLite, Android speech).
  iosMain/        ← iOS-only glue (MainViewController, iOS speech, iOS SQLite).
  commonTest/     ← Tests that run the same shared code on every platform.
  androidUnitTest/← Tests that need the JVM (e.g. a real in-memory SQLite database).
```

The one rule to remember: **`commonMain` may not call platform APIs directly.** Anything that
touches a phone's specifics goes behind a *declared-in-common / implemented-per-platform* pair,
called **expect/actual**. This project has two of them — see §5.

## 2. The layers

Inside `commonMain` the code is deliberately layered. Each layer only talks to the one below it.

```
┌─────────────────────────────────────────────────────────────┐
│  UI      App.kt, ui/, theme/        Compose, Material 3     │
│                      │                                       │
├──────────────────────┼───────────────────────────────────────┤
│  LOGIC   domain/     Fsrs.kt, Queue.kt, Progress.kt        │  ← pure Kotlin, no libraries
│                      │                                       │
├──────────────────────┼───────────────────────────────────────┤
│  DATA    db/, data/  ProgressRepository, DatasetLoader     │  ← SQLDelight, resources
└─────────────────────────────────────────────────────────────┘
```

### `domain/` — the brain (pure Kotlin, zero dependencies)

This is the most valuable code in the project. It knows nothing about Android, iOS, Compose or
databases — it is just data structures and functions. That purity is why it is easy to test.

- `domain/Model.kt` — the vocabulary of the app: an **Entry** (one German word with article,
  plural, verb forms…), a **Card** (an Entry asked in one StudyMode), a **Rating** (Again/Hard/
  Good/Easy), a **StudyCard**, and the supported levels.
- `domain/Fsrs.kt` — the **FSRS-6 spaced-repetition scheduler**, hand-written from the spec.
  Given what you rated a card and how long ago, it computes the card's new *stability* and
  *difficulty* and when it should come back. Pure maths in ~200 lines.
- `domain/Queue.kt` — today's study queue: **due reviews + up to 20 new cards**, and the
  local-midnight day boundary. One pure function, `buildStudyQueue()`.
- `domain/Progress.kt` — how a deck's **Difficulty** chip is computed from your ratings
  (New/Easy/Medium/Hard).

### `data/` and `db/` — the memory

- `db/ProgressRepository.kt` + `sqldelight/.../CardState.sq` — everything you have ever rated:
  the schedule state of each card, whether it is in the Review Deck, daily counters, settings.
  SQLDelight reads the `.sq` SQL and generates Kotlin types at build time.
- `data/DatasetLoader.kt` — reads the bundled `dataset.json` (the 2,471 Goethe words enriched by
  Kaikki) and maps it onto the domain model. Falls back to `SampleData` if the file is missing.

### `ui/` — the face

- `App.kt` — the app shell: which screen is showing, the top bar, the ⋮ overflow menu.
- `ui/Screens.kt` — deck list (Today), Settings, Progress, Review deck, Test, Attribution.
- `ui/StudyScreen.kt` — the card flow: tap to flip, the der/die/das quiz, rating pills,
  the "N left" counter. This is the largest and most instructive file.
- `theme/Theme.kt` — colours, shapes, Poppins typography, light/dark.

## 3. expect/actual — the KMP trick you must understand

Two APIs are declared in `commonMain` as `expect`, and each platform provides an `actual`:

| What | Where declared | Android implementation | iOS implementation |
|---|---|---|---|
| Speech (text-to-speech) | `tts/Speech.kt` | `Speech.android.kt` (Android `TextToSpeech`) | `Speech.ios.kt` (iOS `AVSpeechSynthesizer`) |
| Database driver | `db/DatabaseDriverFactory.kt` | `DatabaseDriverFactory.android.kt` (Android SQLite) | `DatabaseDriverFactory.ios.kt` (iOS SQLite) |

The shared code only ever sees the interface. This is the seam that keeps the app portable.

## 4. A request, end to end

Follow one rating through the system — it touches every layer:

1. You open a deck. `App.kt` → `StudyScreen` with all of the deck's `StudyCard`s.
2. `StudyScreen` asks the `ProgressRepository` for each card's saved schedule state.
3. `buildStudyQueue()` decides what you see today: due reviews first, then up to 20 new cards.
4. You answer. For nouns you may hit the **gender drill** — three `der/die/das` buttons.
5. Your rating goes into `Fsrs.nextState()` → the card's next due date is computed.
6. `ProgressRepository.save()` writes it. If you rated **Again**, the card is flagged into the
   Review Deck. If it was your first time seeing it, the daily new-card counter increments.
7. Tomorrow, `localDayKey()` gives a new day key, the new-card allowance resets, and the queue
   fills with whatever became due overnight.

## 5. Build-time tooling (separate from the app)

`tools/dataset-builder/` is a **standalone JVM module** — it never runs on a phone. It reads the
copyrighted Goethe Wortliste PDFs (`pdf_to_tsv.py` + a Kotlin CLI), enriches each word from the
open Kaikki/Wiktextract dictionary, and emits `dataset.json`, which the app bundles. This split
exists for licensing reasons and keeps heavy data processing off the device.

## 6. The build files

- `settings.gradle.kts` — which modules exist (`composeApp`).
- `gradle/libs.versions.toml` — every dependency version in one place (the *version catalog*).
- `gradle.properties` — JVM sizes and two AGP-9 compatibility flags.
- `composeApp/build.gradle.kts` — the multiplatform setup: Android target, iOS targets, and which
  libraries go in which source set.

## 7. Suggested reading order (a learning path)

1. `domain/Model.kt` — the nouns of the problem.
2. `domain/Queue.kt` and `domain/Progress.kt` — small, pure, and testable.
3. `domain/Fsrs.kt` — the deepest piece of shared logic.
4. `db/ProgressRepository.kt` + the `.sq` files — how shared code talks to a database.
5. `App.kt` — how screens and navigation are wired.
6. `ui/StudyScreen.kt` — state, animation and the quiz, the file where most UI ideas live.
7. `tts/` and `db/DatabaseDriverFactory*` — expect/actual in practice.
8. `tools/dataset-builder/` — build-time data engineering.
