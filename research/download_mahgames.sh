#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
BASE="/home/ubuntu/work/nose-fc-14-fc-27"
OUT="$BASE/originals/fc27-mahgames-2026-09-12"
mkdir -p "$OUT"
UA='Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/124 Safari/537.36'
download() {
  local url="$1" dest="$2"
  if [[ -s "$dest" ]]; then
    echo "exists $dest $(stat -c %s "$dest")"
    return 0
  fi
  echo "downloading $dest"
  curl --http1.1 --fail --location --retry 4 --retry-delay 3 --connect-timeout 30 --max-time 7200 --user-agent "$UA" --output "$dest.part" "$url"
  mv -T "$dest.part" "$dest"
  sha256sum "$dest" | tee -a "$OUT/SHA256SUMS"
}
download 'https://www.mediafire.com/file/qyipn5c2vv6j40i/EA+SPORTS+FC' "$OUT/EA_SPORTS_FC.apk"
download 'https://www.mediafire.com/file/1cooc1n8e4gc97g/obb.zip/file' "$OUT/obb.zip"
download 'https://www.mediafire.com/file/c3tqmcd3sbazxmx/Fifa16ModFC27.zip/file' "$OUT/Fifa16ModFC27.zip"
printf '\nDownloaded originals:\n'
find "$OUT" -maxdepth 1 -type f -printf '%f\t%s bytes\n' | sort
