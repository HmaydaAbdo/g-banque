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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Avance d'un contrat de leasing (échéance n°1) définie par un chèque, une LCN ou un virement existant : génération du
 * plan, statut lu sur le moyen de paiement, mention d'avance sur le moyen de paiement et sa transaction (une seule),
 * absence de double comptage dans les encours.
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
    @DisplayName("Avance = échéance n°1 à la date de début (montant du moyen de paiement), mensualités dès M+1, valeur résiduelle en dernier")
    void generation_avanceEnPremiereEcheance() throws Exception {
        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        long contrat = creerContrat("AV-GEN", "10000.00", 4, "CHEQUE", cheque, 3, "2500.00");

        List<Map<String, Object>> echeances = echeances(contrat);
        assertThat(echeances).hasSize(5);   // avance + 3 mensualités + valeur résiduelle
        assertThat(echeances.get(0)).containsEntry("numero_echeance", 1).containsEntry("moyen_paiement_type", "CHEQUE");
        assertThat(echeances.get(0).get("montant_ttc")).hasToString("3000.00");
        assertThat(echeances.get(0).get("date_echeance")).hasToString("2026-11-01");
        assertThat(echeances.get(1).get("date_echeance")).hasToString("2026-12-01");
        assertThat(echeances.get(1).get("montant_ttc")).hasToString("2500.00");
        assertThat(echeances.get(4).get("montant_ttc")).hasToString("100.00");   // 1 % de 10 000
    }

    @Test
    @DisplayName("Contrat sans avance : plan inchangé (mensualités dès la date de début)")
    void generation_sansAvance_inchangee() throws Exception {
        long contrat = creerContrat("AV-SANS", "10000.00", 3, null, 0, 3, "4000.00");

        List<Map<String, Object>> echeances = echeances(contrat);
        assertThat(echeances).hasSize(4);
        assertThat(echeances.get(0).get("date_echeance")).hasToString("2026-11-01");
        assertThat(echeances.get(0).get("montant_ttc")).hasToString("4000.00");
    }

    @Test
    @DisplayName("Les phases doivent couvrir la période moins le mois de l'avance")
    void generation_phasesIncoherentesAvecAvance_refusees() throws Exception {
        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        mockMvc.perform(post("/contratsLeasing").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat("AV-KO", "10000.00", 4, "CHEQUE", cheque, 4, "2500.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("L'avance (montant du moyen de paiement) doit être strictement inférieure au prix d'achat")
    void generation_avanceTropGrande_refusee() throws Exception {
        long cheque = cheque("10000.00", "DECAISSEMENT", "ENCOURS");
        mockMvc.perform(post("/contratsLeasing").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat("AV-GRD", "10000.00", 4, "CHEQUE", cheque, 3, "2500.00")))
                .andExpect(status().isBadRequest());
    }

    // ── Moyen de paiement : choix, statut, encours ─────────────────────────────

    @Test
    @DisplayName("Chèque en cours : avance PRÉVUE, absente des encours de leasing (le chèque la représente), mention d'avance sur le chèque")
    void chequeEnCours_pasDeDoublon_etMention() throws Exception {
        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        String numero = "AV-ENC";
        long contrat = creerContrat(numero, "10000.00", 4, "CHEQUE", cheque, 3, "2500.00");
        long avance = idAvance(contrat);

        assertThat(dansLesEncoursLeasing(avance)).isFalse();
        assertThat(statutEffectif(avance)).isEqualTo("PREVU");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM v_recapitulatif_encours WHERE source_type = 'CHEQUE' AND entity_id = "
                + cheque, Long.class)).isEqualTo(1);

        String libelle = jdbc.queryForObject("SELECT libelle FROM cheques WHERE id = " + cheque, String.class);
        String observation = jdbc.queryForObject("SELECT observation FROM cheques WHERE id = " + cheque, String.class);
        assertThat(libelle).startsWith("Chèque ").contains("Avance — échéance n°1 du contrat N° " + numero + "-");
        assertThat(observation).contains("Avance réglée via chèque n° ").contains("échéance n°1 du contrat N° " + numero + "-");
    }

    @Test
    @DisplayName("Chèque déjà réglé : avance PAYÉE, UNE seule transaction (celle du chèque) dont le libellé et l'observation mentionnent l'avance")
    void chequeRegle_uneSeuleTransaction_annotee() throws Exception {
        long cheque = cheque("3000.00", "DECAISSEMENT", "REGLE");
        long transaction = transactionDuMoyen("CHEQUE", cheque, "Paiement organisme", "Décaissement chèque");
        long contrat = creerContrat("AV-REG", "10000.00", 4, "CHEQUE", cheque, 3, "2500.00");
        long avance = idAvance(contrat);

        assertThat(statutEffectif(avance)).isEqualTo("PAYE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transaction WHERE referent = " + cheque
                + " AND transaction_source = 'CHEQUE'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transaction WHERE transaction_source = 'LEASING' AND referent = "
                + avance, Long.class)).isZero();

        Map<String, Object> t = jdbc.queryForMap("SELECT libelle, observation FROM transaction WHERE id = " + transaction);
        assertThat((String) t.get("libelle")).startsWith("Paiement organisme").contains("Avance — échéance n°1 du contrat N° AV-REG-");
        assertThat((String) t.get("observation")).startsWith("Décaissement chèque").contains("Avance réglée via chèque");
        // propagé au chèque par la synchronisation transaction → source
        assertThat(jdbc.queryForObject("SELECT libelle FROM cheques WHERE id = " + cheque, String.class)).contains("Avance — échéance n°1");
    }

    @Test
    @DisplayName("Le statut suit le moyen de paiement (PAYÉ quand réglé, PRÉVU sinon) — sans copie de statut")
    void statut_suitLeMoyenDePaiement() throws Exception {
        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        long contrat = creerContrat("AV-SUI", "10000.00", 4, "CHEQUE", cheque, 3, "2500.00");
        long avance = idAvance(contrat);

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
        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        long contrat = creerContrat("AV-ANN", "10000.00", 4, "CHEQUE", cheque, 3, "2500.00");
        long avance = idAvance(contrat);

        jdbc.update("UPDATE cheques SET statut = 'ANNULE' WHERE id = " + cheque);

        assertThat(dansLesEncoursLeasing(avance)).isTrue();
        assertThat(statutEffectif(avance)).isEqualTo("PREVU");
    }

    @Test
    @DisplayName("Encaissement, chèque annulé, moyen déjà utilisé par un autre contrat : refusés")
    void choixDuMoyen_refus() throws Exception {
        mockMvc.perform(post("/contratsLeasing").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat("AV-R1", "10000.00", 4, "CHEQUE", cheque("3000.00", "ENCAISSEMENT", "ENCOURS"), 3, "2500.00")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/contratsLeasing").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat("AV-R2", "10000.00", 4, "CHEQUE", cheque("3000.00", "DECAISSEMENT", "ANNULE"), 3, "2500.00")))
                .andExpect(status().isBadRequest());

        long bon = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        creerContrat("AV-R3", "10000.00", 4, "CHEQUE", bon, 3, "2500.00");
        mockMvc.perform(post("/contratsLeasing").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat("AV-R4", "10000.00", 4, "CHEQUE", bon, 3, "2500.00")))
                .andExpect(status().isBadRequest());
    }

    // ── Modification et suppression du contrat ─────────────────────────────────

    @Test
    @DisplayName("Modifier le contrat en changeant de moyen de paiement : l'ancien perd la mention, le nouveau la reçoit")
    void modification_changeDeMoyen() throws Exception {
        long ancien = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        long nouveau = cheque("2000.00", "DECAISSEMENT", "ENCOURS");
        String libelleOrigine = jdbc.queryForObject("SELECT libelle FROM cheques WHERE id = " + ancien, String.class);
        long contrat = creerContrat("AV-MOD", "10000.00", 4, "CHEQUE", ancien, 3, "2500.00");
        assertThat(jdbc.queryForObject("SELECT libelle FROM cheques WHERE id = " + ancien, String.class)).contains("Avance — échéance n°1");

        mockMvc.perform(put("/contratsLeasing/{id}", contrat).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat(numeroDuContrat(contrat), "10000.00", 4, "CHEQUE", nouveau, 3, "2500.00")))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT libelle FROM cheques WHERE id = " + ancien, String.class)).isEqualTo(libelleOrigine);
        assertThat(jdbc.queryForObject("SELECT libelle FROM cheques WHERE id = " + nouveau, String.class)).contains("Avance — échéance n°1");
        assertThat(echeances(contrat).get(0).get("montant_ttc")).hasToString("2000.00");
    }

    @Test
    @DisplayName("Modifier le contrat en gardant le même moyen de paiement (ex. autres phases) : la mention reste, sans doublon")
    void modification_memeMoyen_conserveLaMentionUneSeuleFois() throws Exception {
        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        long contrat = creerContrat("AV-SAME", "10000.00", 4, "CHEQUE", cheque, 3, "2500.00");

        mockMvc.perform(put("/contratsLeasing/{id}", contrat).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat(numeroDuContrat(contrat), "10000.00", 4, "CHEQUE", cheque, 3, "2600.00")))
                .andExpect(status().isOk());

        String libelle = jdbc.queryForObject("SELECT libelle FROM cheques WHERE id = " + cheque, String.class);
        assertThat(libelle.split("Avance — échéance n°1", -1)).hasSize(2);   // une seule mention
        assertThat(echeances(contrat).get(1).get("montant_ttc")).hasToString("2600.00");
        assertThat(echeances(contrat).get(0)).containsEntry("moyen_paiement_type", "CHEQUE");
    }

    @Test
    @DisplayName("Avance déjà payée (moyen réglé) : le contrat ne peut plus être modifié")
    void modification_refuseeSiAvancePayee() throws Exception {
        long cheque = cheque("3000.00", "DECAISSEMENT", "REGLE");
        long contrat = creerContrat("AV-LCK", "10000.00", 4, "CHEQUE", cheque, 3, "2500.00");

        mockMvc.perform(put("/contratsLeasing/{id}", contrat).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat(numeroDuContrat(contrat), "10000.00", 4, "CHEQUE", cheque, 3, "2500.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Supprimer le contrat retire la mention d'avance du moyen de paiement")
    void suppression_retireLaMention() throws Exception {
        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        String libelleOrigine = jdbc.queryForObject("SELECT libelle FROM cheques WHERE id = " + cheque, String.class);
        long contrat = creerContrat("AV-DEL", "10000.00", 4, "CHEQUE", cheque, 3, "2500.00");

        mockMvc.perform(delete("/contratsLeasing/{id}", contrat).cookie(admin)).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT libelle FROM cheques WHERE id = " + cheque, String.class)).isEqualTo(libelleOrigine);
    }

    @Test
    @DisplayName("Une avance rattachée ne se bascule pas à la main comme une échéance")
    void avanceRattachee_bascule_refusee() throws Exception {
        long cheque = cheque("3000.00", "DECAISSEMENT", "ENCOURS");
        long contrat = creerContrat("AV-TOG", "10000.00", 4, "CHEQUE", cheque, 3, "2500.00");

        mockMvc.perform(put("/echeances-leasing/{id}/toggle-status", idAvance(contrat)).cookie(admin))
                .andExpect(status().isBadRequest());
    }

    // ── Liste déroulante paginée ───────────────────────────────────────────────

    @Test
    @DisplayName("Liste des moyens disponibles : paginée, filtrée sur le sens et le type, sans ceux déjà utilisés")
    void moyensDisponibles_pagines() throws Exception {
        String repere = "REP" + UNICITE.incrementAndGet();
        long libre1 = chequeNumero("1000.00", "DECAISSEMENT", "ENCOURS", repere + "A");
        long libre2 = chequeNumero("1100.00", "DECAISSEMENT", "REGLE", repere + "B");
        chequeNumero("1200.00", "ENCAISSEMENT", "ENCOURS", repere + "C");   // encaissement : exclu
        chequeNumero("1300.00", "DECAISSEMENT", "ANNULE", repere + "D");    // annulé : exclu
        long pris = chequeNumero("3000.00", "DECAISSEMENT", "ENCOURS", repere + "E");
        creerContrat("AV-LST", "10000.00", 4, "CHEQUE", pris, 3, "2500.00");

        mockMvc.perform(get("/echeances-leasing/avance/moyens-disponibles").cookie(admin)
                        .param("type", "CHEQUE").param("recherche", repere).param("size", "1").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mockMvc.perform(get("/echeances-leasing/avance/moyens-disponibles").cookie(admin)
                        .param("type", "CHEQUE").param("recherche", repere).param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + libre1 + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + libre2 + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + pris + ")]").doesNotExist());
    }

    @Test
    @DisplayName("La recherche porte aussi sur le montant : « 7777,77 », « 7777.77 », « 7 777,77 » ou « 7777 » retrouvent le moyen de paiement")
    void moyensDisponibles_rechercheParMontant() throws Exception {
        long cible = cheque("7777.77", "DECAISSEMENT", "ENCOURS");

        for (String saisie : List.of("7777,77", "7777.77", "7 777,77", "7777")) {
            mockMvc.perform(get("/echeances-leasing/avance/moyens-disponibles").cookie(admin)
                            .param("type", "CHEQUE").param("recherche", saisie).param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.id == " + cible + ")]").exists());
        }
        mockMvc.perform(get("/echeances-leasing/avance/moyens-disponibles").cookie(admin)
                        .param("type", "CHEQUE").param("recherche", "7777,78").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + cible + ")]").doesNotExist());
    }

    // ── Données ────────────────────────────────────────────────────────────────

    private long creerContrat(String numero, String achat, int periode, String type, long moyen, int dureePhase, String mensualite)
            throws Exception {
        String reponse = mockMvc.perform(post("/contratsLeasing").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsContrat(numero + "-" + UNICITE.incrementAndGet(), achat, periode, type, moyen, dureePhase, mensualite)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(reponse.replaceAll("^\\{\"id\":(\\d+).*$", "$1"));
    }

    /** {@code type} null (ou {@code moyen} 0) : contrat sans avance. */
    private String corpsContrat(String numero, String achat, int periode, String type, long moyen, int dureePhase, String mensualite) {
        String avance = type != null && moyen != 0
                ? ",\"avanceMoyenPaiement\":{\"type\":\"" + type + "\",\"moyenPaiementId\":" + moyen + "}"
                : "";
        return "{\"numeroContrat\":\"" + numero + "\",\"banqueDePrelevementId\":" + banque
                + ",\"organismeLeasingId\":" + organisme + ",\"immobilisation\":\"Engin\",\"montantAchatTTC\":" + achat
                + ",\"dateDebut\":\"2026-11-01\",\"periodeEnMois\":" + periode + ",\"typeContrat\":\"LINEAIRE\""
                + ",\"valeurResiduelle\":1.00" + avance
                + ",\"phases\":[{\"numeroPhase\":1,\"dureeMois\":" + dureePhase + ",\"mensualiteTTC\":" + mensualite + "}]}";
    }

    private long cheque(String montant, String sens, String statut) {
        return chequeNumero(montant, sens, statut, "CH" + UNICITE.incrementAndGet());
    }

    private long chequeNumero(String montant, String sens, String statut, String numero) {
        return jdbc.queryForObject("INSERT INTO cheques (date_emission, montant, banque_id, beneficiaire, libelle, observation, "
                + "statut, type, numero) VALUES (CURRENT_DATE, " + montant + ", " + banque + ", 'Organisme', 'Chèque " + numero
                + "', 'Décaissement " + numero + "', '" + statut + "', '" + sens + "', '" + numero + "') RETURNING id", Long.class);
    }

    /** Transaction (dans un rapprochement en cours) issue de la validation du chèque : source CHEQUE, référent = le chèque. */
    private long transactionDuMoyen(String source, long referent, String libelle, String observation) {
        long rapprochement = jdbc.queryForObject("INSERT INTO rapprochement (banque_id, created_at, updated_at, statut, date_rapprochement) "
                + "VALUES (" + banque + ", now(), now(), 'EN_COURS', DATE '2026-11-01') RETURNING id", Long.class);
        return jdbc.queryForObject("INSERT INTO transaction (rapprochement_id, transaction_source, referent, banque_id, debit, credit, "
                + "date_valeur, date_operations, date_emission, libelle, observation, created_at, updated_at) VALUES ("
                + rapprochement + ", '" + source + "', " + referent + ", " + banque + ", 3000.00, 0, CURRENT_DATE, CURRENT_DATE, "
                + "CURRENT_DATE, '" + libelle + "', '" + observation + "', now(), now()) RETURNING id", Long.class);
    }

    private String numeroDuContrat(long contrat) {
        return jdbc.queryForObject("SELECT numero_contrat FROM contrat_leasing WHERE id = " + contrat, String.class);
    }

    private List<Map<String, Object>> echeances(long contrat) {
        return jdbc.queryForList("SELECT numero_echeance, date_echeance, montant_ttc, statut_paiement, moyen_paiement_type FROM echeance_leasing "
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

    /** Statut effectif tel que le lit l'application (même formule que les requêtes de l'entité). */
    private String statutEffectif(long echeance) {
        return jdbc.queryForObject("SELECT CASE WHEN el.moyen_paiement_id IS NULL THEN el.statut_paiement "
                + "ELSE COALESCE((SELECT CASE WHEN vm.statut_moyen = 'REGLE' THEN 'PAYE' ELSE 'PREVU' END "
                + "FROM v_echeance_leasing_moyen vm WHERE vm.echeance_id = el.id), 'PREVU') END "
                + "FROM echeance_leasing el WHERE el.id = " + echeance, String.class);
    }

    private BigDecimal dejaPayeDuContrat(long contrat) throws Exception {
        String reponse = mockMvc.perform(get("/echeances-leasing/tableau-amortissement/{id}", contrat).cookie(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"montantTotalDejaPaye\":([0-9.]+)").matcher(reponse);
        assertThat(m.find()).as("montantTotalDejaPaye dans %s", reponse).isTrue();
        return new BigDecimal(m.group(1));
    }
}
