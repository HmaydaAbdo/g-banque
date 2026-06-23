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
 * Les appels effectués depuis l'UI utilisent le bouton "Authorize" pour passer le
 * token JWT ; Spring Security valide ce token exactement comme un appel normal.
 */
@Configuration
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtenu via POST /auth/login — à passer dans le header Authorization: Bearer <token>"
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
