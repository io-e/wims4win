#!/usr/bin/env python3
# GROK_V1 - compare the current full-list approach (/programming/list?folder=all + client-side filter)
# with GET /programming/search on the SQLite/FTS5 catalogue. Standard library only.
# Usage: python3 bench_search_GROK_V1.py http://127.0.0.1:8697 poc_mock
import json, os, statistics, subprocess, sys, time, urllib.parse, urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8697"
POC = sys.argv[2] if len(sys.argv) > 2 else "poc_mock"
QUERIES = ["derivative chain rule", "binomial probability", "integration by parts", "eigenvalue matrix",
           "limit", "fraction", "intégrale", "vecteur", "verrerie", "deriv"]


def get(path):
    t = time.perf_counter()
    with urllib.request.urlopen(BASE + path, timeout=300) as r:
        body = r.read()
    return (time.perf_counter() - t) * 1000, body


def main():
    res = {}
    cat = os.path.join(POC, "logs", "programming_catalog_main_GROK_V1.json")
    if os.path.exists(cat):
        os.remove(cat)          # our mock's cache: force the cold disk walk once
    ms, body = get("/programming/list?folder=all")
    res["list_cold_ms"] = round(ms)
    res["list_bytes"] = len(body)
    warm = [get("/programming/list?folder=all")[0] for _ in range(5)]
    res["list_warm_ms_median"] = round(statistics.median(warm))
    lst = os.path.join(POC, "logs", "_list_all.json")
    open(lst, "wb").write(body)
    # client side: JSON.parse + filter by every word on name/path (what a page can do with this list)
    js = r"""
const fs=require('fs');const t0=performance.now();const d=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));
const t1=performance.now();const qs=JSON.parse(process.argv[2]);const out={};
for(const q of qs){const w=q.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').split(/\s+/);
 const s=performance.now();let n=0;for(const it of d.items){const h=((it.name||'')+' '+(it.path||it.key||'')).toLowerCase();
 if(w.every(x=>h.includes(x)))n++;}out[q]={hits:n,ms:+(performance.now()-s).toFixed(1)};}
console.log(JSON.stringify({parse_ms:+(t1-t0).toFixed(1),items:d.items.length,filters:out}));"""
    r = subprocess.run(["node", "-e", js, lst, json.dumps(QUERIES)], capture_output=True, text=True, check=True)
    res["client"] = json.loads(r.stdout)
    os.remove(lst)
    res["search"] = {}
    for q in QUERIES:
        p = "/programming/search?" + urllib.parse.urlencode({"q": q, "per_page": 50})
        times, last = [], None
        for _ in range(10):
            ms, b = get(p)
            times.append(ms)
            last = json.loads(b)
        res["search"][q] = {"total": last["total"], "by_type": last["by_type"], "http_ms_median": round(statistics.median(times), 1),
                            "server_ms": last["ms"], "bytes": len(b), "top": (last["items"][0]["title"] if last["items"] else None)}
    for label, p in (("type=oef q=fraction", "/programming/search?q=fraction&type=oef"),
                     ("no q page 100", "/programming/search?page=100"),
                     ("folder filter", "/programming/search?q=limit&folder=contrib_plus&page=3")):
        times = [get(p)[0] for _ in range(10)]
        res["search"][label] = {"http_ms_median": round(statistics.median(times), 1), "total": json.loads(get(p)[1])["total"]}
    print(json.dumps(res, indent=1, ensure_ascii=False))


main()
