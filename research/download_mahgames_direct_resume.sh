#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
BASE="/home/ubuntu/work/nose-fc-14-fc-27"
OUT="$BASE/originals/fc27-mahgames-2026-09-12"
UA='Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/124 Safari/537.36'
fetch_direct() {
  local page="$1" dest="$2"
  local url
  url=$(grep -aEo 'https://download[0-9]+\.mediafire\.com/[^"< ]+' "$page" | head -n 1 || true)
  [[ -n "$url" ]] || { echo "No direct URL in $page" >&2; return 1; }
  local part="$dest.part"
  local attempt=1
  while :; do
    echo "$(basename "$dest") attempt $attempt; current $(stat -c %s "$part" 2>/dev/null || echo 0) bytes"
    if curl --http1.1 --fail --location --retry 3 --retry-delay 3 --connect-timeout 30 --max-time 7200 --continue-at - --user-agent "$UA" --output "$part" "$url"; then break; fi
    attempt=$((attempt+1)); [[ "$attempt" -le 8 ]] || exit 1; sleep 5
  done
  mv -T "$part" "$dest"
  file "$dest"
  sha256sum "$dest" | tee -a "$OUT/SHA256SUMS.direct"
}
[[ -s "$OUT/EA_SPORTS_FC_MOBILE_BETA.apk" ]] || fetch_direct "$OUT/EA_SPORTS_FC.apk" "$OUT/EA_SPORTS_FC_MOBILE_BETA.apk"
[[ -s "$OUT/obb.direct.zip" ]] || fetch_direct "$OUT/obb.zip" "$OUT/obb.direct.zip"
[[ -s "$OUT/Fifa16ModFC27.direct.zip" ]] || fetch_direct "$OUT/Fifa16ModFC27.zip" "$OUT/Fifa16ModFC27.direct.zip"
find "$OUT" -maxdepth 1 -type f -printf '%f\t%s bytes\n' | sort
