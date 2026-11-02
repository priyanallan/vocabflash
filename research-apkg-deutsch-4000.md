# Research: Anki deck "Deutsch 4000 German Words by Frequency" (.apkg)

Analysed 2026-11-02 from `/home/priyanallan/Documents/Projects/german/Deutsch_4000_German_Words_by_Frequency.apkg`
(48.2 MB archive; extracted contents: `collection.anki2` SQLite 4.2 MB + **4,220 media files, 51.7 MB of .mp3**).

## What it is

An Anki deck with **4,214 notes / 4,205 unique German headwords**, organised as **169 levels × 25
words** in frequency order. The note type is literally **"Memrise - 4000 German Words by Frequency"**
(2015) — i.e. derived from the Memrise course of that name.

### Fields (per note)

| Field | Coverage | Notes |
| --- | --- | --- |
| German headword | 100% | incl. multiword phrases and Swiss spellings (`abreissen`, `abschliessend` — no ß) |
| English meaning | 100% | multi-sense "alternatives" on ~17% |
| **Audio (.mp3)** | **100%** | 51.7 MB bundled |
| Sample sentence | **15.6%** (659) | single sentence, no translation |
| Level | 100% | **frequency ranks (Level 001–169), not CEFR** |

### What it does NOT have

No IPA · no plural · no verb conjugations (du/er/ihr, Präteritum, Partizip) · no case usage ·
no related words · no CEFR levels · no topics · 3,555 words have no example at all.

## Comparison with our dataset (`2026.10-public.8`, 3,863 entries)

| | .apkg deck | Our dataset |
| --- | --- | --- |
| Unique words | **4,205** | 3,863 |
| Overlap | **1,023 (26.5% of ours)** — complementary, not a replacement | — |
| Audio | **100% (51.7 MB mp3)** | none (TTS only) — but Kaikki gives Commons audio URLs for **99.4%** of our words |
| Examples | 15.6% | 100% (level-length-filtered, up to 5/word) |
| IPA | — | 96% |
| Plural / verb forms (du, er, ihr, Präteritum, Partizip) | — | 100% of verbs / nouns |
| Case usage (Akk/Dat) | — | 596 nouns |
| Related words | — | 572 entries |
| CEFR levels | — (frequency ranks only) | A1/A2/B1 |
| Licence | **none bundled**; Memrise-derived content | open: MIT wordlist, CC BY-SA 4.0 (Kaikki, OdeNet, UD), CC BY 2.0 FR (Tatoeba) |

## Verdict

- **More words?** Marginally (4,205 vs 3,863 unique) — but only **26.5% overlap**: it is a
  frequency-based list, ours is CEFR-based. It adds 3,182 words, but many are multiword phrases and
  Swiss-spelling variants.
- **Open-sourceable?** **No.** No licence is bundled, the content is derived from a Memrise course
  (Memrise terms apply to user content), and the 51.7 MB of mp3 audio is of unknown origin. It cannot
  be redistributed in an open-source app.
- **Replacement value:** weak — it lacks everything our dataset has (IPA, plural, verb forms, case
  usage, related words, CEFR levels) and its examples are sparse.
- **One genuine asset: the audio.** But it is unnecessary: **Kaikki already provides Commons audio
  URLs for 3,832 of our 3,857 words (99.4%)** (`sounds[].audio` + `ogg_url`/`mp3_url`, verified).
  Wikimedia Commons files are openly licensed with attribution, unlike the deck's mp3s.

## Recommendation

- Do **not** adopt the .apkg as a data source or for audio.
- For audio: add an optional `audioUrl` field to the pipeline output (from Kaikki `sounds[].audio`),
  keep TTS as the offline default, and optionally download a small Commons audio pack per level later.
