package staport.finance.gBanque.auth.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger unique pour l'application.
 *
 * <p>Déclare le schéma de sécurité JWT {@code bearerAuth} — référencé par
 * {@code @SecurityRequirement(name = "bearerAuth")} sur chaque controller
 * authentifié. Ne jamais redéfinir ce scheme ailleurs.
 *
 * <p>UI disponible à {@code /swagger-ui.html} — activée/désactivée via la variable
 * d'environnement {@code SWAGGER_ENABLED} ({@code false} en prod, {@code true} en dev).
 * Deux façons de s'authentifier depuis l'UI : appeler {@code POST /auth/login} (le navigateur garde
 * alors les cookies de session, les appels suivants les envoient seuls ; les requêtes modifiantes
 * exigent en plus l'en-tête {@code X-XSRF-TOKEN}), ou coller un JWT via le bouton "Authorize"
 * (en-tête Bearer, accepté en secours et dispensé de CSRF).
 */
@Configuration
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT",
        description = "JWT d'accès (valeur du cookie GBANQUE_ACCESS posé par POST /auth/login) passé dans le header Authorization: Bearer <token>"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI gBanqueOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("G-Banque API")
                        .description("API REST de gestion et suivi des opérations bancaires internes")
                        .version("1.0.0"));
    }
}
