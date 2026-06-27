package staport.finance.gBanque.auth.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Profil de l'utilisateur courant, extrait du token JWT")
public record ProfileResponse(
        @Schema(description = "Nom d'utilisateur")
        String username,

        @Schema(description = "Liste des authorities Spring Security (ex. SYSTEM_ADMIN, DEPT_TRESORERIE:DIRECTOR)")
        List<String> roles
) {
}
