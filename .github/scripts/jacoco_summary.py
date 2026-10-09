"""Affiche la couverture globale d'un rapport JaCoCo XML au format Markdown (résumé de job GitHub Actions)."""
import os
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1]
print("### Couverture backend (JaCoCo)")
if not os.path.exists(path):
    print("Rapport absent (les tests ont échoué avant sa génération).")
else:
    for counter in ET.parse(path).getroot().findall("counter"):
        covered, total = int(counter.get("covered")), int(counter.get("missed")) + int(counter.get("covered"))
        label = counter.get("type").title()
        print(f"- {label} : {100 * covered / total:.1f} % ({covered}/{total})" if total else f"- {label} : n/a")
