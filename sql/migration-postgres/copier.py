"""Copie toutes les données d'une base MySQL 8 (export de production chargé) vers le schéma PostgreSQL 18 créé par Flyway.

Exécuté DANS un conteneur Python par migrer_vers_postgres.py (ne pas lancer directement).
Variables : SRC_HOST, SRC_PASSWORD, SRC_DB (MySQL source, root) ; PGHOST, PGDATABASE, PGUSER, PGPASSWORD (cible).

Règles :
  * la cible doit avoir été créée par Flyway (flyway_schema_history présente) et n'avoir aucun utilisateur ; les
    configurations par défaut créées au 1er démarrage du backend sont remplacées par celles de la production ;
  * une table/colonne présente côté source mais absente côté cible est signalée et ignorée (colonne abandonnée) ;
  * une colonne NOT NULL côté cible absente de la source arrête tout (rien n'est validé) ;
  * tout se fait dans UNE transaction : en cas d'erreur, la cible reste vide ;
  * clés étrangères désactivées pendant la copie (session_replication_role = replica), contraintes CHECK actives ;
  * compteurs d'identifiants, séquence Envers et séquences Spring Batch recalés ;
  * contrôle final : nombre de lignes identique table par table, sinon échec.
"""
import datetime
import os
import sys

import psycopg
import pymysql

UTC = datetime.timezone.utc
SKIP_SOURCE = {'flyway_schema_history'}            # historique Flyway MySQL : propre à l'ancien moteur
SEQUENCE_TABLES = {                                # tables de séquence MySQL -> séquences PostgreSQL
    'audit_revision_entity_seq': None,             # remplacée par la séquence recalée sur max(id) ci-dessous
    'BATCH_JOB_SEQ': 'batch_job_seq',
    'BATCH_JOB_EXECUTION_SEQ': 'batch_job_execution_seq',
    'BATCH_STEP_EXECUTION_SEQ': 'batch_step_execution_seq',
}


def convert(value, pg_type):
    if value is None:
        return None
    if pg_type == 'boolean':
        if isinstance(value, (bytes, bytearray)):
            return any(value)
        return bool(value)
    if pg_type == 'timestamp with time zone' and isinstance(value, datetime.datetime):
        return value.replace(tzinfo=UTC) if value.tzinfo is None else value
    if pg_type == 'bytea' and isinstance(value, str):
        return value.encode('utf-8')
    if isinstance(value, str) and value.startswith('0000-00-00'):
        return None
    return value


def main():
    src = pymysql.connect(host=os.environ['SRC_HOST'], user='root', password=os.environ['SRC_PASSWORD'],
                          database=os.environ['SRC_DB'], charset='utf8mb4', cursorclass=pymysql.cursors.SSCursor)
    dst = psycopg.connect(autocommit=False)
    s, d = src.cursor(), dst.cursor()

    d.execute("SELECT count(*) FROM information_schema.tables WHERE table_name = 'flyway_schema_history'")
    if d.fetchone()[0] != 1:
        sys.exit("Cible non initialisée : démarrer d'abord le backend une fois (Flyway crée le schéma).")
    d.execute('SELECT count(*) FROM users')
    if d.fetchone()[0] != 0:
        sys.exit('La base cible contient déjà des données : migration refusée (base vide exigée).')

    s.execute("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'")
    src_tables = [r[0] for r in s.fetchall()]
    d.execute("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")
    dst_tables = {r[0] for r in d.fetchall()}

    # Le 1er démarrage du backend crée des configurations par défaut (CMT, leasing, lignes fonctionnelles) :
    # elles sont remplacées par celles de la production. Tout est vidé, sauf l'historique Flyway.
    to_empty = sorted(dst_tables - {'flyway_schema_history'})
    d.execute('TRUNCATE ' + ', '.join(f'"{t}"' for t in to_empty) + ' RESTART IDENTITY CASCADE')

    d.execute("SET session_replication_role = replica")
    copied, ignored, sequences = [], [], {}
    for table in sorted(src_tables):
        if table in SKIP_SOURCE:
            continue
        if table in SEQUENCE_TABLES:
            s.execute(f'SELECT * FROM `{table}`')
            rows = s.fetchall()
            sequences[table] = rows[0][0] if rows else 0
            continue
        target = table.lower()
        if target not in dst_tables:
            ignored.append(f'table {table} (absente du schéma PostgreSQL)')
            continue
        s.execute(f'SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = %s',
                  (table,))
        src_cols = [r[0] for r in s.fetchall()]
        d.execute("SELECT column_name, data_type, is_nullable, column_default, is_identity FROM information_schema.columns "
                  "WHERE table_schema = 'public' AND table_name = %s", (target,))
        dst_cols = {r[0]: r for r in d.fetchall()}
        cols = [c for c in src_cols if c.lower() in dst_cols]
        for c in src_cols:
            if c.lower() not in dst_cols:
                ignored.append(f'colonne {table}.{c} (absente du schéma PostgreSQL)')
        missing = [n for n, (_, _, nullable, default, _) in dst_cols.items()
                   if nullable == 'NO' and default is None and n not in {c.lower() for c in cols}]
        if missing:
            sys.exit(f'{target} : colonnes obligatoires sans équivalent dans la source : {missing}')
        types = [dst_cols[c.lower()][1] for c in cols]
        col_list = ', '.join(f'"{c.lower()}"' for c in cols)
        s.execute(f'SELECT {", ".join("`" + c + "`" for c in cols)} FROM `{table}`')
        count = 0
        with d.copy(f'COPY "{target}" ({col_list}) FROM STDIN') as copy:
            for row in s:
                copy.write_row([convert(v, t) for v, t in zip(row, types)])
                count += 1
        copied.append((target, count))

    d.execute("SET session_replication_role = DEFAULT")

    # Identités : prochain id = max(id) + 1
    d.execute("SELECT table_name, column_name FROM information_schema.columns WHERE table_schema = 'public' AND is_identity = 'YES'")
    for table, column in d.fetchall():
        d.execute(f'SELECT max("{column}") FROM "{table}"')
        top = d.fetchone()[0]
        if top is not None:
            d.execute(f'ALTER TABLE "{table}" ALTER COLUMN "{column}" RESTART WITH {top + 1}')
    # Séquence Envers (pas de 50, optimiseur « pooled ») : prochaine plage au-delà du max actuel
    d.execute('SELECT max(id) FROM audit_revision_entity')
    top = d.fetchone()[0] or 0
    d.execute("SELECT setval('audit_revision_entity_seq', %s, %s)", (max(top, 1), top > 0))
    # Séquences Spring Batch : reprennent après le dernier identifiant utilisé côté MySQL
    for src_table, seq in SEQUENCE_TABLES.items():
        if seq:
            last = int(sequences.get(src_table, 0) or 0)
            d.execute('SELECT setval(%s, %s, %s)', (seq, max(last, 1), last > 0))

    # Contrôle : mêmes volumes table par table
    errors = []
    for target, count in copied:
        d.execute(f'SELECT count(*) FROM "{target}"')
        if d.fetchone()[0] != count:
            errors.append(target)
    if errors:
        dst.rollback()
        sys.exit(f'Volumes différents après copie : {errors} — rien n\'a été validé.')
    dst.commit()

    total = sum(c for _, c in copied)
    print(f'Copie validée : {len(copied)} tables, {total} lignes.')
    for target, count in sorted(copied, key=lambda x: -x[1])[:12]:
        print(f'  {target:40s} {count:>8}')
    if ignored:
        print('Ignoré (absent du schéma cible) :')
        for i in ignored:
            print('  -', i)
    d.execute('ANALYZE')


if __name__ == '__main__':
    main()
