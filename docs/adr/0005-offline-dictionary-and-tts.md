# Bundle an openly-licensed dictionary + on-device TTS; no runtime APIs

To fill the gaps the Goethe Wortliste leaves (article, plural, verb forms, examples, pronunciation),
the app **bundles an offline Dictionary built from the Kaikki/Wiktextract German extract**, plus
**on-device speech**. No runtime API is used — the app is offline-only (D4).

Kaikki (CC BY-SA 4.0) is the only source that supplies **all** of: gender/article, full inflection,
verb forms, example sentences and IPA in one place, and it can be trimmed to a few MB for A1–B1.
FreeDict/Ding were rejected (GPL/AGPL copyleft and weaker data); dict.cc/LEO/Linguee/DeepL were
rejected (proprietary, no redistribution); dictionaryapi.dev is English-only. Runtime dictionary APIs
were rejected outright because the app must work without a network — APIs are used **at build time
only**, to fetch and trim Kaikki.

For audio, **native OS TTS** (Android `TextToSpeech`, iOS `AVSpeechSynthesizer`) via `expect/actual`
costs 0 MB and is the default. An optional **Piper** engine (MIT) with the CC0 `de_DE` thorsten voice
can be bundled (~20–60 MB) where fully-offline, higher-quality speech is wanted. eSpeak (GPL-3.0) and
Coqui XTTS weights (non-commercial) were rejected.

## Consequences
- A **build step** fetches Kaikki and emits a trimmed, bundled Dictionary (SQLite/JSONL); it never runs
  at app runtime.
- Entry→headword **matching** happens at build time, not while studying.
- **Attribution screen** required; **CC BY-SA is share-alike** — matters if the app is ever shared
  (see ADR-0002).
- TTS may require the German voice to be installed once on the device.

## Considered Options
- **Runtime dictionary API** — rejected: needs network, contrary to offline-first.
- **FreeDict / Ding** — rejected: GPL/AGPL copyleft, no examples, weaker inflection.
- **Coqui / eSpeak TTS** — rejected: non-commercial weights / GPL-3.0.
