# Research: hasharmujahid/deutsch-karten (German A1–C2 dataset + Android app)

Analysed 2026-11-02. Repo: https://github.com/hasharmujahid/deutsch-karten
(the URL without the trailing "b" 404s — the repo is `deutsch-karten`).

## What it is

A **5,684-word German A1–C2 vocabulary dataset** (CSV + JSON, level- and frequency-ordered) plus a
solo Android flashcard app (Kotlin + Compose) built on it. Pipeline: `fetch_sources.sh` → `build.py`
(queries de.wiktionary.org, batched + cached) → `gloss_coverage.py` (feasibility study).

- **Code licence: MIT** (build.py, fetch_sources.sh, app).
- **Data licence: separate — `LICENCE-DATA.md`**, assembled from four upstream sources:
  1. **A1–B2 — `goethe-5000` (5,009 rows)**: Goethe-Institut word lists packaged as TSV/CSV by
     [voothi/20260716201616-german-5000](https://github.com/voothi/20260716201616-german-5000) (MIT
     packaging). **The underlying word list is © Goethe-Institut** — redistribution must "respect the
     Goethe-Institut's terms".
  2. **C1/C2 — `wordfeather` (675 rows)**: CC **BY-NC 4.0** — non-commercial only, attribution required.
  3. **Wiktionary / Wikimedia Commons** (all rows): plurals, IPA, pronunciation audio — **CC BY-SA 4.0**
     (share-alike on those fields).
  4. **hermitdave/FrequencyWords** (OpenSubtitles 2018): corpus frequency ranks — **CC BY-SA 4.0**.

Their practical table: study/fork/modify ✅; free redistribution with attribution ✅ (their
interpretation); commercial use ❌ (because of the CC BY-NC C1/C2 layer). Dropping the C1/C2 layer
leaves `goethe-5000` (A1–B2) — but that layer is still © Goethe with only "respect the terms".

## Answer: is it open source up to B1?

**No, not in the redistributable sense.** The code is MIT, but the A1–B1 vocabulary (5,009 rows)
derives from the Goethe-Institut word lists, which carry **© Goethe-Institut** — the same status as
our own dataset.json. Their LICENCE-DATA interprets "publish for free with attribution" as
acceptable, but there is no open licence covering the Goethe-derived word list itself. A
truly-open-source publish still requires the open wordlist path (as our `2026.10-public.*` dataset
already does).

## Verified data quality (from `out/german_vocab_a1_c2.csv`)

| Field | Coverage | Notes |
| --- | --- | --- |
| `english` (gloss) | 5,682 / 5,684 | |
| `example_de` + `example_en` | 5,684 / 5,684 (**100 %**) | median 53 chars; **83 % ≤ 70 chars**, 99.9 % ≤ 110 — fits our level caps |
| `ipa` | 5,435 / 5,684 | |
| `audio_url` (Wikimedia .ogg) | 5,404 / 5,684 | **A1–B1: 3,059 / 3,071 (99.6 %)** |
| `conjugation` | 733 / 3,071 (A1–B1 verbs are sparse) | string format: `ich laufe \| du läufst \| er/sie/es läuft \| wir laufen \| ihr lauft \| sie/Sie laufen \| Prät. ich lief \| Perf. ich bin gelaufen` |
| `topic` | only on wordfeather rows | **empty for all 5,009 goethe-5000 rows** — no category data for A1–B1 |
| `corpus_freq_rank` | all rows | OpenSubtitles frequency |
| `id` + `content_version` + `rank_in_level` | all rows | stable IDs for sync |

Levels: A1 655 · A2 590 · B1 1,826 · B2 1,938 · C1 355 · C2 320 (**A1–B1 slice = 3,071**).

They also fixed two systematic Goethe-export errors in every present-tense table (`ihr isst` →
*esst*, `du hälst` → *hältst*), cross-checked against wordfeather's independent conjugations.

## Comparison with our current dataset (`2026.10-public.8`, 3,863 entries)

- Their A1–B1 slice covers **1,782 of our 3,857 lemmas (46 %)**.
- **Ours adds 2,075 lemmas they lack** — many are inflected forms (`abgefahren`, `abgeht`,
  `abgelehnt`, `alle/alles/allem/allen`, `affen`…), i.e. our wordlist source includes inflections
  that theirs treats as forms of one lemma.
- **Theirs adds 1,258 lemmas we lack** (`abbauen`, `abfall`, `abheben`, `abhängen`, `Abitur`, `Afrika`…).
- Audio URLs: theirs has **99.6 % coverage on A1–B1**; our app has no audio field at all yet (TTS only).
- Case usage (AKK/DAT): **not present** — our Tatoeba-derived case examples remain necessary.
- Multiple examples per word: **not present** (one example per word) — Tatoeba still needed.

## What is worth adopting, and what is not

**High value (private use, same licence posture as our current dataset):**
- Swap/add it as a **word source** (A1–B1 slice = 3,071 words): 100 % example coverage (DE+EN, short),
  99.6 % audio URLs, conjugations including du/er/ihr, stable IDs + `content_version` + frequency
  ranks. It is a superset of our Goethe extraction in most dimensions.
- Adopt their **`repair_present_tense()` cross-check** for our Kaikki verb forms.
- Adopt their **`LICENCE-DATA.md` structure** (per-source licence file + practical-use table) for our
  own dataset attribution.

**Not adoptable for an open-source publish:**
- The A1–B1 words themselves are © Goethe — an OSS release still needs the open wordlist path.
- The C1/C2 layer is CC BY-NC — never ship it in anything commercial.

**Caveats:**
- Solo project, 5 stars — pin/copy the data rather than depending on the repo.
- The MIT licence on the code does **not** extend to the data.
- Redistribution of the Goethe-derived vocabulary is legally murkier than their table suggests.
