#!/usr/bin/env python3
"""Regenerate tools/msys2/msys2-pins.txt from an extracted MSYS2 msys.db.
Usage: resolve-pins.py <extracted msys.db dir> > msys2-pins.txt
Only used when deliberately upgrading the pinned MSYS2 shell toolset."""
import os,re,sys
DB=sys.argv[1]
ROOTS=["msys2-runtime","bash","coreutils","sed","grep","gawk","findutils","tar",
       "gzip","diffutils","which","wget","unzip","bzip2","xz","ca-certificates","perl"]
SKIP={"sh","less","bash-completion","libcurl-devel"}
pk={}
for d in os.listdir(DB):
    t=open(os.path.join(DB,d,'desc'),encoding='utf-8').read()
    f={}
    for m in re.finditer(r'%(\w+)%\n(.*?)(?:\n\n|\Z)',t,re.S): f[m.group(1)]=m.group(2).split('\n')
    pk[f['NAME'][0]]=f
prov={}
for n,f in pk.items():
    for p in f.get('PROVIDES',[]): prov.setdefault(re.split('[<>=]',p)[0],n)
seen=[];todo=list(ROOTS)
while todo:
    n=re.split('[<>=]',todo.pop(0))[0]
    if n in SKIP: continue
    n=n if n in pk else prov.get(n,n)
    if n in seen or n in SKIP: continue
    if n not in pk: sys.exit('missing '+n)
    seen.append(n); todo+=pk[n].get('DEPENDS',[])
print("# name version filename sha256 license  (MSYS2 msys/x86_64, https://repo.msys2.org/msys/x86_64/)")
for n in sorted(seen):
    f=pk[n]; print(n,f['VERSION'][0],f['FILENAME'][0],f['SHA256SUM'][0],'|'.join(f.get('LICENSE',['?'])))
