package staport.finance.gBanque.recapitulatif;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import staport.finance.gBanque.AbstractIntegrationTest;
import staport.finance.gBanque.auth.services.AuthCookieService;
import staport.finance.gBanque.user.entities.User;
import staport.finance.gBanque.user.repositories.UserRepository;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Trésorerie prévisionnelle et horizon fin de trimestre, sur le vrai contexte Spring et la vraie
 * vue {@code v_recapitulatif_encours} : la requête d'agrégation s'exécute, l'horizon est appliqué,
 * et les endpoints sont réservés à Finance / SYSTEM_ADMIN (cf. docs/rbac/README.md).
 */
class TresoreriePrevisionnelleIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "MotDePasse-Test-1";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Administrateur : plan fin de trimestre, colonne « À ce jour » en tête, PDF consolidé")
    void administrateur_accesAutorise() throws Exception {
        Cookie session = connecter(true);

        mockMvc.perform(get("/tresorerie-previsionnelle").param("periode", "FIN_DE_TRIMESTRE").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periode").value("FIN_DE_TRIMESTRE"))
                .andExpect(jsonPath("$.colonnes[0].code").value("ECHU"))
                .andExpect(jsonPath("$.groupes").isArray());

        mockMvc.perform(get("/tresorerie-previsionnelle/pdf").cookie(session))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        mockMvc.perform(get("/banques/accueil-info").param("periode", "FIN_DE_TRIMESTRE").cookie(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Banque inconnue pour l'export PDF : 404")
    void exportPdf_banqueInconnue_404() throws Exception {
        mockMvc.perform(get("/tresorerie-previsionnelle/pdf").param("banqueId", "987654").cookie(connecter(true)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Utilisateur hors Finance : 403 sur le plan et sur son export")
    void horsFinance_accesRefuse() throws Exception {
        Cookie session = connecter(false);

        mockMvc.perform(get("/tresorerie-previsionnelle").cookie(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/tresorerie-previsionnelle/pdf").cookie(session)).andExpect(status().isForbidden());
    }

    /** Crée un utilisateur (administrateur ou sans département) et renvoie son cookie de session. */
    private Cookie connecter(boolean administrateur) throws Exception {
        String username = "it-treso-" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User(username, passwordEncoder.encode(PASSWORD), true);
        user.setSystemAdmin(administrateur);
        userRepository.save(user);

        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthCookieService.ACCESS_COOKIE);
    }
}
