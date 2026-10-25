#!/usr/bin/env python3
"""
Turn the OdeNet (Open German WordNet) LMF XML into the prepared relation TSV the
dataset-builder's OdeNetSource expects:

    <lemma>\t<relation>\t<related lemma>

relation is one of: broader (hypernym), narrower (hyponym), similar (co-hyponym/synonym).

Input:  odenet_oneline.xml  (https://github.com/hdaSprachtechnologie/odenet)
Usage:  python3 odenet_to_relations.py --xml input/odenet_oneline.xml \
                                        --out input/odenet.tsv \
                                        [--filter input/wordlist.tsv] [--max-related 6]

NOTE: `odenet_oneline.xml` is not well-formed XML (some LexicalEntry elements carry a stray
`</Sense>`), so this uses tolerant regex extraction rather than an XML parser.

`--filter` limits output to lemmas present in the pipeline's word TSV (first column).

Licence: OdeNet is CC BY-SA 4.0 — attribution + share-alike.
"""
from __future__ import annotations

import argparse
import re
from collections import defaultdict
from pathlib import Path

HYPERNYM = {"hypernym", "instance_hypernym"}
HYPONYM = {"hyponym", "instance_hyponym"}
SIMILAR = {"similar", "other", "also", "has_synonym"}

LEXICAL_ENTRY = re.compile(r"<LexicalEntry\b[^>]*>(.*?)</LexicalEntry>")
SENSE_SYNSET = re.compile(r"<Sense\b[^>]*?synset\s*=\s*[\"']([^\"']+)[\"']")
LEMMA_FORM = re.compile(r"<Lemma\b[^>]*?writtenForm\s*=\s*[\"']([^\"']+)[\"']")
SYNSET_ID = re.compile(r"<Synset\b[^>]*?\bid\s*=\s*[\"']([^\"']+)[\"']")
SYNSET_REL_TAG = re.compile(r"<SynsetRelation\b[^>]*>")
ATTR = re.compile(r"(\w+)\s*=\s*[\"']([^\"']*)[\"']")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--xml", default="input/odenet_oneline.xml")
    ap.add_argument("--out", default="input/odenet.tsv")
    ap.add_argument("--filter", default="")
    ap.add_argument("--max-related", type=int, default=6)
    ap.add_argument("--max-tokens", type=int, default=2, help="max words in a related term")
    ap.add_argument(
        "--include-hierarchical",
        action="store_true",
        help="also emit Princeton-WordNet hypernym/hyponym links (NOISY — off by default)",
    )
    ap.add_argument(
        "--all-words",
        action="store_true",
        help="allow related words outside the filter list (default: keep them within it)",
    )
    ap.add_argument(
        "--all-synsets",
        action="store_true",
        help="use every synset of a word instead of only the tightest (noisier)",
    )
    args = ap.parse_args()

    targets: set[str] | None = None
    if args.filter:
        targets = {
            line.split("\t")[0].strip().casefold()
            for line in Path(args.filter).read_text(encoding="utf-8").splitlines()[1:]
            if line.strip()
        }
        print(f"filter: {len(targets)} target lemmas")

    lemma_synsets: dict[str, set[str]] = defaultdict(set)
    lemma_display: dict[str, str] = {}
    synset_lemmas: dict[str, set[str]] = defaultdict(set)
    hypernyms: dict[str, set[str]] = defaultdict(set)   # synset -> its hypernyms
    children: dict[str, set[str]] = defaultdict(set)    # synset -> its hyponyms
    similar: dict[str, set[str]] = defaultdict(set)     # synset -> similar/other

    with open(args.xml, encoding="utf-8") as handle:
        for line in handle:
            if "<LexicalEntry" in line:
                entry = LEXICAL_ENTRY.search(line)
                if not entry:
                    continue
                body = entry.group(1)
                form = LEMMA_FORM.search(body)
                if not form:
                    continue
                lemma = form.group(1)
                key = lemma.casefold()
                lemma_display.setdefault(key, lemma)
                for sid in SENSE_SYNSET.findall(body):
                    lemma_synsets[key].add(sid)
                    synset_lemmas[sid].add(lemma)
            elif "<Synset " in line:
                sid_match = SYNSET_ID.search(line)
                if not sid_match:
                    continue
                sid = sid_match.group(1)
                for tag in SYNSET_REL_TAG.findall(line):
                    attrs = dict(ATTR.findall(tag))
                    rel = attrs.get("relType", "").lower()
                    target = attrs.get("target")
                    if not target:
                        continue
                    if rel in HYPERNYM:
                        hypernyms[sid].add(target)
                        children[target].add(sid)
                    elif rel in HYPONYM:
                        children[sid].add(target)
                        hypernyms[target].add(sid)
                    elif rel in SIMILAR:
                        similar[sid].add(target)

    print(f"lemmas: {len(lemma_synsets)}, synsets: {len(synset_lemmas)}")

    written = 0
    with open(args.out, "w", encoding="utf-8") as out:
        for key, sids in lemma_synsets.items():
            if targets is not None and key not in targets:
                continue
            display = lemma_display.get(key, key)
            rows: list[tuple[str, str]] = []

            # Prefer the tightest synonym group: for polysemous words it is almost always the
            # most specific/common sense (Stuhl → Sitz, not the toilet sense).
            chosen = (
                sorted(sids, key=lambda s: len(synset_lemmas.get(s, ())))[:1]
                if not args.all_synsets
                else sids
            )
            for sid in chosen:
                rows += [("similar", w) for w in synset_lemmas.get(sid, ())]
                if args.include_hierarchical:
                    for hyper in hypernyms.get(sid, ()):
                        rows += [("broader", w) for w in synset_lemmas.get(hyper, ())]
                        for sibling in children.get(hyper, ()):   # co-hyponyms
                            if sibling != sid:
                                rows += [("similar", w) for w in synset_lemmas.get(sibling, ())]
                    for child in children.get(sid, ()):
                        rows += [("narrower", w) for w in synset_lemmas.get(child, ())]
                    for other in similar.get(sid, ()):
                        rows += [("similar", w) for w in synset_lemmas.get(other, ())]

            seen: set[str] = set()
            count = 0
            for relation, word in rows:
                normalised = word.casefold()
                if normalised == key or normalised in seen:
                    continue
                if len(word.split()) > args.max_tokens:
                    continue  # keep related words short enough to study
                if not args.all_words and targets is not None and normalised not in targets:
                    continue  # keep related words within vocabulary the learner is studying
                if count >= args.max_related:
                    break
                seen.add(normalised)
                out.write(f"{display}\t{relation}\t{word}\n")
                count += 1
                written += 1

    print(f"wrote {written} relations to {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
