package staport.finance.gBanque.commision;

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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Régression : huit commissions de production portent l'opération « Importation » (frais d'importation payés au
 * 27/08 – 24/09/2026). La valeur avait disparu de l'énumération et ouvrir l'une d'elles renvoyait une erreur 500.
 */
class CommissionImportationIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "MotDePasse-Test-1";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Une commission d'opération « Importation » s'ouvre (200) au lieu de renvoyer une erreur 500")
    void commissionImportation_sOuvre() throws Exception {
        Long id = insererCommissionImportation();

        mockMvc.perform(get("/commissions/" + id).cookie(connexionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("Importation"));
    }

    /** Insère une banque et une commission minimales (les clés étrangères sont suspendues le temps de l'insertion). */
    private Long insererCommissionImportation() {
        return jdbcTemplate.execute((java.sql.Connection c) -> {
            try (Statement st = c.createStatement()) {
                st.execute("SET session_replication_role = replica");
                st.execute("INSERT INTO banque (id, date_creation, date_modification, numero_compte, agence, nom, address, devise, logo_data) "
                        + "VALUES (900001, now(), now(), '000', 'AGENCE', 'BANQUE ESSAI', 'ADRESSE', 'MAD', decode('00', 'hex'))");
                try (var rs = st.executeQuery("INSERT INTO commission (banque_id, montant, operation, status, libelle, observation) "
                        + "VALUES (900001, 132.00, 'Importation', 'PAYE', 'FRAIS IMPORTATION', 'CREDOC') RETURNING id")) {
                    rs.next();
                    return rs.getLong(1);
                } finally {
                    st.execute("SET session_replication_role = DEFAULT");
                }
            }
        });
    }

    private Cookie connexionAdmin() throws Exception {
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
