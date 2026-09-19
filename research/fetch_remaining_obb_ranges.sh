#!/usr/bin/env bash
set -Eeuo pipefail
BASE=/home/ubuntu/work/nose-fc-14-fc-27/originals/fc27-mahgames-2026-09-12
PART="$BASE/obb.direct.zip.part"
PAGE="$BASE/obb.zip"
URL=$(grep -aEo 'https://download[0-9]+\.mediafire\.com/[^"< ]+' "$PAGE" | head -n 1)
TOTAL=1425685730
CHUNK=$((10*1024*1024))
START=$(stat -c %s "$PART")
[[ "$START" -lt "$TOTAL" ]] || { echo "already complete"; exit 0; }
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
while [[ "$START" -lt "$TOTAL" ]]; do
  END=$((START+CHUNK-1)); [[ "$END" -lt "$TOTAL" ]] || END=$((TOTAL-1))
  EXPECTED=$((END-START+1))
  echo "range $START-$END ($EXPECTED bytes)"
  ok=0
  for attempt in 1 2 3 4 5; do
    rm -f "$TMP/chunk"
    if curl --http1.1 --fail --silent --show-error --connect-timeout 30 --max-time 300 --retry 1 -H "Range: bytes=${START}-${END}" -o "$TMP/chunk" "$URL"; then
      GOT=$(stat -c %s "$TMP/chunk")
      if [[ "$GOT" -eq "$EXPECTED" ]]; then ok=1; break; fi
      echo "wrong chunk length: got $GOT expected $EXPECTED" >&2
    else echo "range attempt $attempt failed" >&2; fi
    sleep 3
  done
  [[ "$ok" -eq 1 ]] || exit 1
  cat "$TMP/chunk" >> "$PART"
  START=$((END+1))
  DONE=$((START))
  printf 'progress %s/%s bytes (%s%%)\n' "$DONE" "$TOTAL" "$((DONE*100/TOTAL))"
done
mv -T "$PART" "$BASE/obb.direct.zip"
file "$BASE/obb.direct.zip"
sha256sum "$BASE/obb.direct.zip" | tee -a "$BASE/SHA256SUMS.direct"
