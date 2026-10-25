# Publish the repository with open data; no copyrighted wordlists in history

The project is now public on GitHub. That invalidates the private-use basis on which the Goethe
Wortliste was previously used as the level authority (ADR-0002): bundling or redistributing that
list is not permitted without a written licence, and history that contains it would redistribute it
too.

Decision:

- **The public repository ships only openly licensed data.** The bundled `dataset.json` is built
  from a level-tagged wordlist derived from [`ph98/german-cefr-wordlist`](https://github.com/ph98/german-cefr-wordlist)
  (MIT; levels are frequency-anchored estimates), enriched from **Kaikki/Wiktextract** (CC BY-SA 4.0),
  **Tatoeba** (CC BY 2.0 FR), **OdeNet** (CC BY-SA 4.0) and **UD German GSD** (CC BY-SA 4.0).
- **Levels are approximate.** They are not official CEFR certifications and the app says so in its
  attribution screen.
- **History is rebuilt.** The public `main` branch starts from a fresh history authored for this
  repository; the earlier private history (including any Goethe-derived artefacts) is not pushed.
- **The builder is source-agnostic.** `--wordlist` accepts any level-tagged TSV/CSV the operator has
  the right to use. No wordlist, PDF or extracted list is committed; the module's `.gitignore`
  excludes them.
- **Licensing split:** code under MIT; Kaikki-derived word data under CC BY-SA 4.0 with attribution
  in `NOTICE`. Grammar content is original, with openly licensed references recorded per concept.

## Consequences

- Public dataset counts differ from the old private build (4,565 entries here vs 2,471), and A1/A2/B1
  boundaries are frequency-derived.
- The app remains fully offline; no runtime APIs were introduced.
- Anyone rebuilding the dataset must supply their own wordlist file; only open sources are committed.
- ADR-0002 remains as a record of the private phase and is superseded for anything published.

## Considered Options

- **Keep the Goethe-derived dataset but make the repo private** — rejected: the point was a public
  portfolio repository.
- **Publish code only, no data** — rejected: the app would not run out of the box; a small, legally
  clean dataset is more useful.
- **Rewrite history in place (`filter-repo`) on the existing branch** — rejected in favour of a fresh
  history: the old commits still describe a private-only design, and a clean initial history is
  easier to review.
