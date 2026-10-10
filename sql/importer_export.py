"""Importe un export phpMyAdmin (XAMPP ou production) dans le MySQL 8 de la stack Docker.

Usage (depuis la racine du dépôt, stack démarrée : `docker compose up -d mysql`) :

    python sql/importer_export.py "C:/chemin/vers/gbanque.sql"

Ce que fait le script :
  1. Adapte l'export à MySQL 8 sous Linux :
     * tables Spring Batch remises en MAJUSCULES (un export passé par un MySQL Windows/XAMPP les met en
       minuscules ; sous Linux les noms sont sensibles à la casse et Spring Batch ne les trouverait pas) ;
     * vues et « tables provisoires » de vues retirées : les vues sont recréées par Flyway (R__v_*.sql) au
       démarrage du backend, et la définition exportée par phpMyAdmin est inutilisable telle quelle.
  2. Charge le résultat dans la base MYSQL_DATABASE du conteneur gbanque-mysql (mot de passe root lu dans .env).
  3. Affiche le nombre de tables et de lignes de quelques tables clés.

La base cible doit être VIDE (premier démarrage, ou volume ./docker-data/mysql supprimé). Ensuite, démarrer le
backend : Flyway marque la base en version 1 (baseline), applique V2 et suivantes, recrée les vues, puis
Hibernate valide le schéma.
"""
import os
import re
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTAINER = 'gbanque-mysql'


def read_env():
    env = {}
    for line in open(os.path.join(ROOT, '.env'), encoding='utf-8'):
        m = re.match(r'^([A-Z_][A-Z0-9_]*)=(.*)$', line.rstrip('\r\n'))
        if m:
            env[m.group(1)] = m.group(2)
    return env


def prepare(path):
    text = open(path, encoding='utf-8').read()
    text = re.sub(r'`batch_([a-z_]+)`', lambda m: '`BATCH_' + m.group(1).upper() + '`', text)
    out, skip = [], False
    for line in text.split('\n'):
        # Début d'un bloc de vue (« Stand-in structure » ou « Structure for view ») : ignoré jusqu'au commentaire suivant
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


def mysql(env, sql_args, stdin=None):
    cmd = ['docker', 'exec', '-i', CONTAINER, 'mysql', '-uroot', f"-p{env['MYSQL_ROOT_PASSWORD']}",
           '--default-character-set=utf8mb4'] + sql_args
    return subprocess.run(cmd, stdin=stdin, capture_output=True, text=True)


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    env = read_env()
    db = env['MYSQL_DATABASE']
    existing = mysql(env, ['-N', '-e', f"SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='{db}'"])
    if existing.returncode != 0:
        sys.exit('MySQL injoignable : lancer d\'abord `docker compose up -d mysql`.\n' + existing.stderr)
    if existing.stdout.strip() != '0':
        sys.exit(f"La base {db} contient déjà {existing.stdout.strip()} tables : import refusé (base vide exigée).")

    prepared = prepare(sys.argv[1])
    try:
        with open(prepared, encoding='utf-8') as f:
            result = mysql(env, [db], stdin=f)
        errors = [l for l in result.stderr.splitlines() if 'Using a password' not in l]
        # La restauration finale des variables de session par phpMyAdmin échoue sans conséquence
        errors = [l for l in errors if "character_set_client' can't be set to the value of 'NULL'" not in l]
        if errors:
            sys.exit('Erreur pendant l\'import :\n' + '\n'.join(errors))
    finally:
        os.remove(prepared)

    report = mysql(env, ['-N', db, '-e',
                         f"SELECT 'tables', COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='{db}' "
                         "UNION ALL SELECT 'banque', COUNT(*) FROM banque "
                         "UNION ALL SELECT 'transaction', COUNT(*) FROM transaction "
                         "UNION ALL SELECT 'users', COUNT(*) FROM users"])
    print('Import terminé :')
    print(report.stdout)
    print('Étape suivante : démarrer le backend (Flyway : baseline V1, V2, vues ; puis validation du schéma).')


if __name__ == '__main__':
    main()
