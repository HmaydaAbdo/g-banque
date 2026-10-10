package staport.finance.gBanque.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import staport.finance.gBanque.AbstractIntegrationTest;
import staport.finance.gBanque.auth.entities.RefreshToken;
import staport.finance.gBanque.auth.repositories.RefreshTokenRepository;
import staport.finance.gBanque.auth.services.AuthCookieService;
import staport.finance.gBanque.user.entities.User;
import staport.finance.gBanque.user.repositories.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sessions par cookies HttpOnly : connexion, accès, rafraîchissement (rotation, réutilisation),
 * déconnexion et protection CSRF — sur le vrai contexte Spring et un vrai MySQL.
 */
class AuthSessionIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "MotDePasse-Test-1";
    private static final String XSRF_COOKIE = "XSRF-TOKEN";
    private static final String XSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    private String username;

    @BeforeEach
    void createUser() {
        username = "it-" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User(username, passwordEncoder.encode(PASSWORD), true);
        user.setSystemAdmin(true);
        userRepository.save(user);
    }

    @Test
    @DisplayName("Connexion : profil dans le corps, jetons uniquement en cookies HttpOnly, Secure, SameSite=Strict")
    void login_poseLesCookiesEtNeRenvoiePasLeJeton() throws Exception {
        MvcResult result = login()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.authorities[0]").value("SYSTEM_ADMIN"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        for (String name : List.of(AuthCookieService.ACCESS_COOKIE, AuthCookieService.REFRESH_COOKIE)) {
            String header = setCookies.stream().filter(h -> h.startsWith(name + "=")).findFirst().orElseThrow();
            assertThat(header).contains("HttpOnly", "Secure", "SameSite=Strict");
        }
        assertThat(setCookies.stream().filter(h -> h.startsWith(AuthCookieService.REFRESH_COOKIE + "=")).findFirst().orElseThrow())
                .contains("Path=/gbanque/api/auth");
        assertThat(cookie(result, XSRF_COOKIE)).isNotNull();
    }

    @Test
    @DisplayName("Mauvais mot de passe : 401, aucun cookie de session")
    void login_mauvaisMotDePasse_401() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "mauvais-mdp")))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertThat(cookie(result, AuthCookieService.ACCESS_COOKIE)).isNull();
    }

    @Test
    @DisplayName("Accès : cookie d'accès accepté, en-tête Bearer accepté en secours, rien => 401")
    void profile_cookieOuBearer() throws Exception {
        MvcResult loginResult = login().andReturn();
        Cookie access = cookie(loginResult, AuthCookieService.ACCESS_COOKIE);

        mockMvc.perform(get("/auth/profile").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
        mockMvc.perform(get("/auth/profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + access.getValue()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rafraîchissement sans jeton CSRF : 403")
    void refresh_sansCsrf_403() throws Exception {
        MvcResult loginResult = login().andReturn();
        mockMvc.perform(post("/auth/refresh").cookie(cookie(loginResult, AuthCookieService.REFRESH_COOKIE)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rafraîchissement : nouveaux cookies, ancien jeton rejoué aussitôt => 204 (deux onglets), sans révocation")
    void refresh_rotationEtCourseEntreOnglets() throws Exception {
        MvcResult loginResult = login().andReturn();
        Cookie refresh = cookie(loginResult, AuthCookieService.REFRESH_COOKIE);
        Cookie xsrf = cookie(loginResult, XSRF_COOKIE);

        MvcResult refreshed = refresh(refresh, xsrf)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andReturn();
        Cookie newRefresh = cookie(refreshed, AuthCookieService.REFRESH_COOKIE);
        assertThat(newRefresh.getValue()).isNotEqualTo(refresh.getValue());
        assertThat(cookie(refreshed, AuthCookieService.ACCESS_COOKIE)).isNotNull();

        refresh(refresh, xsrf).andExpect(status().isNoContent());
        refresh(newRefresh, xsrf).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Réutilisation d'un jeton déjà échangé (hors délai de grâce) : 401 et toute la session est révoquée")
    void refresh_reutilisation_revoqueLaSession() throws Exception {
        MvcResult loginResult = login().andReturn();
        Cookie refresh = cookie(loginResult, AuthCookieService.REFRESH_COOKIE);
        Cookie xsrf = cookie(loginResult, XSRF_COOKIE);
        Cookie newRefresh = cookie(refresh(refresh, xsrf).andReturn(), AuthCookieService.REFRESH_COOKIE);

        // Le jeton d'origine a été échangé il y a une minute (au-delà du délai de grâce)
        RefreshToken original = refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUsername().equals(username) && t.getRotatedAt() != null)
                .findFirst().orElseThrow();
        original.markRotated(Instant.now().minusSeconds(60));
        refreshTokenRepository.save(original);

        refresh(refresh, xsrf).andExpect(status().isUnauthorized());
        refresh(newRefresh, xsrf).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Déconnexion : session révoquée côté serveur, cookies effacés ; sans CSRF => 403")
    void logout_revoqueLaSession() throws Exception {
        MvcResult loginResult = login().andReturn();
        Cookie refresh = cookie(loginResult, AuthCookieService.REFRESH_COOKIE);
        Cookie xsrf = cookie(loginResult, XSRF_COOKIE);

        mockMvc.perform(post("/auth/logout").cookie(refresh)).andExpect(status().isForbidden());

        MvcResult logout = mockMvc.perform(post("/auth/logout").cookie(refresh, xsrf).header(XSRF_HEADER, xsrf.getValue()))
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(cookie(logout, AuthCookieService.ACCESS_COOKIE).getMaxAge()).isZero();
        assertThat(cookie(logout, AuthCookieService.REFRESH_COOKIE).getMaxAge()).isZero();

        refresh(refresh, xsrf).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Compte désactivé après connexion : le rafraîchissement est refusé")
    void refresh_compteDesactive_401() throws Exception {
        MvcResult loginResult = login().andReturn();
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        refresh(cookie(loginResult, AuthCookieService.REFRESH_COOKIE), cookie(loginResult, XSRF_COOKIE))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private org.springframework.test.web.servlet.ResultActions login() throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(username, PASSWORD)));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(Cookie refresh, Cookie xsrf) throws Exception {
        return mockMvc.perform(post("/auth/refresh").cookie(refresh, xsrf).header(XSRF_HEADER, xsrf.getValue()));
    }

    private static String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private static Cookie cookie(MvcResult result, String name) {
        return result.getResponse().getCookie(name);
    }
}
