import csv, itertools, datetime as dt
from decimal import Decimal as D
ag=[r for r in csv.DictReader(open('agios.tsv',encoding='utf-8'),delimiter='\t')]
ca=[r for r in csv.DictReader(open('calc.tsv',encoding='utf-8'),delimiter='\t')]
def d(s): return dt.date.fromisoformat(s)
def qend(y,q): return [dt.date(y,3,31),dt.date(y,6,30),dt.date(y,9,30),dt.date(y,12,31)][q-1]
def period_of(dv):
    best=None
    for y,q in [(2025,4),(2026,1),(2026,2),(2026,3)]:
        e=qend(y,q); diff=(dv-e).days
        if -8<=diff<=16:
            best=(y,q)
    return best
by={}
for a in ag:
    if a['status']!='PAYE': continue
    dv=d(a['date_valeur']) if a['date_valeur']!='NULL' else d(a['date_emission'])
    e=d(a['date_emission'])
    p=period_of(min(dv,e)) or period_of(max(dv,e))
    by.setdefault((a['nom'],p),[]).append(a)
types=['FACILITE_CAISSE','PREFINANCEMENT','DROIT_CONSTATE']
for (bank,p),lines in sorted(by.items(), key=lambda x:(str(x[0][1]),x[0][0])):
    if p is None: continue
    print(f"\n=== {bank} {p[1] and 'T'+str(p[1])}/{p[0]} — {len(lines)} lignes, total {sum(D(l['montant']) for l in lines):,.2f}")
    for l in lines: print(f"   #{l['id']:>3} {D(l['montant']):>12,.2f}  {l['libelle'][:55]}")
    for t in types:
        for c in ca:
            if c['nom']==bank and c['type_agios']==t and int(c['trimestre'])==p[1] and int(c['annee'])==p[0]:
                tgt=D(c['total_agios_ttc']); tolr=max(D('2'),tgt*D('0.003'))
                found=None
                for n in range(1,4):
                    for sub in itertools.combinations(lines,n):
                        s=sum(D(x['montant']) for x in sub)
                        if abs(s-tgt)<=tolr: found=(sub,s); break
                    if found: break
                tag = ('~ '+' + '.join('#'+x['id'] for x in found[0])+f" = {found[1]:,.2f}") if found else 'AUCUNE ligne correspondante'
                print(f"   calcul {t:16} {c['statut']:7} {tgt:>12,.2f}  -> {tag}")
