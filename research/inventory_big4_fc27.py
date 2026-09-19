#!/usr/bin/env python3
from __future__ import annotations
import csv, json, re, struct, sys
from pathlib import Path


def cstring(f, limit=4096):
    b=bytearray()
    while len(b)<limit:
        x=f.read(1)
        if not x or x==b'\0': break
        b.extend(x)
    return b.decode('latin-1','replace')

def classify(name):
    n=name.casefold().replace('\\','/')
    groups={
      'menu':('menu','frontend','front_end','mainmenu','home','cro_','fui'),
      'hud':('hud','overlay','ingame','in_game','mainbe'),
      'scoreboard':('scoreboard','score','clock','timer','full_time','halftime'),
      'background':('background','backdrop','bg_','splash','loading'),
      'button':('button','btn','selector','tab'),
      'icon':('icon','glyph','crest','badge','icons'),
      'ui_layout':('.apt','.const','.layout','.coords'),
      'ui_image':('.tga','.png','.dds','.fsh','.rx2','.sm2'),
      'texture':('texture','tex','image','sprite','atlas'),
    }
    return [k for k,terms in groups.items() if any(t in n for t in terms)]

def parse(path):
    size=path.stat().st_size
    with path.open('rb') as f:
        sig=f.read(4)
        archive_size=struct.unpack('<I',f.read(4))[0]
        count=struct.unpack('>I',f.read(4))[0]
        header_size=struct.unpack('>I',f.read(4))[0]
        members=[]
        valid_header=16 <= header_size <= size and 0 < count <= 500000
        if valid_header:
            for i in range(count):
                if f.tell()+8>header_size: break
                off=struct.unpack('>I',f.read(4))[0]
                length=struct.unpack('>I',f.read(4))[0]
                name=cstring(f)
                members.append({'index':i,'name':name,'offset':off,'size_bytes':length,'range_valid':off+length<=size,'classification':classify(name)})
        return {'path':str(path),'size_bytes':size,'signature':sig.decode('ascii','replace'),'archive_size_le':archive_size,'declared_file_count':count,'declared_header_size':header_size,'valid_header':valid_header,'parsed_member_count':len(members),'members':members}

def main():
    if len(sys.argv)!=3: raise SystemExit('usage: script INPUT_DIR OUTPUT_DIR')
    inp=Path(sys.argv[1]); out=Path(sys.argv[2]); out.mkdir(parents=True,exist_ok=True)
    records=[parse(p) for p in sorted(inp.rglob('*.obb'))]
    (out/'big4_manifest.json').write_text(json.dumps(records,indent=2,ensure_ascii=False),encoding='utf-8')
    with (out/'big4_members.csv').open('w',newline='',encoding='utf-8') as h:
        w=csv.DictWriter(h,fieldnames=['container','index','name','offset','size_bytes','range_valid','classification'])
        w.writeheader()
        for r in records:
            for m in r['members']:
                w.writerow({'container':Path(r['path']).name,**{k:m[k] for k in ['index','name','offset','size_bytes','range_valid']},'classification':';'.join(m['classification'])})
    for r in records:
        print(f"{Path(r['path']).name}: {r['signature']} {r['size_bytes']} bytes; declared={r['declared_file_count']} parsed={r['parsed_member_count']} header={r['declared_header_size']}")
        for m in r['members']:
            if m['classification'] or re.search(r'(?i)(\.big$|\.apt$|\.const$|\.hud$|icons|menu|score|background|texture|sprite)',m['name']):
                print(f"  {m['name']} [{','.join(m['classification'])}]")
    print(f'Wrote {out}/big4_manifest.json and {out}/big4_members.csv')
if __name__=='__main__': main()
