# Local-first, no accounts or backend — but seamed for a future switch

The app is local-first: **no login, no server, no sync, all progress on-device.** This is a deliberate
architectural "no" that keeps the system tiny, private, and free to run, and it directly serves the
single-user private context (ADR-0002).

However, it is *seamed* rather than *hard-coded*: if openly-licensed content is later adopted and the
app is shared, auth + cross-device sync may be added. So:
- persistence sits behind repository interfaces (local implementation today, remote later);
- Entries have stable IDs independent of any source list;
- the Dataset is source-agnostic and versioned, with CEFR level as data;
- Schedule State and progress are serialisable (also needed for JSON export/import).

This costs almost nothing now and prevents the "just add sync" suggestion from becoming a rewrite.
The trap is over-building speculative multi-user machinery — explicitly out of scope.

## Considered Options
- **Build sync/auth now** — rejected: no users, no backend, no requirement.
- **Ignore the seam entirely** — rejected: the content-licensing situation (ADR-0002) makes a future
  pivot plausible enough to justify the cheap interface boundary.
