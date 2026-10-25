#!/usr/bin/env python3
"""
Derive SIMPLE case-usage examples from Tatoeba instead of the (often complex) UD corpus
sentences. Tatoeba is a learner-oriented sentence corpus, so its sentences are short and
comprehensible at A1/A2.

Emits a prepared TSV for the dataset-builder:

    <lemma>\t<case>\t<sentence>\t<gloss>

where case is `accusative` or `dative`, and gloss explains the cue, e.g. `After "mit"`.

Only sentences with an UNAMBIGUOUS case marker are used:
  * dative     : (mit|bei|nach|von|zu|aus|seit|gegenüber) + (dem|der|den) + <noun form>
  * accusative : (für|durch|ohne|gegen|um|bis) + (den|die|das|ein…) + <noun form>
  * accusative : (den) + <masculine noun form>            (direct object)

Sentences longer than --max-chars are skipped, and the shortest per case wins. `--filter`
restricts output to the pipeline's word TSV. Noun forms are matched leniently (common
suffixes plus an umlaut variant) because German nouns inflect.

Usage:
    python3 tatoeba_case_examples.py --pairs input/tatoeba.tsv \
                                     --filter input/wordlist.tsv \
                                     --out input/caseexamples.tsv

Licence: Tatoeba sentence pairs are CC BY 2.0 FR — attribution required.
"""
from __future__ import annotations

import argparse
import re
from pathlib import Path

DATIVE_PREPS = ("mit", "bei", "nach", "von", "zu", "aus", "seit", "gegenüber")
ACCUSATIVE_PREPS = ("für", "durch", "ohne", "gegen", "um", "bis")
DATIVE_ARTICLES = ("dem", "der", "den")
ACCUSATIVE_ARTICLES = ("den", "die", "das", "einen", "eine", "ein")
SUFFIXES = ("", "s", "es", "e", "en", "n", "er", "ern")
UMLAUT = str.maketrans({"a": "ä", "o": "ö", "u": "ü"})


def noun_forms(lemma: str) -> set[str]:
    """Plausible inflected forms of a German noun (suffix + umlaut variants)."""
    variants = {lemma, lemma.translate(UMLAUT)}
    forms: set[str] = set()
    for stem in variants:
        for suffix in SUFFIXES:
            forms.add((stem + suffix).casefold())
    return forms


def sentence_case(sentence: str, lemma: str) -> tuple[str, str] | None:
    """
    Return (case, gloss) only when the case is marked UNAMBIGUOUSLY.

    Bare articles are never trusted: `das Haus` could be nominative or accusative, `der Baum`
    nominative or dative. Only a governing PREPOSITION determines the case safely.
    """
    forms = noun_forms(lemma)
    tokens = re.findall(r"[A-Za-zÄÖÜäöüß-]+|[^\sA-Za-zÄÖÜäöüß-]+", sentence)

    for i, token in enumerate(tokens):
        if token.casefold() not in forms or i < 2:
            continue
        determiner = tokens[i - 1].casefold()
        preposition = tokens[i - 2].casefold()
        if determiner not in DATIVE_ARTICLES + ACCUSATIVE_ARTICLES:
            continue
        if preposition in DATIVE_PREPS:
            return "dative", f'After "{preposition}"'
        if preposition in ACCUSATIVE_PREPS:
            return "accusative", f'After "{preposition}"'
    return None


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--pairs", default="input/tatoeba.tsv")
    ap.add_argument("--filter", default="input/wordlist.tsv")
    ap.add_argument("--out", default="input/caseexamples.tsv")
    ap.add_argument("--max-chars", type=int, default=70)
    args = ap.parse_args()

    targets: dict[str, str] = {}
    for line in Path(args.filter).read_text(encoding="utf-8").splitlines()[1:]:
        parts = line.split("\t")
        if len(parts) >= 3 and parts[2].strip():
            targets[parts[0].strip().casefold()] = parts[0].strip()
    print(f"filter: {len(targets)} nouns")

    best: dict[tuple[str, str], tuple[int, str, str]] = {}  # (lemma,case) -> (len, sentence, gloss)
    with open(args.pairs, encoding="utf-8") as handle:
        for line in handle:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 2:
                continue
            german, _english = parts[0], parts[1]
            if len(german) > args.max_chars:
                continue
            for key in {word.casefold() for word in re.findall(r"[A-ZÄÖÜ][A-Za-zÄÖÜäöüß-]*", german)}:
                lemma = targets.get(key)
                if lemma is None:
                    continue
                found = sentence_case(german, lemma)
                if found is None:
                    continue
                case, gloss = found
                slot = (lemma.casefold(), case)
                current = best.get(slot)
                if current is None or len(german) < current[0]:
                    best[slot] = (len(german), german, gloss)

    with open(args.out, "w", encoding="utf-8") as out:
        for (key, case), (_, sentence, gloss) in best.items():
            out.write(f"{targets[key]}\t{case}\t{sentence}\t{gloss}\n")

    print(f"wrote {len(best)} case examples to {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
