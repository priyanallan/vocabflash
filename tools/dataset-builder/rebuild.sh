#!/usr/bin/env bash
# Rebuild the bundled Dataset from the third-party inputs in input/ (all git-ignored).
#
# Usage:   ./rebuild.sh <dataset-version>          e.g. ./rebuild.sh 2026.10-public.9
# Then:    cp out/dataset.json ../../composeApp/src/commonMain/composeResources/files/dataset.json
#
# Inputs expected in input/  (see README.md for how to obtain and prepare each one):
#   open-wordlist.tsv        level-tagged headwords (lemma<TAB>level) — ph98/german-cefr-wordlist, MIT
#   kaikki-de.jsonl.gz       Kaikki/Wiktextract German extract — CC BY-SA 4.0
#   tatoeba.tsv              prepared deu->eng sentence pairs — CC BY 2.0 FR
#   odenet.tsv               prepared OdeNet relations — CC BY-SA 4.0
#   caseexamples.tsv         Tatoeba-derived simple case examples — CC BY 2.0 FR
#   ud.conllu                Universal Dependencies German GSD — CC BY-SA 4.0
set -euo pipefail

VERSION="${1:?Pass a dataset version, e.g. ./rebuild.sh 2026.10-public.9}"
cd "$(dirname "$0")"
HERE="$PWD"

if [[ ! -f input/open-wordlist.tsv || ! -f input/kaikki-de.jsonl.gz ]]; then
  echo "Missing inputs in $HERE/input/ — see README.md." >&2
  exit 1
fi

../../gradlew -p . --console=plain run -q --args="
  --wordlist $HERE/input/open-wordlist.tsv
  --kaikki   $HERE/input/kaikki-de.jsonl.gz
  --out      $HERE/out
  --tatoeba  $HERE/input/tatoeba.tsv
  --odenet   $HERE/input/odenet.tsv
  --simple-case $HERE/input/caseexamples.tsv
  --ud       $HERE/input/ud.conllu
  --dataset-version $VERSION
"

echo
echo "Bundle it with:"
echo "  cp out/dataset.json ../../composeApp/src/commonMain/composeResources/files/dataset.json"
