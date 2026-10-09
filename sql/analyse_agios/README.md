# Analyse des agios (09/10/2026)

Scripts d'analyse utilisés pour cadrer le chantier « Agios unifiés » (voir `CHANTIER-AGIOS-UNIFIES.md`).
Lecture seule, à lancer sur une **copie** de la base (jamais sur la production).

1. Importer le dump dans un schéma jetable (ex. `gbanque_analysis`).
2. Exporter deux fichiers TSV dans le dossier courant :

```sql
-- agios.tsv
select a.id, b.nom, a.date_emission, a.date_valeur, a.montant, a.status, a.libelle
  from agios a join banque b on b.id=a.banque_id;
-- calc.tsv
select b.nom, t.type_agios, t.trimestre, t.annee, t.total_agios_ttc, t.total_agios_ht, t.statut
  from agios_trimestriel t join banque b on b.id=t.banque_id where t.total_agios_ttc>0;
```

3. `python -I rapprochement_calcule_vs_facture.py` : pour chaque banque et période, associe les lignes
   saisies aux totaux calculés (sous-ensembles de 1 à 3 lignes, tolérance 0,3 % ou 2 MAD).
4. `python -I categorisation_libelles.py` : première catégorisation des lignes par mots-clés du libellé.

Les en-têtes TSV doivent porter les noms de colonnes ci-dessus (export `mysql -B`), encodage UTF-8.
