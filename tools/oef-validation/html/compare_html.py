import re,sys,glob,os,difflib
A,B=sys.argv[1],sys.argv[2]
def norm(t):
    t=re.sub(r'mathsize="\d+%"','mathsize=X',t)
    t=re.sub(r'wims_mathml\d+','wims_mathmlN',t); t=re.sub(r'session=[A-Z0-9]+\.?\d*','session=S',t)
    t=re.sub(r'G\u00e9n\u00e9r\u00e9 le [^<]*','GEN',t); t=re.sub(r'data:[^"]+',lambda m:'DATA:'+__import__('hashlib').sha256(m.group(0).encode()).hexdigest()[:12],t)
    t=re.sub(r'\b[0-9A-Z]{10}\b','SID',t); return t
for fa in sorted(glob.glob(A+'/*.html')):
    fb=os.path.join(B,os.path.basename(fa))
    if not os.path.exists(fb): print(os.path.basename(fa),'ABSENT dans',B); continue
    a=norm(open(fa,encoding='utf-8',errors='replace').read()).splitlines()
    b=norm(open(fb,encoding='utf-8',errors='replace').read()).splitlines()
    d=[l for l in difflib.unified_diff(a,b,lineterm='',n=0) if l[:1] in '+-' and l[:3] not in ('+++','---')]
    print(os.path.basename(fa),'IDENTIQUE' if not d else 'DIFF %d lignes'%len(d))
    for l in d[:6]: print('   ',l[:160])
