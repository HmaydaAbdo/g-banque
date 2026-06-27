package staport.finance.gBanque.auth.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Réponse renvoyée après une authentification réussie")
public record LoginResponse(
        @Schema(description = "Token JWT à passer dans le header Authorization: Bearer <token>")
        String accessToken,

        @Schema(description = "Nom d'utilisateur authentifié")
        String username,

        @Schema(description = "Liste des authorities Spring Security (ex. SYSTEM_ADMIN, DEPT_TRESORERIE:DIRECTOR)")
        List<String> authorities
) {
}
