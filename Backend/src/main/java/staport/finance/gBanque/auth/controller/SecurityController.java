package staport.finance.gBanque.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.auth.dtos.request.LoginRequest;
import staport.finance.gBanque.auth.dtos.response.ProfileResponse;
import staport.finance.gBanque.auth.services.AuthCookieService;
import staport.finance.gBanque.auth.services.AuthenticationService;
import staport.finance.gBanque.exception.customExceptions.InvalidRefreshTokenException;

import java.util.Optional;

@Tag(name = "Authentification", description = "Connexion, renouvellement et fin de session, profil de l'utilisateur courant")
@RestController
@RequestMapping("/auth")
public class SecurityController {

    private final AuthenticationService authenticationService;
    private final AuthCookieService authCookieService;

    public SecurityController(AuthenticationService authenticationService, AuthCookieService authCookieService) {
        this.authenticationService = authenticationService;
        this.authCookieService = authCookieService;
    }

    /**
     * Retourne le profil de l'appelant — dérivé uniquement de son propre
     * {@code Authentication} (username + authorities), jamais d'un autre
     * utilisateur. Accessible à tout utilisateur authentifié (MEMBER,
     * DIRECTOR, SYSTEM_ADMIN).
     */
    @Operation(
            summary = "Profil de l'utilisateur connecté",
            description = "Retourne le username et les authorities de la session courante. Accessible à tout rôle authentifié.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "401", description = "Session absente ou expirée")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    public ProfileResponse authentication(Authentication authentication) {
        return this.authenticationService.getAuthentication(authentication);
    }

    @Operation(
            summary = "Connexion",
            description = "Authentifie l'utilisateur, ouvre une session et pose les cookies HttpOnly de session "
                    + "(jeton d'accès JWT_ACCESS_TOKEN_MINUTES, durée maximale de session JWT_REFRESH_TOKEN_HOURS). "
                    + "Le seuil de verrouillage est configuré via LOGIN_MAX_ATTEMPTS et LOGIN_LOCKOUT_MINUTES."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Corps de requête invalide (champ manquant ou vide)"),
            @ApiResponse(responseCode = "401", description = "Identifiants incorrects"),
            @ApiResponse(responseCode = "429", description = "Compte temporairement verrouillé — trop de tentatives")
    })
    @PostMapping("/login")
    public ProfileResponse login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        AuthenticationService.Session session =
                this.authenticationService.login(loginRequest.username(), loginRequest.password());
        authCookieService.writeSession(response, session.accessToken(), session.refreshToken());
        return session.profile();
    }

    @Operation(
            summary = "Renouvellement de la session",
            description = "Échange le cookie de rafraîchissement contre de nouveaux cookies de session (rotation). "
                    + "204 si une autre requête de la même session vient de le faire : les cookies sont déjà à jour."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session renouvelée"),
            @ApiResponse(responseCode = "204", description = "Session déjà renouvelée à l'instant par une autre requête"),
            @ApiResponse(responseCode = "401", description = "Session terminée — reconnexion nécessaire"),
            @ApiResponse(responseCode = "403", description = "Jeton CSRF absent ou invalide")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ProfileResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        Optional<AuthenticationService.Session> session;
        try {
            session = authenticationService.refresh(
                    AuthCookieService.read(request, AuthCookieService.REFRESH_COOKIE).orElse(null));
        } catch (InvalidRefreshTokenException e) {
            authCookieService.clearSession(response);
            throw e;
        }
        if (session.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        authCookieService.writeSession(response, session.get().accessToken(), session.get().refreshToken());
        return ResponseEntity.ok(session.get().profile());
    }

    @Operation(
            summary = "Déconnexion",
            description = "Révoque la session côté serveur et efface les cookies de session. Toujours 204, même sans session."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Session terminée"),
            @ApiResponse(responseCode = "403", description = "Jeton CSRF absent ou invalide")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authenticationService.logout(AuthCookieService.read(request, AuthCookieService.REFRESH_COOKIE).orElse(null));
        authCookieService.clearSession(response);
        return ResponseEntity.noContent().build();
    }
}
