package staport.finance.gBanque.recapitulatif;

import jakarta.servlet.http.Cookie;
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

import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /recapitulatif/toutes-banques} : les encours de toutes les banques jusqu'à l'horizon, avec leur banque et leur
 * date de référence, en un seul appel (page globale de la Trésorerie prévisionnelle).
 */
class EncoursToutesBanquesIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "MotDePasse-Test-1";
    private static final long BANQUE_A = 930_001L;
    private static final long BANQUE_B = 930_002L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Renvoie les encours des deux banques jusqu'à la fin du mois, avec banque et date de référence ; pas ceux d'après")
    void encoursDeToutesLesBanques() throws Exception {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate apresHorizon = aujourdhui.withDayOfMonth(1).plusMonths(2);
        donnees(aujourdhui, apresHorizon);

        mockMvc.perform(get("/recapitulatif/toutes-banques").param("periode", "FIN_DE_MOIS").cookie(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.banqueId == " + BANQUE_A + ")].libelle").value(hasItem("FOURNISSEUR A")))
                .andExpect(jsonPath("$[?(@.banqueId == " + BANQUE_B + ")].libelle").value(hasItem("FOURNISSEUR B")))
                .andExpect(jsonPath("$[?(@.libelle == 'FOURNISSEUR A')].dateReference").value(hasItem(aujourdhui.toString())))
                .andExpect(jsonPath("$[*].libelle").value(not(hasItem("PLUS TARD"))));
    }

    private void donnees(LocalDate aujourdhui, LocalDate apresHorizon) {
        jdbc.execute((java.sql.Connection c) -> {
            try (Statement st = c.createStatement()) {
                st.execute("SET session_replication_role = replica");
                for (long id : new long[]{BANQUE_A, BANQUE_B}) {
                    st.execute("INSERT INTO banque (id, date_creation, date_modification, numero_compte, agence, nom, address, devise, logo_data) "
                            + "VALUES (" + id + ", now(), now(), 'IT-TB-" + id + "', 'AGENCE', 'BANQUE " + id + "', 'ADRESSE', 'MAD', decode('00', 'hex')) "
                            + "ON CONFLICT DO NOTHING");
                }
                st.execute(cheque(BANQUE_A, "FOURNISSEUR A", aujourdhui, "1"));
                st.execute(cheque(BANQUE_B, "FOURNISSEUR B", aujourdhui, "2"));
                st.execute(cheque(BANQUE_A, "PLUS TARD", apresHorizon, "3"));
                st.execute("SET session_replication_role = DEFAULT");
                return null;
            }
        });
    }

    private static String cheque(long banque, String beneficiaire, LocalDate date, String suffixe) {
        return "INSERT INTO cheques (banque_id, montant, date_emission, beneficiaire, emetteur, libelle, observation, numero, statut, type) "
                + "VALUES (" + banque + ", 1000.00, '" + date + "', '" + beneficiaire + "', 'STAPORT', 'Règlement', 'essai', "
                + "'IT-TB-" + suffixe + "-" + UUID.randomUUID().toString().substring(0, 6) + "', 'ENCOURS', 'DECAISSEMENT')";
    }

    private Cookie admin() throws Exception {
        String username = "it-" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User(username, passwordEncoder.encode(PASSWORD), true);
        user.setSystemAdmin(true);
        userRepository.save(user);
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthCookieService.ACCESS_COOKIE);
    }
}
