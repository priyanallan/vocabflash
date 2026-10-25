# dataset-builder

A JVM-only, independently runnable Gradle module that builds the German vocabulary **Dataset** for
Vocab Flash. It takes a **level-tagged wordlist you are allowed to use**, enriches every entry from
the **Kaikki/Wiktextract German extract** (article/gender, plural, verb forms, translations, IPA,
examples), and emits a versioned JSON dataset plus a manifest and a QA report.

> **Licensing**
>
> - **The repository ships no wordlist.** You supply the A1/A2/B1 list on the command line. Use a
>   source you may redistribute — for example
>   [`ph98/german-cefr-wordlist`](https://github.com/ph98/german-cefr-wordlist) (MIT; levels are
>   frequency-estimated) or your own list. Do **not** commit third-party PDFs or extracted lists.
> - **Kaikki / Wiktextract** enrichment is **CC BY-SA 4.0**: attribution is required and the derived
>   dataset is share-alike. Tatoeba sentences are **CC BY 2.0 FR**; OdeNet and UD German GSD are
>   **CC BY-SA 4.0**. See the root `NOTICE` for the attribution text.
> - This tool is **build-time only**. It performs no network calls and never reads PDFs.

## Layout

```
tools/dataset-builder/
  build.gradle.kts            self-contained JVM build (own plugin versions; not tied to the root catalog)
  settings.gradle.kts         lets the module build standalone from this directory
  src/main/kotlin/app/vocabflash/datasetbuilder/
    Main.kt / Cli.kt          CLI entry point + arg parsing
    Pipeline.kt               stage orchestration
    model/Models.kt           serializable Dataset schema (schemaVersion = 1)
    wordlist/WordListSource.kt   normalized TSV/CSV wordlist importer
    kaikki/KaikkiParser.kt    JSONL line → CompactKaikki
    kaikki/KaikkiReader.kt    streaming gzip/plain reader + lemma index
    match/Matcher.kt          exact / case-normalized matching
    enrich/Enricher.kt        merge wordlist + Kaikki into a DatasetEntry
    tatoeba/TatoebaSource.kt  prepared de/en Tatoeba sentence pairs → examples
    tatoeba/SimpleCaseSource.kt  Tatoeba-derived simple case examples → caseUsage
    odenet/OdeNetSource.kt    prepared OdeNet relations → related
    ud/UdCaseSource.kt        UD German GSD CoNLL-U → caseUsage (fallback)
    emit/DatasetWriter.kt     dataset.json / manifest.json / qa-report.md / unmatched.tsv
    util/Text.kt              normalization, slug + stable-id generation
  src/test/…                  fixture-based unit tests
```

All enrichment sources are **absent-safe**: if a path is missing or unreadable the pipeline still
runs and simply omits that field.

## Preparing the wordlist input

Supply a delimiter-separated file with a header. `lemma` and `level` are required; everything else
is optional and Kaikki fills the gaps.

```
lemma	level	pos	article	gender	plural	verb_praeteritum	verb_partizip2	translation	topic
Haus	A1	noun					house	Housing
laufen	A1	verb					to run	Actions
schön	A2	adj					beautiful	Description
```

Column names are matched case-insensitively, with common synonyms accepted (`word`/`stichwort` for
`lemma`, `wortart` for `pos`, `genus`/`geschlecht` for `gender`, `präteritum` for past, …). The
delimiter is auto-detected: tab if the header contains a tab, otherwise comma; `"`-quoting is
supported. Multiple translations are separated by `;` or `|`.

For the dataset bundled in this repository the list was derived from
[`ph98/german-cefr-wordlist`](https://github.com/ph98/german-cefr-wordlist) (MIT), which maps
FrequencyWords (MIT) into approximate CEFR bands. Levels from that list are **approximate, not
official CEFR**.

## Obtaining the Kaikki extract

Download `de-extract.jsonl.gz` from <https://kaikki.org/dictionary/German/> once. Keep it
**outside** the repository (it is ignored). The reader accepts `.jsonl` and `.jsonl.gz`.

## Preparing the case examples (Tatoeba)

`caseUsage` shows a noun in the accusative/dative. Prefer the Tatoeba-derived, learner-friendly TSV
produced by `tatoeba_case_examples.py`:

```
<lemma>\t<case>\t<sentence>\t<gloss>
Brot\taccusative\tIss das Brot!\tAfter "für"
```

```bash
python3 tatoeba_case_examples.py --pairs input/tatoeba.tsv \
                                 --filter input/wordlist.tsv \
                                 --out input/caseexamples.tsv
```

Point the pipeline at it with `--simple-case input/caseexamples.tsv`. The UD German GSD treebank
(`--ud`) is a fallback for lemmas with no simple-case evidence.

## Build & run

From the repository root:

```bash
export JAVA_HOME=…            # JDK 21
./gradlew -p tools/dataset-builder build
./gradlew -p tools/dataset-builder run --args="--wordlist input/wordlist.tsv \
  --kaikki input/de-extract.jsonl.gz --out out \
  --tatoeba input/tatoeba.tsv --odenet input/odenet.tsv \
  --simple-case input/caseexamples.tsv --ud input/ud.conllu \
  --dataset-version 2026.10-public.1"
```

### CLI

```
--wordlist <file.tsv>        Level-tagged wordlist (required)
--kaikki <de-extract.jsonl[.gz]>  Kaikki German extract (required)
--out <dir>                Output directory (required)
--dataset-version <s>      Version stamped into the Dataset (default: "dev")
--levels <list>            Comma-separated CEFR levels (default: A1,A2,B1)
--tatoeba <path>           Prepared Tatoeba sentence-pair TSV (de<TAB>en). Optional
--odenet <path>            Prepared OdeNet relation TSV. Optional
--simple-case <path>       Tatoeba-derived case-example TSV. Optional; preferred over --ud
--ud <path>                UD German GSD CoNLL-U file. Optional; case-example fallback only
--max-translations <n>     Cap translations per entry (default: 5)
--max-examples <n>         Cap examples per entry (default: 3)
--max-tatoeba-examples <n> Cap Tatoeba examples per entry (default: 5)
--max-related <n>          Cap OdeNet relations per entry (default: 6)
--max-ipa <n>              Cap IPA strings per entry (default: 3)
--limit <n>                Only the first n wordlist rows (smoke testing)
--help
```

All paths are supplied on the command line; nothing personal is hard-coded.

## Output

| File | Contents |
| --- | --- |
| `dataset.json` | The versioned Dataset (`schemaVersion` + `datasetVersion` + sorted `entries`). |
| `manifest.json` | Counts, match rate, per-level counts, sources, licences, tool version. |
| `qa-report.md` | Human-readable QA: totals, field coverage, unmatched lemmas, warnings. |
| `unmatched.tsv` | wordlist lemmas not found in Kaikki (emitted with wordlist-only data). |

Entry IDs are **stable** and derived from normalized lemma + level (+ `pos` when present), e.g.
`a1-haus`, `a2-schoen-adj`.

## Staged design

1. **Import** — read the normalized TSV/CSV wordlist; filter by level.
2. **Index** — stream the gzipped Kaikki JSONL line by line (never fully buffered) into a lemma index,
   keeping the richest entry per lemma.
3. **Match** — exact lemma first, then case-normalized.
4. **Enrich** — the supplied wordlist is authoritative; Kaikki only fills gaps.
5. **Emit** — deterministic, sorted JSON + manifest + QA + unmatched report.

## Limitations / not yet implemented

- **No PDF extraction.** The wordlist input is a prepared TSV/CSV.
- **Senses/examples are truncated.** Examples and IPA are capped per entry; glosses are not emitted.
- **No POS disambiguation.** If a lemma appears as several parts of speech, the richest Kaikki entry
  wins (unless the wordlist pins `pos`). Multi-POS splitting is a future enhancement.
- **Separable-verb flag** is only taken from the wordlist; it is not inferred from Kaikki.
- **In-memory lemma index.** Fine for a few hundred thousand lemmas; a disk-backed index would be
  needed for much larger inputs.
- **B2** is out of scope for the bundled dataset.

## Tests

```bash
./gradlew -p tools/dataset-builder test
```

The suite runs the full pipeline over tiny synthetic fixtures (no third-party content) and covers
matching, enrichment, gzip streaming, level filtering, stable-ID generation and the delimited reader.
