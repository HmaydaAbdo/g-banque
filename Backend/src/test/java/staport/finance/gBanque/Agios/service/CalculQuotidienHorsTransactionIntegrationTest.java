package staport.finance.gBanque.Agios.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import staport.finance.gBanque.AbstractIntegrationTest;

import java.sql.Statement;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Régression : le calcul quotidien (tâche de 02h00 et bouton « Recalculer tout ») est lancé sans transaction englobante.
 * La lecture de la ligne fonctionnelle prenait un verrou {@code SELECT ... FOR UPDATE} dans la transaction en lecture seule
 * ouverte par {@code estEligible} : PostgreSQL le refuse, et les droits constatés, le préfinancement et le factoring de
 * toutes les banques étaient en erreur chaque nuit (seule la facilité de caisse était calculée).
 */
class CalculQuotidienHorsTransactionIntegrationTest extends AbstractIntegrationTest {

    private static final long BANQUE = 900101L;
    private static final long MARCHE = 900101L;

    @Autowired
    private AgiosOrchestrateurService orchestrateur;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Le calcul quotidien lancé hors transaction calcule les droits constatés, le préfinancement et le factoring")
    void troisTypesCalculesHorsTransaction() {
        LocalDate debutAnnee = LocalDate.now().withDayOfYear(1);
        preparerDonnees(debutAnnee);

        orchestrateur.calculQuotidienAutomatique();

        for (String type : new String[]{"DROIT_CONSTATE", "PREFINANCEMENT", "FACTORING"}) {
            Integer periodes = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM agios_trimestriel WHERE banque_id = ? AND type_agios = ?", Integer.class, BANQUE, type);
            assertThat(periodes).as("périodes créées pour %s", type).isGreaterThan(0);
            Integer lignesJournalieres = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM agios_journalier WHERE banque_id = ? AND type_agios = ? AND agios_ttc > 0",
                    Integer.class, BANQUE, type);
            assertThat(lignesJournalieres).as("jours avec agios pour %s", type).isGreaterThan(0);
        }
    }

    /**
     * Une banque, un marché, une ligne spécifique active et un déblocage payé pour chacun des trois types. Les clés étrangères
     * sont suspendues le temps de l'insertion (lignes minimales).
     */
    private void preparerDonnees(LocalDate debutAnnee) {
        jdbcTemplate.execute((java.sql.Connection c) -> {
            try (Statement st = c.createStatement()) {
                st.execute("SET session_replication_role = replica");
                st.execute("INSERT INTO banque (id, date_creation, date_modification, numero_compte, agence, nom, address, devise, logo_data) "
                        + "VALUES (" + BANQUE + ", now(), now(), '000', 'AGENCE', 'BANQUE CALCUL', 'ADRESSE', 'MAD', decode('00', 'hex'))");
                st.execute("INSERT INTO marches (id, banque_id, actif, created_at, modified_at, montant, numero, maitre_douvrage, nom) "
                        + "VALUES (" + MARCHE + ", " + BANQUE + ", true, now(), now(), 10000000.00, 'M-CALCUL', 'MO', 'MARCHE CALCUL')");
                for (String[] nature : new String[][]{
                        {"900111", "PREFINANCEMENT", "Préfinancement"},
                        {"900112", "DROIT_CONSTATE", "Droit constaté"},
                        {"900113", "FACTORING", "Factoring"}}) {
                    st.execute("INSERT INTO nature_ligne_fonctionnelle (id, date_creation, date_modification, code, portee, description, label) "
                            + "VALUES (" + nature[0] + ", now(), now(), '" + nature[1] + "', 'GENERALE_ET_SPECIFIQUE', 'essai', '" + nature[2] + "') "
                            + "ON CONFLICT DO NOTHING");
                    st.execute("INSERT INTO ligne_fonctionnelle_specifique (actif, date_depart, date_fin, plafond, taux, date_creation, "
                            + "date_modification, marche_id, nature_ligne_id) SELECT true, '" + debutAnnee + "', '" + debutAnnee.plusYears(2)
                            + "', 100000000.00, 5.00, now(), now(), " + MARCHE + ", n.id FROM nature_ligne_fonctionnelle n WHERE n.code = '"
                            + nature[1] + "'");
                }
                String dates = "'" + debutAnnee + "', '" + debutAnnee + "', '" + debutAnnee + "'";
                st.execute("INSERT INTO prefinancement (marche_id, type, status, montant, date_emission, date_operation, date_valeur, libelle, observation) "
                        + "VALUES (" + MARCHE + ", 'DEBLOCAGE', 'PAYE', 1000000.00, " + dates + ", 'Déblocage', 'essai')");
                st.execute("INSERT INTO droit_constate (marche_id, banque_id, type, status, montant, date_emission, date_operation, date_valeur, libelle, observation) "
                        + "VALUES (" + MARCHE + ", " + BANQUE + ", 'DEBLOCAGE', 'PAYE', 1000000.00, " + dates + ", 'Déblocage', 'essai')");
                st.execute("INSERT INTO factoring (marche_id, type, status, montant, date_emission, date_operation, date_valeur, libelle, observation) "
                        + "VALUES (" + MARCHE + ", 'DEBLOCAGE', 'PAYE', 1000000.00, " + dates + ", 'Déblocage', 'essai')");
                st.execute("SET session_replication_role = DEFAULT");
                return null;
            }
        });
    }
}
