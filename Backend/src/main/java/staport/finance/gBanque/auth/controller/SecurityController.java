package staport.finance.gBanque.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.auth.dtos.request.LoginRequest;
import staport.finance.gBanque.auth.dtos.response.LoginResponse;
import staport.finance.gBanque.auth.dtos.response.ProfileResponse;
import staport.finance.gBanque.auth.services.AuthenticationService;

@Tag(name = "Authentification", description = "Connexion et consultation du profil de l'utilisateur courant")
@RestController
@RequestMapping("/auth")
public class SecurityController {

    private final AuthenticationService authenticationService;

    public SecurityController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Retourne le profil de l'appelant — dérivé uniquement de son propre
     * {@code Authentication} (username + authorities), jamais d'un autre
     * utilisateur. Accessible à tout utilisateur authentifié (MEMBER,
     * DIRECTOR, SYSTEM_ADMIN).
     */
    @Operation(
            summary = "Profil de l'utilisateur connecté",
            description = "Retourne le username et les authorities tirés du token JWT. Accessible à tout rôle authentifié.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "401", description = "Token absent ou expiré")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    public ProfileResponse authentication(Authentication authentication) {
        return this.authenticationService.getAuthentication(authentication);
    }

    @Operation(
            summary = "Connexion (obtention du token JWT)",
            description = "Authentifie l'utilisateur et retourne un token Bearer. " +
                    "La durée de validité et le seuil de verrouillage sont configurés " +
                    "via les variables d'environnement JWT_EXPIRATION_HOURS, " +
                    "LOGIN_MAX_ATTEMPTS et LOGIN_LOCKOUT_MINUTES."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Corps de requête invalide (champ manquant ou vide)"),
            @ApiResponse(responseCode = "401", description = "Identifiants incorrects"),
            @ApiResponse(responseCode = "429", description = "Compte temporairement verrouillé — trop de tentatives")
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return this.authenticationService.login(loginRequest.username(), loginRequest.password());
    }
}
