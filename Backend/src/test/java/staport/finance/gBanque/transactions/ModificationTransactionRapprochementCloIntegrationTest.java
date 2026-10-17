package staport.finance.gBanque.transactions;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import staport.finance.gBanque.AbstractIntegrationTest;
import staport.finance.gBanque.Agios.service.AgiosOrchestrateurService;
import staport.finance.gBanque.auth.services.AuthCookieService;
import staport.finance.gBanque.user.entities.User;
import staport.finance.gBanque.user.repositories.UserRepository;

import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modification d'une transaction dont le rapprochement est définitivement clôturé (décision HMAYDA, 17/10/2026) :
 * dates, libellé, observation et TVA sont corrigeables ; le montant ne change jamais. Une correction de date de valeur
 * recalcule tout de suite les agios des périodes ouvertes concernées ; une période clôturée n'est pas touchée et
 * l'écart est signalé.
 */
class ModificationTransactionRapprochementCloIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "MotDePasse-Test-1";
    private static final AtomicLong IDS = new AtomicLong(920_000);
    private static final int ANNEE = LocalDate.now().getYear() - 1;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AgiosOrchestrateurService orchestrateur;

    private Cookie admin;

    @Test
    @DisplayName("Rapprochement clôturé définitivement : libellé, observation, dates et TVA modifiables, montant intact")
    void clotureDefinitive_champsCorrigeables() throws Exception {
        long banque = nouvelleBanque();
        long transaction = transaction(banque, "CLOTURE_DEFINITIVE", null, null);

        modifier(transaction, "2026-02-03", "NOUVEAU LIBELLE", 20.0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelle").value("NOUVEAU LIBELLE"))
                .andExpect(jsonPath("$.dateValeur").value("2026-02-03"))
                .andExpect(jsonPath("$.debit").value(1000.00))
                .andExpect(jsonPath("$.tvaDeductible").value(166.67))
                .andExpect(jsonPath("$.avertissements", hasSize(0)));

        assertThat(jdbc.queryForObject("SELECT debit FROM transaction WHERE id = ?", BigDecimal.class, transaction))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Date de valeur corrigée : les agios de la période ouverte sont recalculés immédiatement")
    void dateDeValeur_recalculeLaPeriodeOuverte() throws Exception {
        long banque = nouvelleBanque();
        long marche = preparerDroitConstate(banque);
        long droitConstate = ligneDroitConstate(marche, banque, ANNEE + "-01-01");
        orchestrateur.recalculerTrimestreManuel(banque, 1, ANNEE);
        BigDecimal avant = totalAgios(banque);
        assertThat(avant).as("agios calculés depuis le 1er janvier").isPositive();

        long transaction = transaction(banque, "CLOTURE_DEFINITIVE", "DROIT_CONSTATE", droitConstate);
        modifier(transaction, ANNEE + "-02-01", "LIBELLE", 0.0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avertissements", hasSize(0)));

        assertThat(jdbc.queryForObject("SELECT date_valeur FROM droit_constate WHERE id = ?", java.sql.Date.class, droitConstate)
                .toLocalDate()).as("date propagée à l'opération source").isEqualTo(LocalDate.of(ANNEE, 2, 1));
        assertThat(totalAgios(banque)).as("un mois d'agios en moins").isLessThan(avant);
    }

    @Test
    @DisplayName("Date de valeur dans une période d'agios clôturée : modification acceptée, période non recalculée, avertissement")
    void periodeCloturee_avertissement() throws Exception {
        long banque = nouvelleBanque();
        jdbc.update("INSERT INTO agios_trimestriel (annee, date_debut, date_fin, nb_jours_calcules, nb_jours_total, total_agios_ht, "
                        + "total_agios_ttc, total_tva, trimestre, banque_id, statut, type_agios) "
                        + "VALUES (?, ?, ?, 90, 90, 100, 110, 10, 1, ?, 'CLOTURE', 'FACILITE_CAISSE')",
                ANNEE, java.sql.Date.valueOf(ANNEE + "-01-01"), java.sql.Date.valueOf(ANNEE + "-03-31"), banque);
        long transaction = transaction(banque, "CLOTURE_DEFINITIVE", null, null);

        modifier(transaction, ANNEE + "-02-10", "LIBELLE", 0.0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avertissements", hasSize(1)))
                .andExpect(jsonPath("$.avertissements[0]", containsString("clôturée")));

        assertThat(jdbc.queryForObject("SELECT total_agios_ttc FROM agios_trimestriel WHERE banque_id = ?", BigDecimal.class, banque))
                .isEqualByComparingTo("110");
    }

    // ── Accès ──────────────────────────────────────────────────────────────────

    private ResultActions modifier(long transaction, String dateValeur, String libelle, double tauxTva) throws Exception {
        String corps = "{\"dateValeur\":\"" + dateValeur + "\",\"dateOperation\":\"" + dateValeur + "\",\"libelle\":\"" + libelle
                + "\",\"observation\":\"OBS\",\"tauxTva\":" + tauxTva + "}";
        return mockMvc.perform(patch("/transactions/" + transaction).cookie(admin())
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private Cookie admin() throws Exception {
        if (admin == null) {
            String username = "it-" + UUID.randomUUID().toString().substring(0, 8);
            User user = new User(username, passwordEncoder.encode(PASSWORD), true);
            user.setSystemAdmin(true);
            userRepository.save(user);
            admin = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getCookie(AuthCookieService.ACCESS_COOKIE);
        }
        return admin;
    }

    // ── Données ────────────────────────────────────────────────────────────────

    private BigDecimal totalAgios(long banque) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(total_agios_ttc), 0) FROM agios_trimestriel "
                + "WHERE banque_id = ? AND type_agios = 'DROIT_CONSTATE'", BigDecimal.class, banque);
    }

    private long nouvelleBanque() {
        long id = IDS.incrementAndGet();
        sql("INSERT INTO banque (id, date_creation, date_modification, numero_compte, agence, nom, address, devise, logo_data) "
                + "VALUES (" + id + ", now(), now(), 'IT-MOD-" + id + "', 'AGENCE', 'BANQUE MOD " + id + "', 'ADRESSE', 'MAD', decode('00', 'hex'))");
        return id;
    }

    /** Un marché, la nature « droit constaté » et sa ligne spécifique active : la banque devient éligible au calcul. */
    private long preparerDroitConstate(long banque) {
        long marche = IDS.incrementAndGet();
        sql("INSERT INTO marches (id, banque_id, actif, created_at, modified_at, montant, numero, maitre_douvrage, nom) "
                + "VALUES (" + marche + ", " + banque + ", true, now(), now(), 10000000.00, 'M-" + marche + "', 'MO', 'MARCHE " + marche + "')");
        sql("INSERT INTO nature_ligne_fonctionnelle (id, date_creation, date_modification, code, portee, description, label) "
                + "VALUES (" + IDS.incrementAndGet() + ", now(), now(), 'DROIT_CONSTATE', 'GENERALE_ET_SPECIFIQUE', 'essai', 'Droit constaté') ON CONFLICT DO NOTHING");
        sql("INSERT INTO ligne_fonctionnelle_specifique (actif, date_depart, date_fin, plafond, taux, date_creation, date_modification, marche_id, nature_ligne_id) "
                + "SELECT true, '" + (ANNEE - 1) + "-01-01', '" + (ANNEE + 3) + "-01-01', 100000000.00, 5.00, now(), now(), " + marche
                + ", n.id FROM nature_ligne_fonctionnelle n WHERE n.code = 'DROIT_CONSTATE'");
        return marche;
    }

    private long ligneDroitConstate(long marche, long banque, String date) {
        String dates = "'" + date + "', '" + date + "', '" + date + "'";
        return jdbc.queryForObject("INSERT INTO droit_constate (marche_id, banque_id, type, status, montant, date_emission, date_operation, "
                + "date_valeur, libelle, observation) VALUES (" + marche + ", " + banque + ", 'DEBLOCAGE', 'PAYE', 1000000.00, " + dates
                + ", 'Déblocage', 'essai') RETURNING id", Long.class);
    }

    private long transaction(long banque, String statutRapprochement, String source, Long referent) {
        long rapprochement = jdbc.queryForObject("INSERT INTO rapprochement (banque_id, date_rapprochement, statut, created_at, updated_at) "
                + "VALUES (" + banque + ", '" + ANNEE + "-02-01', '" + statutRapprochement + "', now(), now()) RETURNING id", Long.class);
        return jdbc.queryForObject("INSERT INTO transaction (rapprochement_id, banque_id, transaction_source, referent, debit, credit, "
                        + "libelle, observation, date_emission, date_valeur, date_operations, tva_deductible, tva_collectee, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, 1000.00, 0, 'ANCIEN', 'OBS', ?, ?, ?, 0, 0, now(), now()) RETURNING id",
                Long.class, rapprochement, banque, source, referent, java.sql.Date.valueOf(ANNEE + "-01-01"),
                java.sql.Date.valueOf(ANNEE + "-01-01"), java.sql.Date.valueOf(ANNEE + "-01-01"));
    }

    private void sql(String requete) {
        jdbc.execute((java.sql.Connection c) -> {
            try (Statement st = c.createStatement()) {
                st.execute("SET session_replication_role = replica");
                try {
                    st.execute(requete);
                } finally {
                    st.execute("SET session_replication_role = DEFAULT");
                }
                return null;
            }
        });
    }
}
