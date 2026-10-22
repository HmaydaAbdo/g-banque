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
import staport.finance.gBanque.AbstractIntegrationTest;
import staport.finance.gBanque.auth.services.AuthCookieService;
import staport.finance.gBanque.user.entities.User;
import staport.finance.gBanque.user.repositories.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Une règle de commission leasing ajoutée après la saisie d'un contrat ne s'applique pas seule : le rattrapage (action
 * d'administration et migration V20) crée les commissions des échéances prévues, sans toucher aux échéances payées et
 * sans jamais créer de doublon.
 */
class LeasingRattrapageCommissionsIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "MotDePasse-Test-1";
    private static final AtomicLong UNICITE = new AtomicLong(900_000);

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
                + "address, devise, logo_data) VALUES (CURRENT_DATE, CURRENT_DATE, 'RC" + n + "', 'Agence', 'Banque rattrapage " + n
                + "', 'Adresse', 'MAD', decode('00', 'hex')) RETURNING id", Long.class);
        organisme = jdbc.queryForObject("INSERT INTO organisme_leasing (date_creation, date_modification, nom) "
                + "VALUES (now(), now(), 'Organisme rattrapage " + n + "') RETURNING id", Long.class);
        jdbc.update("INSERT INTO contrat_cadre (date_debut, date_fin, plafond, date_creation, date_modification, organisme_id, statut) "
                + "VALUES ('2020-01-01', '2099-12-31', 100000000, now(), now(), " + organisme + ", 'ACTIF')");
    }

    @Test
    @DisplayName("Règle ajoutée après le contrat : le rattrapage crée la commission des échéances prévues seulement, et se rejoue sans doublon")
    void rattrapage_echeancesPrevuesSeulement_etIdempotent() throws Exception {
        long contrat = creerContrat();
        assertThat(commissions()).isEmpty();   // aucune règle à la saisie : aucune commission

        jdbc.update("UPDATE echeance_leasing SET statut_paiement = 'PAYE' WHERE contrat_leasing_id = " + contrat + " AND numero_echeance = 1");
        regleParDefaut("11.00");

        mockMvc.perform(post("/echeances-leasing/commissions/rattrapage").cookie(admin)).andExpect(status().isOk());

        List<Map<String, Object>> commissions = commissions();
        assertThat(commissions).hasSize(3);   // 4 échéances (3 mensualités + valeur résiduelle) − 1 payée
        assertThat(commissions).allSatisfy(c -> {
            assertThat(c.get("montant")).hasToString("11.0000");
            assertThat(c.get("status")).isEqualTo("PREVU");
        });
        assertThat(commissions.stream().map(c -> c.get("date_emission").toString()))
                .doesNotContain("2026-11-01")   // date de l'échéance payée
                .containsExactly("2026-12-01", "2027-01-01", "2027-02-01");

        mockMvc.perform(post("/echeances-leasing/commissions/rattrapage").cookie(admin)).andExpect(status().isOk());
        assertThat(commissions()).hasSize(3);
    }

    @Test
    @DisplayName("Banque sans règle leasing : le rattrapage ne crée rien")
    void rattrapage_sansRegle_neCreeRien() throws Exception {
        creerContrat();

        mockMvc.perform(post("/echeances-leasing/commissions/rattrapage").cookie(admin)).andExpect(status().isOk());

        assertThat(commissions()).isEmpty();
    }

    @Test
    @DisplayName("Migration V20 : mêmes commissions que le rattrapage, rejouable sans doublon")
    void migration_memeResultat_etIdempotente() throws Exception {
        long contrat = creerContrat();
        regleParDefaut("11.00");
        jdbc.update("UPDATE echeance_leasing SET statut_paiement = 'PAYE' WHERE contrat_leasing_id = " + contrat + " AND numero_echeance = 1");

        String sql = new String(getClass().getClassLoader().getResourceAsStream("db/migration/V20__rattrapage_commissions_leasing.sql")
                .readAllBytes(), StandardCharsets.UTF_8);
        jdbc.execute(sql);
        assertThat(commissions()).hasSize(3);

        jdbc.execute(sql);
        assertThat(commissions()).hasSize(3);

        mockMvc.perform(post("/echeances-leasing/commissions/rattrapage").cookie(admin)).andExpect(status().isOk());
        assertThat(commissions()).hasSize(3);   // le rattrapage reconnaît les commissions créées par la migration
    }

    private long creerContrat() throws Exception {
        String numero = "RC-" + UNICITE.incrementAndGet();
        String reponse = mockMvc.perform(post("/contratsLeasing").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroContrat\":\"" + numero + "\",\"banqueDePrelevementId\":" + banque
                                + ",\"organismeLeasingId\":" + organisme + ",\"immobilisation\":\"Engin\",\"montantAchatTTC\":10000.00"
                                + ",\"dateDebut\":\"2026-11-01\",\"periodeEnMois\":3,\"typeContrat\":\"LINEAIRE\",\"valeurResiduelle\":1.00"
                                + ",\"phases\":[{\"numeroPhase\":1,\"dureeMois\":3,\"mensualiteTTC\":4000.00}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(reponse.replaceAll("^\\{\"id\":(\\d+).*$", "$1"));
    }

    private void regleParDefaut(String montant) {
        jdbc.update("INSERT INTO commission_config (banque_id, montant, type, commission_condition) VALUES (" + banque + ", " + montant
                + ", 'LEASING', 'DEFAULT')");
    }

    private List<Map<String, Object>> commissions() {
        return jdbc.queryForList("SELECT montant, status, date_emission FROM commission WHERE operation = 'LEASING' AND banque_id = "
                + banque + " ORDER BY date_emission");
    }
}
