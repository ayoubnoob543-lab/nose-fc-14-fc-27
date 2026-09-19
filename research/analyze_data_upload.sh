#!/usr/bin/env bash
set -Eeuo pipefail
SRC=/home/ubuntu/upload/Fifa16ModFC27.zip
BASE=/home/ubuntu/work/nose-fc-14-fc-27
OUT="$BASE/originals/fc27-mahgames-upload-2026-09-12"
R="$BASE/research/fc27-data-upload"
mkdir -p "$OUT" "$R"
stat -c 'source=%n size=%s bytes' "$SRC"
file "$SRC"
cp -p "$SRC" "$OUT/Fifa16ModFC27.zip"
stat -c 'copy=%n size=%s bytes' "$OUT/Fifa16ModFC27.zip"
sha256sum "$OUT/Fifa16ModFC27.zip" | tee "$R/data.sha256"
unzip -Z1 "$OUT/Fifa16ModFC27.zip" > "$R/data-members.txt"
head -n 160 "$R/data-members.txt" > "$R/data-top-list.txt"
printf 'member_count='; wc -l < "$R/data-members.txt"
printf '\nTop-level paths:\n'; awk -F/ 'NF>1{print $1"/"}' "$R/data-members.txt" | sort -u | tee "$R/data-top-level.txt"
printf '\nCandidate content paths:\n'; rg -i 'com\.ea|fifaworld|data/|obb/|assets/|face|kit|player|squad|roster|fifa|fc27' "$R/data-members.txt" | head -n 300 > "$R/data-content-candidates.txt" || true
unzip -t "$OUT/Fifa16ModFC27.zip" > "$R/data-zip-test.txt"
tail -n 5 "$R/data-zip-test.txt"
