"""Migre les données de production (export phpMyAdmin de la base MySQL) vers le PostgreSQL 18 de la stack Docker.

Usage, depuis la racine du dépôt (procédure complète : docs/infra/migration-postgres.md) :

    docker compose up -d postgres              # base vierge
    python sql/migration-postgres/migrer_vers_postgres.py "C:/chemin/vers/export.sql"
    docker compose up -d --build                # le backend applique V2 et suivantes sur les données migrées

Étapes :
  0. Crée le schéma en version 1 SEULEMENT (image flyway/flyway, mêmes fichiers db/migration que le backend) : les
     données de production entrent dans le schéma V1 ; les migrations suivantes (V2 : correction des données puis
     colonnes rendues obligatoires…) s'appliquent ensuite au démarrage du backend.
  1. Démarre un MySQL 8 temporaire (gbanque-mysql-source) sur le réseau de la stack et y charge l'export
     (adapté : tables Spring Batch en majuscules, vues retirées).
  2. Lance copier.py dans un conteneur Python éphémère (pymysql + psycopg) : copie transactionnelle de toutes
     les tables vers PostgreSQL, recalage des compteurs, contrôle des volumes.
  3. Supprime le MySQL temporaire.
Refuse de s'exécuter si la base PostgreSQL contient déjà des utilisateurs ou un schéma au-delà de la version 1.
"""
import os
import re
import subprocess
import sys
import tempfile
import time

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
HERE = os.path.dirname(os.path.abspath(__file__))
SOURCE = 'gbanque-mysql-source'
SOURCE_PASSWORD = 'source-temporaire'
PYTHON_IMAGE = 'python:3.12-slim'
FLYWAY_IMAGE = 'flyway/flyway:11.20.3-alpine'   # même version que le backend (pom.xml)
MIGRATIONS = os.path.join(ROOT, 'Backend', 'src', 'main', 'resources', 'db', 'migration')


def run(cmd, **kw):
    return subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', **kw)


def read_env():
    env = {}
    for line in open(os.path.join(ROOT, '.env'), encoding='utf-8'):
        m = re.match(r'^([A-Z_][A-Z0-9_]*)=(.*)$', line.rstrip('\r\n'))
        if m:
            env[m.group(1)] = m.group(2)
    return env


def prepare(path):
    """Adapte l'export phpMyAdmin : tables Spring Batch en majuscules, vues et tables provisoires retirées."""
    text = open(path, encoding='utf-8').read()
    text = re.sub(r'`batch_([a-z_]+)`', lambda m: '`BATCH_' + m.group(1).upper() + '`', text)
    out, skip = [], False
    for line in text.split('\n'):
        if line.startswith('-- Stand-in structure for view') or line.startswith('-- Structure for view'):
            skip = True
            continue
        if skip and (line.startswith('-- Indexes for dumped tables') or line.startswith('-- Table structure for table')
                     or line.startswith('-- Dumping data for table')):
            skip = False
        if not skip:
            out.append(line)
    tmp = tempfile.NamedTemporaryFile('w', suffix='.sql', delete=False, encoding='utf-8', newline='\n')
    tmp.write('\n'.join(out))
    tmp.close()
    return tmp.name


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    env = read_env()
    network = run(['docker', 'inspect', 'gbanque-postgres', '--format',
                   '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}']).stdout.strip()
    if not network:
        sys.exit("Conteneur gbanque-postgres introuvable : lancer d'abord `docker compose up -d postgres`.")

    psql = ['docker', 'exec', 'gbanque-postgres', 'psql', '-U', env['POSTGRES_USER'], '-d', env['POSTGRES_DB'], '-tAc']
    has_history = run(psql + ["SELECT count(*) FROM information_schema.tables WHERE table_name = 'flyway_schema_history'"])
    if has_history.stdout.strip() == '1':
        version = run(psql + ['SELECT max(version::int) FROM flyway_schema_history WHERE version IS NOT NULL AND success'])
        users = run(psql + ['SELECT count(*) FROM users'])
        if version.stdout.strip() != '1' or users.stdout.strip() != '0':
            sys.exit(f'Base PostgreSQL non vierge (schéma en version {version.stdout.strip()}, '
                     f'{users.stdout.strip()} utilisateur(s)) : migration refusée. Repartir d\'une base vide '
                     '(cf. docs/infra/migration-postgres.md).')
    else:
        print('0/3 Création du schéma en version 1 (Flyway)…')
        flyway = run(['docker', 'run', '--rm', '--network', network, '-v', f'{MIGRATIONS}:/flyway/sql:ro', FLYWAY_IMAGE,
                      f"-url=jdbc:postgresql://postgres:5432/{env['POSTGRES_DB']}", f"-user={env['POSTGRES_USER']}",
                      f"-password={env['POSTGRES_PASSWORD']}", '-locations=filesystem:/flyway/sql', '-target=1', 'migrate'])
        if flyway.returncode != 0:
            sys.exit('Création du schéma en échec :\n' + flyway.stdout[-2000:] + flyway.stderr[-2000:])

    print('1/3 MySQL temporaire et chargement de l\'export…')
    run(['docker', 'rm', '-f', SOURCE])
    started = run(['docker', 'run', '-d', '--name', SOURCE, '--network', network,
                   '-e', f'MYSQL_ROOT_PASSWORD={SOURCE_PASSWORD}', '-e', 'MYSQL_DATABASE=source', 'mysql:8.0',
                   '--character-set-server=utf8mb4', '--collation-server=utf8mb4_unicode_ci'])
    if started.returncode != 0:
        sys.exit(started.stderr)
    try:
        for _ in range(120):
            logs = run(['docker', 'logs', SOURCE])
            if 'ready for connections' in logs.stderr and 'port: 3306' in logs.stderr:
                break
            time.sleep(3)
        time.sleep(3)
        prepared = prepare(sys.argv[1])
        try:
            with open(prepared, encoding='utf-8') as f:
                loaded = subprocess.run(['docker', 'exec', '-i', SOURCE, 'mysql', '-uroot', f'-p{SOURCE_PASSWORD}',
                                         '--default-character-set=utf8mb4', 'source'],
                                        stdin=f, capture_output=True, text=True, encoding='utf-8')
        finally:
            os.remove(prepared)
        errors = [l for l in loaded.stderr.splitlines() if 'Using a password' not in l
                  and "character_set_client' can't be set to the value of 'NULL'" not in l]
        if errors:
            sys.exit("Erreur au chargement de l'export :\n" + '\n'.join(errors))

        print('2/3 Copie vers PostgreSQL…')
        copy = subprocess.run([
            'docker', 'run', '--rm', '--network', network, '-v', f'{HERE}:/migration:ro',
            '-e', f'SRC_HOST={SOURCE}', '-e', f'SRC_PASSWORD={SOURCE_PASSWORD}', '-e', 'SRC_DB=source',
            '-e', 'PGHOST=postgres', '-e', f"PGDATABASE={env['POSTGRES_DB']}", '-e', f"PGUSER={env['POSTGRES_USER']}",
            '-e', f"PGPASSWORD={env['POSTGRES_PASSWORD']}", PYTHON_IMAGE, 'sh', '-c',
            'pip install --quiet --disable-pip-version-check --root-user-action=ignore pymysql "psycopg[binary]" '
            '&& python /migration/copier.py'], text=True, encoding='utf-8')
        if copy.returncode != 0:
            sys.exit('Copie en échec : la base PostgreSQL est restée vide (transaction annulée).')
    finally:
        print('3/3 Suppression du MySQL temporaire.')
        run(['docker', 'rm', '-f', SOURCE])
    print('Terminé. Démarrer la stack : docker compose up -d --build (le backend applique V2 et suivantes).')


if __name__ == '__main__':
    main()
