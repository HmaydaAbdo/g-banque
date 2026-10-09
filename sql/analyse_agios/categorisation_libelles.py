import csv,re
from decimal import Decimal as D
ag=list(csv.DictReader(open('agios.tsv',encoding='utf-8'),delimiter='\t'))
def cat(l):
    s=l.lower()
    if 'factoring' in s: return 'Factoring'
    if re.search(r'pr[ée]fi|pr.financement|pr.fi\b',s): return 'Préfinancement'
    if re.search(r'escompte|lcn',s): return 'Escompte LCN'
    if re.search(r'int[eé]r|inetr|int\. debt|d[ée]biteur',s): return 'Intérêts débiteurs (compte)'
    return 'Agios / arrêté de trimestre (non détaillé)'
from collections import defaultdict
g=defaultdict(list)
for a in ag: g[cat(a['libelle'])].append(a)
for k,v in sorted(g.items(), key=lambda x:-sum(D(a['montant']) for a in x[1])):
    banks=sorted({a['nom'] for a in v})
    print(f"{k} | {len(v)} | {sum(D(a['montant']) for a in v):,.2f} | {', '.join(banks)} | PREVU={sum(1 for a in v if a['status']=='PREVU')}")
small=[a for a in ag if D(a['montant'])<=1000]
print('petits (<=1000):',[(a['id'],a['nom'],a['montant']) for a in small])
print('total', len(ag), sum(D(a['montant']) for a in ag))
