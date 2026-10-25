#!/usr/bin/env python3
"""Genere la table symboles+lignes et l'ajoute en overlay a la fin de l'exe (magic OEFSYM01)."""
import subprocess,sys,struct,re,os
exe,out=sys.argv[1],sys.argv[2]; P='x86_64-w64-mingw32-'
hdr=subprocess.run([P+'objdump','-p',exe],capture_output=True,text=True).stdout
base=int(re.search(r'ImageBase\s+([0-9a-fA-F]+)',hdr).group(1),16)
files=[];fi={}
def fidx(f):
    f=os.path.basename(f)
    if f not in fi: fi[f]=len(files); files.append(f)
    return fi[f]
funcs=[]
for l in subprocess.run([P+'nm','-n','-S','--defined-only',exe],capture_output=True,text=True).stdout.splitlines():
    p=l.split()
    if len(p)==4 and p[2] in 'Tt': funcs.append((int(p[0],16),int(p[1],16),p[3]))
    elif len(p)==3 and p[1] in 'Tt': funcs.append((int(p[0],16),0,p[2]))
a2l=subprocess.run([P+'addr2line','-e',exe]+['%x'%a for a,_,_ in funcs],capture_output=True,text=True).stdout.splitlines()
lines=[]
cur=None
for l in subprocess.run([P+'objdump','--dwarf=decodedline',exe],capture_output=True,text=True).stdout.splitlines():
    m=re.match(r'^(\S+\.[ch])\s+(\d+)\s+(0x[0-9a-f]+)',l)
    if m: lines.append((int(m.group(3),16)-base,fidx(m.group(1)),int(m.group(2))))
F=[]
for (a,s,n),loc in zip(funcs,a2l):
    f,_,ln=loc.rpartition(':'); ln=re.match(r'\d+',ln); 
    F.append((a-base,s,n,fidx(f) if f and f!='??' else -1,int(ln.group()) if ln else 0))
lines.sort()
txt=''.join('S\t%s\n'%f for f in files)+''.join('F\t%x\t%x\t%s\t%d\t%d\n'%x for x in F)+''.join('L\t%x\t%d\t%d\n'%x for x in lines)
b=txt.encode()
data=open(exe,'rb').read()+b+struct.pack('<I',len(b))+b'OEFSYM01'
open(out,'wb').write(data)
print('fonctions=%d lignes=%d fichiers=%d overlay=%d octets'%(len(F),len(lines),len(files),len(b)))
