package staport.finance.gBanque.rapprochement;

import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractMigrationTest;

import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reprise V22 : liens individuels, invariants comptables et refus atomique des données divergentes. */
class AgiosT42025MigrationTest extends AbstractMigrationTest {

    @Test
    void ventileLesAgiosPayesEtConserveLesTotauxClotures() throws Exception {
        String url = preparerSources();
        migrer(url, "22");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM agios WHERE annee=2025")).isEqualTo("11");
            assertThat(texte(st, "SELECT count(*) FROM transaction")).isEqualTo("12");
            assertThat(texte(st, "SELECT count(*) FROM commission WHERE operation='CAUTION' AND status='PAYE' AND montant=50565.44 AND caution_id IS NULL")).isEqualTo("1");
            assertThat(texte(st, "SELECT count(*) FROM agios WHERE annee=2025 AND (montant_calcule IS DISTINCT FROM montant_facture OR montant IS DISTINCT FROM montant_facture)")).isEqualTo("0");
            assertThat(texte(st, "SELECT count(*) FROM agios a WHERE a.annee=2025 AND (SELECT count(*) FROM transaction t WHERE t.transaction_source='AGIOS' AND t.referent=a.id AND t.debit=a.montant AND t.banque_id=a.banque_id)<>1")).isEqualTo("0");
            assertThat(texte(st, "SELECT periodicite || ':' || periode_numero FROM agios WHERE id=7")).isEqualTo("MENSUEL:12");
            assertThat(texte(st, "SELECT type_agios || ':' || periode_numero FROM agios WHERE id=10")).isEqualTo("PREFINANCEMENT:4");
            verifierTotal(st, 5, "178458.60", "16223.51", "4");
            verifierTotal(st, 4, "105370.74", "9579.16", "3");
            assertThat(texte(st, "SELECT count(*) FROM rapprochement WHERE statut='CLOTURE_DEFINITIVE' AND solde_debut=123.45")).isEqualTo("5");
            assertThat(texte(st, "SELECT count(*) FROM agios WHERE id=100 AND annee IS NULL AND type_agios IS NULL AND montant=99")).isEqualTo("1");
            assertThat(texte(st, "SELECT count(*) FROM agios_journalier")).isEqualTo("0");
            // Chaque ligne nouvelle reprend les trois dates de son débit initial (ID 8 : valeur 2025).
            assertThat(texte(st, "SELECT count(*) FROM transaction WHERE rapprochement_id=5 AND date_emission='2026-01-05' AND date_operations='2026-01-05' AND date_valeur='2025-12-31'")).isEqualTo("4");
            assertThat(texte(st, "SELECT tva_deductible FROM transaction WHERE transaction_source='COMMISSION'")).isEqualTo("4596.86");
            assertThat(texte(st, "SELECT tva_deductible FROM transaction WHERE id=68")).isEqualTo("0.00");
            // Seul le facturé est optionnel dans les métadonnées des lignes reprises.
            st.execute("UPDATE agios SET montant_facture=NULL WHERE id=4");
            assertThatThrownBy(() -> st.execute("UPDATE agios SET montant_calcule=NULL WHERE id=4"))
                    .hasMessageContaining("ck_agios_reprise_complete");
            assertThatThrownBy(() -> st.execute("UPDATE agios SET type_agios=NULL WHERE id=4"))
                    .hasMessageContaining("ck_agios_reprise_complete");
        }
    }

    @Test
    void refuseUnMontantDivergentSansReprisePartielle() throws Exception {
        String url = preparerSources();
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("UPDATE transaction SET debit=178458.61 WHERE id=125");
        }
        assertThatThrownBy(() -> migrer(url, "22")).hasStackTraceContaining("état source différent");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM agios")).isEqualTo("8");
            assertThat(texte(st, "SELECT count(*) FROM transaction")).isEqualTo("7");
            assertThat(texte(st, "SELECT montant FROM agios WHERE id=8")).isEqualTo("178458.6000");
            assertThat(texte(st, "SELECT count(*) FROM information_schema.columns WHERE table_name='agios' AND column_name='type_agios'")).isEqualTo("0");
        }
    }

    @Test
    void accepteUneBaseViergeEtLesAgiosHorsTranche() throws Exception {
        String url = baseJetable();
        migrer(url, null);
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM agios")).isEqualTo("0");
            assertThat(texte(st, "SELECT count(*) FROM pg_constraint WHERE conname='ck_agios_reprise_complete' AND convalidated")).isEqualTo("1");
            // Le schéma transitoire reste compatible avec le module actuel, même sur une base vierge.
            st.execute("SET session_replication_role=replica");
            st.execute("INSERT INTO agios(id,banque_id,montant,status,libelle,observation) VALUES (4,1,10,'PREVU','AGIOS COURANT','AGIOS COURANT')");
            assertThatThrownBy(() -> st.execute("UPDATE agios SET type_agios='FACILITE_CAISSE' WHERE id=4"))
                    .hasMessageContaining("ck_agios_reprise_complete");
        }
    }

    private static void verifierTotal(Statement st, int rapprochement, String debit, String tva, String lignes) throws Exception {
        String filtre = " FROM transaction WHERE rapprochement_id=" + rapprochement;
        assertThat(texte(st, "SELECT sum(debit)" + filtre)).isEqualTo(debit);
        assertThat(texte(st, "SELECT sum(tva_deductible)" + filtre)).isEqualTo(tva);
        assertThat(texte(st, "SELECT sum(credit)" + filtre)).isEqualTo("0.00");
        assertThat(texte(st, "SELECT count(*)" + filtre)).isEqualTo(lignes);
    }

    private static String preparerSources() throws Exception {
        String url = baseJetable();
        migrer(url, "21");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role=replica");
            st.execute("INSERT INTO banque (id,date_creation,date_modification,numero_compte,agence,nom,address,devise,logo_data) SELECT n,now(),now(),'V22-'||n,'AGENCE','BANQUE '||n,'ADRESSE','MAD',decode('00','hex') FROM generate_series(1,7) n");
            st.execute("INSERT INTO rapprochement (id,banque_id,solde_debut,statut,date_rapprochement,created_at,updated_at) VALUES (2,1,123.45,'CLOTURE_DEFINITIVE','2026-01-01',now(),now()),(3,5,123.45,'CLOTURE_DEFINITIVE','2026-01-01',now(),now()),(4,4,123.45,'CLOTURE_DEFINITIVE','2026-01-01',now(),now()),(5,7,123.45,'CLOTURE_DEFINITIVE','2026-01-01',now(),now()),(8,3,123.45,'CLOTURE_DEFINITIVE','2026-01-01',now(),now())");
            st.execute("INSERT INTO agios (id,banque_id,montant,status,libelle,observation,date_emission,date_operation,date_valeur) SELECT id,b,m,'PAYE','ORIGINAL '||id,'OBSERVATION '||id,'2026-01-05','2026-01-05','2025-12-31' FROM (VALUES (4,5,22856.98),(5,5,64858.79),(6,1,19389.83),(7,3,230935.73),(8,7,178458.60),(9,4,105370.74),(10,3,39469.20),(100,1,99)) s(id,b,m)");
            st.execute("INSERT INTO transaction (id,referent,banque_id,rapprochement_id,debit,credit,tva_deductible,tva_collectee,transaction_source,libelle,observation,date_emission,date_operations,date_valeur) SELECT tx,a,b,r,m,0,v,0,'AGIOS','ORIGINAL '||a,'OBSERVATION '||a,'2026-01-05','2026-01-05','2025-12-31' FROM (VALUES (20,4,5,3,22856.98,2077.91),(19,5,5,3,64858.79,5896.25),(62,6,1,2,19389.83,0),(68,7,3,8,230935.73,0),(125,8,7,5,178458.60,16223.51),(136,9,4,4,105370.74,9579.16),(468,10,3,8,39469.20,3588.11)) s(tx,a,b,r,m,v)");
            st.execute("SELECT setval(pg_get_serial_sequence('agios','id'),100)");
            st.execute("SELECT setval(pg_get_serial_sequence('transaction','id'),468)");
            st.execute("SET session_replication_role=origin");
        }
        return url;
    }
}
