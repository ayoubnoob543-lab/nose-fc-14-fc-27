#!/usr/bin/env bash
set -Eeuo pipefail
BASE=/home/ubuntu/work/nose-fc-14-fc-27
SRC="$BASE/originals/fc27-mahgames-upload-2026-09-12/Fifa16ModFC27.zip"
OUT="$BASE/release-assets/fc27-mahgames-2026-09-12/data-parts"
mkdir -p "$OUT"
rm -f "$OUT"/Fifa16ModFC27.zip.part-*
split -b 1800M -d -a 2 "$SRC" "$OUT/Fifa16ModFC27.zip.part-"
python3 - "$SRC" "$OUT" <<'PY'
import hashlib, json, pathlib, sys
src=pathlib.Path(sys.argv[1]); out=pathlib.Path(sys.argv[2])
def h(p):
 d=hashlib.sha256()
 with p.open('rb') as f:
  while True:
   b=f.read(1024*1024)
   if not b: break
   d.update(b)
 return d.hexdigest()
parts=sorted(out.glob('Fifa16ModFC27.zip.part-*'))
manifest={'original_name':src.name,'original_size':src.stat().st_size,'original_sha256':h(src),'part_size_bytes':1800*1000*1000,'parts':[{'name':p.name,'size':p.stat().st_size,'sha256':h(p)} for p in parts],'reassemble':'cat Fifa16ModFC27.zip.part-* > Fifa16ModFC27.zip'}
(out/'DATA_PARTS_MANIFEST.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print(json.dumps(manifest,indent=2))
PY
