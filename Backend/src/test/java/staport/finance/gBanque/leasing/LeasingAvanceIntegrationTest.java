package staport.finance.gBanque.leasing;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import staport.finance.gBanque.AbstractIntegrationTest;
import staport.finance.gBanque.auth.services.AuthCookieService;
import staport.finance.gBanque.user.entities.User;
import staport.finance.gBanque.user.repositories.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Avance d'un contrat de leasing (échéance n°1) réglée par un chèque, une LCN ou un virement : génération du plan,
 * rattachement depuis le contrat, statut lu sur le moyen de paiement et absence de double comptage dans les encours.
 */
class LeasingAvanceIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "MotDePasse-Test-1";
    private static final AtomicLong UNICITE = new AtomicLong(700_000);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Cookie admin;
    private long banque;
    private long organisme;

    @BeforeEach
    void preparer() throws Exception {
        String username = "it-" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User(username, passwordEncoder.encode(PASSWORD), true);
        user.setSystemAdmin(true);
        userRepository.save(user);
        admin = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthCookieService.ACCESS_COOKIE);

        long n = UNICITE.incrementAndGet();
        banque = jdbc.queryForObject("INSERT INTO banque (date_creation, date_modification, numero_compte, agence, nom, "
                + "address, devise, logo_data) VALUES (CURRENT_DATE, CURRENT_DATE, 'AV" + n + "', 'Agence', 'Banque avance " + n
                + "', 'Adresse', 'MAD', decode('00', 'hex')) RETURNING id", Long.class);
        organisme = jdbc.queryForObject("INSERT INTO organisme_leasing (date_creation, date_modification, nom) "
                + "VALUES (now(), now(), 'Organisme avance " + n + "') RETURNING id", Long.class);
        jdbc.update("INSERT INTO contrat_cadre (date_debut, date_fin, plafond, date_creation, date_modification, organisme_id, statut) "
                + "VALUES ('2020-01-01', '2099-12-31', 100000000, now(), now(), " + organisme + ", 'ACTIF')");
    }

    // ── Génération du plan ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Avance = échéance n°1 à la date de début, mensualités à partir de M+1, valeur résiduelle en dernier")
    void generation_avanceEnPremiereEcheance() throws Exception {
        long contrat = creerContrat("AV-GEN", "10000.00", 4, "3000.00", 3, "2500.00").longValue();

        List<Map<String, Object>> echeances = echeances(contrat);
        assertThat(echeances).hasSize(5);   // avance + 3 mensualités + valeur résiduelle
        assertThat(echeances.get(0)).containsEntry("numero_echeance", 1).containsEntry("statut_paiement", "PREVU");
        assertThat(echeances.get(0).get("montant_ttc")).hasToString("3000.00");
        assertThat(echeances.get(0).get("date_echeance")).hasToString("2026-11-01");
        assertThat(echeances.get(1).get("date_echeance")).hasToString("2026-12-01");
        assertThat(echeances.get(1).get("montant_ttc")).hasToString("2500.00");
        assertThat(echeances.get(4).get("montant_ttc")).hasToString("100.00");   // 1 % de 10 000
    }

    @Test
    @DisplayName("Contrat sans avance : plan inchangé (mensualités dès la date de début)")
    void generation_sansAvance_inchangee() throws Exception {
        long contrat = creerContrat("AV-SANS", "10000.00", 3, null, 3, "4000.00").longValue();

        List<Map<String, Object>> echeances = echeances(contrat);
        assertThat(echeances).hasSize(4);
        assertThat(echeances.get(0).get("date_echeance")).hasToString("2026-11-01");
        assertThat(echeances.get(0).get("montant_ttc")).hasToString("4000.00");
    }

    @Test
    @DisplayName("Les phases doivent couvrir la période moins le mois de l'avance")
    void generation_phasesIncoherentesAvecAvance_refusees() throws Exception {
        mockMvc.perform(post("/contratsLeasing").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat("AV-KO", "10000.00", 4, "3000.00", 4, "2500.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("L'avance doit être strictement inférieure au prix d'achat")
    void generation_avanceTropGrande_refusee() throws Exception {
        mockMvc.perform(post("/contratsLeasing").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat("AV-GRD", "10000.00", 4, "10000.00", 3, "2500.00")))
                .andExpect(status().isBadRequest());
    }

    // ── Rattachement et statut calculé ─────────────────────────────────────────

    @Test
    @DisplayName("Rattachée à un chèque en cours : avance PRÉVUE, absente des encours de leasing (le chèque la représente)")
    void rattachement_chequeEnCours_pasDeDoublon() throws Exception {
        long contrat = creerContrat("AV-CHQ", "10000.00", 4, "3000.00", 3, "2500.00").longValue();
        long avance = idAvance(contrat);
        assertThat(dansLesEncoursLeasing(avance)).isTrue();   // avant rattachement : l'échéance seule porte l'avance

        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        rattacher(contrat, "CHEQUE", cheque).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("PREVU"))
                .andExpect(jsonPath("$.moyenPaiement.id").value(cheque));

        assertThat(dansLesEncoursLeasing(avance)).isFalse();
        assertThat(statutEffectif(avance)).isEqualTo("PREVU");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM v_recapitulatif_encours WHERE source_type = 'CHEQUE' AND entity_id = "
                + cheque, Long.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Le chèque réglé fait passer l'avance PAYÉE, et son retour en cours la repasse PRÉVUE — sans aucune copie de statut")
    void statut_suitLeMoyenDePaiement() throws Exception {
        long contrat = creerContrat("AV-SUI", "10000.00", 4, "3000.00", 3, "2500.00").longValue();
        long avance = idAvance(contrat);
        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        rattacher(contrat, "CHEQUE", cheque).andExpect(status().isOk());

        jdbc.update("UPDATE cheques SET statut = 'REGLE' WHERE id = " + cheque);
        assertThat(statutEffectif(avance)).isEqualTo("PAYE");
        assertThat(dejaPayeDuContrat(contrat)).isEqualByComparingTo("3000.00");

        jdbc.update("UPDATE cheques SET statut = 'ENCOURS' WHERE id = " + cheque);
        assertThat(statutEffectif(avance)).isEqualTo("PREVU");
        assertThat(dejaPayeDuContrat(contrat)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Chèque annulé : l'avance réapparaît dans les encours de leasing, PRÉVUE")
    void chequeAnnule_avanceReapparait() throws Exception {
        long contrat = creerContrat("AV-ANN", "10000.00", 4, "3000.00", 3, "2500.00").longValue();
        long avance = idAvance(contrat);
        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        rattacher(contrat, "CHEQUE", cheque).andExpect(status().isOk());

        jdbc.update("UPDATE cheques SET statut = 'ANNULE' WHERE id = " + cheque);

        assertThat(dansLesEncoursLeasing(avance)).isTrue();
        assertThat(statutEffectif(avance)).isEqualTo("PREVU");
    }

    @Test
    @DisplayName("Montant différent, encaissement, moyen déjà utilisé : rattachement refusé")
    void rattachement_refus() throws Exception {
        long contrat = creerContrat("AV-REF", "10000.00", 4, "3000.00", 3, "2500.00").longValue();
        rattacher(contrat, "CHEQUE", cheque("2999.00", "DECAISSEMENT", "ENCOURS")).andExpect(status().isBadRequest());
        rattacher(contrat, "CHEQUE", cheque("3000.00", "ENCAISSEMENT", "ENCOURS")).andExpect(status().isBadRequest());

        long bon = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        rattacher(contrat, "CHEQUE", bon).andExpect(status().isOk());

        long autre = creerContrat("AV-REF2", "10000.00", 4, "3000.00", 3, "2500.00").longValue();
        rattacher(autre, "CHEQUE", bon).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Détachement possible tant que le moyen n'est pas réglé, refusé ensuite")
    void detachement() throws Exception {
        long contrat = creerContrat("AV-DET", "10000.00", 4, "3000.00", 3, "2500.00").longValue();
        long avance = idAvance(contrat);
        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        rattacher(contrat, "CHEQUE", cheque).andExpect(status().isOk());

        mockMvc.perform(delete("/echeances-leasing/avance/{id}/moyen-paiement", contrat).cookie(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.moyenPaiement").doesNotExist());
        assertThat(dansLesEncoursLeasing(avance)).isTrue();

        rattacher(contrat, "CHEQUE", cheque).andExpect(status().isOk());
        jdbc.update("UPDATE cheques SET statut = 'REGLE' WHERE id = " + cheque);
        mockMvc.perform(delete("/echeances-leasing/avance/{id}/moyen-paiement", contrat).cookie(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Une avance rattachée ne se valide pas comme une échéance et verrouille la modification du contrat")
    void avanceRattachee_verrous() throws Exception {
        long contrat = creerContrat("AV-LCK", "10000.00", 4, "3000.00", 3, "2500.00").longValue();
        long avance = idAvance(contrat);
        rattacher(contrat, "CHEQUE", cheque("3000.00", "DECAISSEMENT", "ENCOURS")).andExpect(status().isOk());

        mockMvc.perform(put("/echeances-leasing/{id}/toggle-status", avance).cookie(admin)).andExpect(status().isBadRequest());
        mockMvc.perform(put("/contratsLeasing/{id}", contrat).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat("AV-LCK", "10000.00", 4, "3000.00", 3, "2500.00")))
                .andExpect(status().isBadRequest());
    }

    // ── Données ────────────────────────────────────────────────────────────────

    private Long creerContrat(String numero, String achat, int periode, String avance, int dureePhase, String mensualite)
            throws Exception {
        String reponse = mockMvc.perform(post("/contratsLeasing").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat(numero + "-" + UNICITE.incrementAndGet(), achat, periode, avance, dureePhase, mensualite)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.valueOf(reponse.replaceAll("^\\{\"id\":(\\d+).*$", "$1"));
    }

    private String corpsContrat(String numero, String achat, int periode, String avance, int dureePhase, String mensualite) {
        return "{\"numeroContrat\":\"" + numero + "\",\"banqueDePrelevementId\":" + banque
                + ",\"organismeLeasingId\":" + organisme + ",\"immobilisation\":\"Engin\",\"montantAchatTTC\":" + achat
                + ",\"dateDebut\":\"2026-11-01\",\"periodeEnMois\":" + periode + ",\"typeContrat\":\"LINEAIRE\""
                + ",\"valeurResiduelle\":1.00" + (avance != null ? ",\"avanceTTC\":" + avance : "")
                + ",\"phases\":[{\"numeroPhase\":1,\"dureeMois\":" + dureePhase + ",\"mensualiteTTC\":" + mensualite + "}]}";
    }

    private long cheque(String montant, String sens, String statut) {
        return jdbc.queryForObject("INSERT INTO cheques (date_emission, montant, banque_id, beneficiaire, libelle, observation, "
                + "statut, type, numero) VALUES (CURRENT_DATE, " + montant + ", " + banque + ", 'Organisme', 'Avance', 'Avance', '"
                + statut + "', '" + sens + "', 'CH" + UNICITE.incrementAndGet() + "') RETURNING id", Long.class);
    }

    private ResultActions rattacher(long contrat, String type, long moyen) throws Exception {
        return mockMvc.perform(put("/echeances-leasing/avance/{id}/moyen-paiement", contrat).cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"" + type + "\",\"moyenPaiementId\":" + moyen + "}"));
    }

    private List<Map<String, Object>> echeances(long contrat) {
        return jdbc.queryForList("SELECT numero_echeance, date_echeance, montant_ttc, statut_paiement FROM echeance_leasing "
                + "WHERE contrat_leasing_id = " + contrat + " ORDER BY numero_echeance");
    }

    private long idAvance(long contrat) {
        return jdbc.queryForObject("SELECT id FROM echeance_leasing WHERE contrat_leasing_id = " + contrat
                + " AND numero_echeance = 1", Long.class);
    }

    private boolean dansLesEncoursLeasing(long echeance) {
        return jdbc.queryForObject("SELECT count(*) FROM v_recapitulatif_encours WHERE source_type = 'LEASING' AND entity_id = "
                + echeance, Long.class) > 0;
    }

    /** Statut effectif tel que le lit l'application (même formule que les requêtes de l'entité), via une vraie requête JPQL. */
    private String statutEffectif(long echeance) {
        return jdbc.queryForObject("SELECT CASE WHEN el.moyen_paiement_id IS NULL THEN el.statut_paiement "
                + "ELSE COALESCE((SELECT CASE WHEN vm.statut_moyen = 'REGLE' THEN 'PAYE' ELSE 'PREVU' END "
                + "FROM v_echeance_leasing_moyen vm WHERE vm.echeance_id = el.id), 'PREVU') END "
                + "FROM echeance_leasing el WHERE el.id = " + echeance, String.class);
    }

    private java.math.BigDecimal dejaPayeDuContrat(long contrat) throws Exception {
        String reponse = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/echeances-leasing/tableau-amortissement/{id}", contrat).cookie(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"montantTotalDejaPaye\":([0-9.]+)").matcher(reponse);
        assertThat(m.find()).as("montantTotalDejaPaye dans %s", reponse).isTrue();
        return new java.math.BigDecimal(m.group(1));
    }
}
