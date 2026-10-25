> **Status (2026-10-25): historical.** This ADR justified using the Goethe Wortliste only
> because the app was private. The repository is now public, so that basis no longer holds —
> see `docs/adr/0007-public-repo-open-data.md`. No Goethe-derived data ships in the public history.

# Use the copyright-protected Goethe Wortliste because the app stays private

The vocabulary Dataset is built from the **official Goethe-Institut Wortliste (A1, A2, B1)** — PDFs
that carry an explicit all-rights-reserved notice ("Jede Verwertung … bedarf der vorherigen
schriftlichen Einwilligung des Goethe-Instituts"). Using and bundling this content is lawful here
**only because the app is private, offline, and never distributed**. This is a deliberate, load-bearing
constraint, not an oversight.

The alternatives were to build on openly-licensed data (e.g. Kaikki/Wiktextract, CC BY-SA) with
*approximate* CEFR levels, or to request a written licence from Goethe. Neither was chosen: the open
data lacks exam-official fidelity, and a licence request adds a legal dependency and delay for a
single-user tool. See `research-cefr-wordlists.md`.

## Escape hatch (important)
**If the app is ever published or shared, this decision is void** and the Dataset must switch to an
openly-licensed source. D26 keeps that switch cheap (source-agnostic, versioned Dataset; CEFR level as
data; stable IDs), so the licence change would not force a data-model rewrite.

## Consequences
- No redistribution of the extracted word content, ever, under the current decision.
- The Goethe B2 list has no free standalone PDF, so B2 content is deferred separately.
