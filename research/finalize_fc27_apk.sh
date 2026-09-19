#!/usr/bin/env bash
set -Eeuo pipefail
REPO=/home/ubuntu/work/nose-fc-14-fc-27
APK_SRC="$REPO/launcher/app/build/outputs/apk/debug/app-debug.apk"
OUT="$REPO/release-assets/fc27-launcher-0.2.0"
mkdir -p "$OUT"
cp -f "$APK_SRC" "$OUT/FC27.apk"
SHA=$(sha256sum "$OUT/FC27.apk" | awk '{print $1}')
SIZE=$(stat -c '%s' "$OUT/FC27.apk")
APKSIGNER=/home/ubuntu/work/android-toolchain/build-tools/35.0.0/apksigner
ZIPALIGN=/home/ubuntu/work/android-toolchain/build-tools/35.0.0/zipalign
if [ -x "$ZIPALIGN" ]; then "$ZIPALIGN" -c -v 4 "$OUT/FC27.apk"; fi
if [ -x "$APKSIGNER" ]; then "$APKSIGNER" verify --verbose "$OUT/FC27.apk"; fi
printf '%s  %s\n' "$SHA" "FC27.apk" > "$OUT/FC27.apk.sha256"
cat > "$REPO/launcher/update.json" <<EOF
{
  "version": "0.2.0",
  "apk_url": "https://github.com/ayoubnoob543-lab/nose-fc-14-fc-27/releases/download/fc27-launcher-0.2.0/FC27.apk",
  "sha256": "$SHA",
  "size_bytes": $SIZE,
  "mandatory": false,
  "notes": "Primera versión del launcher FC 27 con descarga reanudable, verificación de archivos y preparación automática."
}
EOF
cat > "$REPO/research/fc27-launcher-0.2.0.json" <<EOF
{
  "name": "FC27.apk",
  "version": "0.2.0",
  "size_bytes": $SIZE,
  "sha256": "$SHA",
  "build_variant": "debug-signed test build",
  "min_sdk": 23,
  "target_sdk": 35
}
EOF
printf 'APK=%s\nSIZE=%s\nSHA256=%s\n' "$OUT/FC27.apk" "$SIZE" "$SHA"
