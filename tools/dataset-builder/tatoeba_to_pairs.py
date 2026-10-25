#!/usr/bin/env python3
"""
Turn the raw Tatoeba per-language exports into the prepared sentence-pair TSV the
dataset-builder's TatoebaSource expects:

    <german sentence>\t<english sentence>

Inputs (download from https://downloads.tatoeba.org/exports/per_language/):
    deu/deu_sentences.tsv.bz2   id \t lang \t text
    deu/deu-eng_links.tsv.bz2   sentence_id \t translation_id
    eng/eng_sentences.tsv.bz2   id \t lang \t text

Usage:
    python3 tatoeba_to_pairs.py --dir input --out input/tatoeba.tsv [--max-pairs N]

Licence: the sentence pairs are CC BY 2.0 FR — attribution required.
"""
from __future__ import annotations

import argparse
import bz2
from pathlib import Path


def read_sentences(path: Path) -> dict[str, str]:
    """id -> text, from a Tatoeba per-language sentences dump (bz2 or plain)."""
    opener = bz2.open if path.suffix == ".bz2" else open
    sentences: dict[str, str] = {}
    with opener(path, "rt", encoding="utf-8") as handle:
        for line in handle:
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 3:
                sentences[parts[0]] = parts[2]
    return sentences


def read_links(path: Path) -> list[tuple[str, str]]:
    """(german_id, english_id) pairs."""
    opener = bz2.open if path.suffix == ".bz2" else open
    pairs: list[tuple[str, str]] = []
    with opener(path, "rt", encoding="utf-8") as handle:
        for line in handle:
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 2:
                pairs.append((parts[0], parts[1]))
    return pairs


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default="input")
    ap.add_argument("--out", default="input/tatoeba.tsv")
    ap.add_argument("--max-pairs", type=int, default=0, help="0 = no limit")
    args = ap.parse_args()

    base = Path(args.dir)
    deu = read_sentences(base / "deu_sentences.tsv.bz2")
    eng = read_sentences(base / "eng_sentences.tsv.bz2")
    links = read_links(base / "deu-eng_links.tsv.bz2")
    print(f"german sentences: {len(deu)}, english sentences: {len(eng)}, links: {len(links)}")

    written = 0
    with open(args.out, "w", encoding="utf-8") as out:
        for deu_id, eng_id in links:
            de = deu.get(deu_id)
            en = eng.get(eng_id)
            if not de or not en:
                continue
            out.write(f"{de}\t{en}\n")
            written += 1
            if args.max_pairs and written >= args.max_pairs:
                break

    print(f"wrote {written} pairs to {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
