# Project purpose: learning Compose Multiplatform first, vocabulary app second

This project exists **primarily as a vehicle for learning Compose Multiplatform (KMP) and mobile
development**, and secondarily as a genuinely useful German vocabulary app. Where a choice exists
between "the fastest shipped result" and "the path that teaches the most," we favour learning —
concretely, hand-rolling core logic (e.g. the FSRS scheduler, ADR-0001) rather than pulling in a
library that hides the mechanics, and treating the repo as a teaching artifact.

This meta-decision resolves lower-level tie-breaks and explains choices that would look odd to someone
optimising purely for velocity. It also sets explicit non-goals: publishing, monetisation, multi-user
support, backend/sync, and exam-certification fidelity.

## Consequences
- Learning priorities, highest to lowest: Compose UI/state; KMP shared logic & expect/actual; iOS
  interop; Gradle/KMP project structure; DI; navigation; persistence; testing.
- Target iOS early (so interop is learned, not bolted on).
- Unit tests are kept proportionate — with the exception of the pure, high-risk FSRS scheduler.
