# Research: German–English dictionary, IPA, and offline German TTS

Scope: build-time data sourcing for a private, offline-only KMP flashcard app.
Constraint: **no network calls at runtime**; build-time API use is permitted.

---

## 1. Bilingual dictionary (DE–EN / EN–DE)

| Source | Type | Format | Licence | Size / coverage | Includes gender, plural, verb forms, examples? |
|---|---|---|---|---|---|
| **Kaikki.org / Wiktextract** (`de-extract.jsonl`) | Open dataset | JSONL (one word-sense per line) | Content **CC BY-SA 4.0**; extractor code MIT | DE: `de-extract.jsonl.gz` **294 MB** (2.9 GB raw); 352,527 distinct word forms; nouns 155k, verbs 144k | ✅ gender (`gen`), plural & full inflection tables, verb forms, IPA, example sentences, translations. Richest source by far. |
| **FreeDict deu-eng / eng-deu** | Open dataset (from Ding) | TEI-P5 XML; also DICT, StarDict, GoldenDict | **GPLv3 + AGPLv3** (combined work; original Ding GPLv2+) | deu-eng: **517,534** headwords (v1.9-fd1); eng-deu: **460,315** | ⚠️ gender (`<gen>`), POS, number; limited/no example sentences, thin inflection. |
| **Ding** (dict.tu-chemnitz.de) | Open dataset | plain text `de-en.txt` | **GPLv2+** | ~326k DE→EN / ~380k EN→DE entries | gender/POS present; no examples. Upstream of FreeDict. |
| **Wiktionary dumps** (de/en) | Open dataset | XML dumps | CC BY-SA 3.0/4.0 + GFDL | de dump large; en Wiktionary richer for DE lemmas | needs parsing (Wiktextract) to be useful. |
| **dict.cc** | Online service (+ "free API") | — | **Proprietary/copyrighted**; free API for personal, non-commercial use only, **no redistribution** | ~1M+ entries | Not redistributable — unusable for bundling. |
| **LEO (dict.leo.org)** | Online service | — | No public API; scraping against ToS | — | Not usable. |
| **Linguee / DeepL** | Online service | REST API | Paid; content DeepL-copyrighted, no redistribution | — | Not usable for bundling. |
| **dictionaryapi.dev** | Runtime API | JSON REST | Free, data from Wiktionary (**CC BY-SA**) | **English only** — no DE↔EN translations | Gives definitions + IPA + audio for EN; not a bilingual dict. |
| **Wiktionary REST API** | Runtime API | JSON | Free, no key; content **CC BY-SA** | multilingual | Runtime-only; ToS + attribution + rate limits. |

**Notes:** Kaikki is the single best bundling candidate (gender, plural, conjugation, examples all machine-readable). FreeDict/Ding is a good *secondary/fallback* and is smaller to ship, but is **GPL/AGPL** — a copyleft concern if the app is ever distributed.

---

## 2. IPA / pronunciation

| Source | Type | Format | Licence | Coverage |
|---|---|---|---|---|
| **WikiPron** (`deu_latn_broad.tsv`) | Open dataset | TSV `word<TAB>IPA` | **Code Apache-2.0**; mined data derived from Wiktionary → treat as **CC BY-SA** for redistribution | **60,277** German word/pronunciation pairs, ~2 MB |
| **Wiktionary IPA** | Open dataset (via Kaikki/Wiktextract or the dump) | JSON / wiki markup | CC BY-SA | broad + narrow, dialect-tagged; embedded in Kaikki entries |
| **Kaikki `de-extract`** | Open dataset | JSONL `sounds[]` | CC BY-SA 4.0 | IPA + audio refs bundled with each lemma |

**Note:** WikiPron is derived from Wiktionary, so even though the tooling is Apache-2.0 the *pronunciation strings* carry CC BY-SA attribution. Kaikki already contains IPA, so IPA can be sourced from the **same Kaikki extract** — no separate pipeline needed.

---

## 3. Offline TTS for German

| Option | Type | Licence | Size | Offline? | KMP feasibility |
|---|---|---|---|---|---|
| **Android `TextToSpeech`** (Google/system engine) | OS API | OS/engine EULA | voice data per language (tens–hundreds MB, user-downloaded) | ✅ offline **after** voice data installed; `Voice.isNetworkConnectionRequired()` tells you. Not guaranteed pre-installed (esp. non-English / emulators). | Native via expect/actual in KMP; no bundling control. |
| **iOS `AVSpeechSynthesizer`** | OS API | OS EULA | compact voices bundled; **enhanced/premium require download** | ✅ offline for installed voices (de-DE compact usually present) | Native via expect/actual; no bundle control; quality varies. |
| **Piper** (rhasspy) | On-device neural engine | Engine **MIT**; `de_DE` voices incl. **thorsten CC0** | low ≈ 20–30 MB, medium ≈ 60 MB; ONNX | ✅ fully offline, CPU | ✅ realistic on Android/iOS via **ONNX Runtime** (or sherpa-onnx). Best quality/size trade-off. |
| **eSpeak-NG** | On-device formant synth | **GPL-3.0** | few MB total (very compact) | ✅ offline | Android JNI port exists; iOS possible but **GPL** applies to linked code — copyleft risk. Robotic quality. |
| **Coqui TTS (XTTS v2)** | On-device neural | Engine MPL-2.0; **XTTS weights = CPML, non-commercial, non-redistributable** | large (100s MB+) | ✅ | Heavy for mobile; **licence blocks redistribution** — avoid. |

**Notes:** system TTS is zero-cost but unreliable for guaranteed offline German (user may have no de voice installed). Piper + CC0 German voices gives a self-contained, permissively-licensed offline option.

---

## 4. Can an online API replace bundling?

**No.** The app is fully offline at runtime, so **no runtime API is viable** (Wiktionary REST, dictionaryapi.dev, dict.cc API, DeepL/Linguee all require network, carry attribution/share-alike or proprietary terms, and some prohibit redistribution).

**Where build-time APIs make sense:**
- Pull the **Kaikki `de-extract.jsonl.gz`** once at build time → derive gender/plural/conjugation/examples/IPA into a compressed SQLite/JSON bundle.
- Optionally run the **Wiktionary REST / Wiktextract** tooling at build time to fill gaps, but Kaikki already pre-extracts it.
- Fetch **Piper de_DE voice** (CC0) and ship it (or bundle-audio vs on-demand — but on-demand = network, disallowed).

---

## Recommendation

**Bundle:**
1. **Kaikki German extract** (CC BY-SA 4.0) as the primary dictionary — only source covering gender, plural, verb forms, examples and IPA in one machine-readable file. Trim to the A1–B1 vocabulary + needed fields at build time to shrink 294 MB → a few MB.
2. **Piper `de_DE` voice (thorsten, CC0)** + **MIT** ONNX Runtime for offline TTS.
3. Fall back to **system TTS** (Android/iOS) when no bundled voice is present.

**Fetch at build time:**
- Kaikki `de-extract.jsonl.gz`; optionally WikiPron TSV (60k pairs, 2 MB) to supplement IPA; regenerate on each release.

**Runtime APIs:** none — use at build time only.

**Licence caveats (matter if the private app is ever shared):**
- **CC BY-SA 4.0** (Kaikki/Wiktionary/WikiPron data): **share-alike** — a redistributed derivative must be licensed CC BY-SA and attributed.
- **GPLv3/AGPLv3** (FreeDict deu-eng; derived from Ding GPLv2+): strong **copyleft**; linking into the app can impose GPL obligations — avoid if you may distribute.
- **GPL-3.0** (eSpeak-NG): copyleft; precautionary for App-Store distribution.
- **CPML** (Coqui XTTS): **non-commercial, non-redistributable** — do not bundle.
- **dict.cc / LEO / Linguee / DeepL**: proprietary — no redistribution.
- **Piper engine MIT + thorsten voice CC0**: permissive — safe to bundle and redistribute.

### Sources
- Kaikki rawdata: https://kaikki.org/dictionary/rawdata.html
- Kaikki German: https://kaikki.org/dictionary/German/index.html
- Wiktextract: https://github.com/tatuylonen/wiktextract
- FreeDict downloads: https://freedict.org/downloads/
- FreeDict TEI header (deu-eng licence): https://download.freedict.org/dictionaries/deu-eng/1.9-fd1/
- Ding: https://www-user.tu-chemnitz.de/~fri/ding/
- WikiPron: https://github.com/CUNY-CL/wikipron
- freeDictionaryAPI: https://dictionaryapi.dev/
- Wiktionary REST API: https://en.wiktionary.org/wiki/User:Amgine/Wiktionary_data_&_API
- Piper: https://github.com/rhasspy/piper
- Piper German voice model card (thorsten, CC0): https://huggingface.co/rhasspy/piper-voices
- eSpeak-NG: https://github.com/espeak-ng/espeak-ng
- Coqui XTTS licence (CPML): https://huggingface.co/coqui/XTTS-v2/blob/main/LICENSE.txt
- Android TextToSpeech: https://developer.android.com/reference/android/speech/tts/TextToSpeech
