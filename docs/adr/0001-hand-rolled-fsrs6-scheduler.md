# Hand-rolled FSRS-6 scheduler instead of `fsrs-rs` bindings

We schedule reviews with **FSRS-6, implemented from the specification in Kotlin `commonMain`**, rather
than binding the mature Rust reference implementation `fsrs-rs` (via UniFFI/JNI + cinterop) or
depending on an existing Kotlin port.

The scheduler core is small and fully deterministic (~200–400 LOC of pure functions over a fixed list
of 21 parameters), so writing it is less work than wiring up cross-platform native toolchains, and it
yields identical behaviour on Android and iOS with no binary-size or CI cost. Crucially, `fsrs-rs`'s
main advantage is its ML parameter *optimiser*, which a single-user offline app does not need. This
also supports the project's learning goal (ADR-0004) and avoids the GPL-3.0 obligation of the one
true-KMP Kotlin port (`FsrsKt`), which would matter if the app were ever published.

## Consequences
- Correctness must be validated against reference vectors from `fsrs-rs`/`ts-fsrs` (e.g. `R(S,S)=0.9`).
- Each Card persists `difficulty`, `stability`, `state`, `due`, `last_review`, `reps`, `lapses`;
  retrievability is computed, never stored.
- If the ML optimiser is ever wanted, it can be added later as a separate offline step.

## Considered Options
- **`fsrs-rs` via UniFFI/JNI + cinterop** — rejected: days–weeks of native build work for no benefit
  without the optimiser.
- **Vendor MIT `FSRS-Kotlin`** — viable fallback if time-boxed; not chosen because hand-rolling is a
  deliberate learning objective.
