#!/usr/bin/env python3
from pathlib import Path
import subprocess, sys, collections, re
zip_path=Path(sys.argv[1]); out=Path(sys.argv[2]); out.mkdir(parents=True,exist_ok=True)
raw=subprocess.check_output(['unzip','-Z1',str(zip_path)],text=True,errors='replace')
paths=[x for x in raw.splitlines() if x and not x.endswith('/')]
patterns={
 'main_menu':r'MainMenu|mainmenu|menu',
 'background':r'background|Bg_|Background|splash',
 'face':r'face|head|Facial|Face',
 'kit':r'kit|uniform|jersey',
 'player':r'player|Player|players',
 'roster':r'roster|squad|Squad|lineup|formation',
 'hud':r'hud|HUD|score|Score|clock|timer',
 'texture':r'\.(png|jpg|jpeg|dds|tga|fsh|rx2|sm2)$',
 'big':r'\.big$',
 'database':r'\.db$|\.dat$|\.ini$|\.bin$',
}
for label,pat in patterns.items():
 vals=[p for p in paths if re.search(pat,p)]
 (out/f'{label}-paths.txt').write_text('\n'.join(vals)+'\n',encoding='utf-8')
 print(label,len(vals))
# first two meaningful path components
c=collections.Counter()
for p in paths:
 parts=p.split('/')
 if len(parts)>=3: c['/'.join(parts[:3])+'/']+=1
(out/'directory-counts.tsv').write_text('\n'.join(f'{n}\t{k}' for k,n in c.most_common())+'\n',encoding='utf-8')
print('files',len(paths))
