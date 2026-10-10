package staport.finance.gBanque.auth.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import staport.finance.gBanque.auth.dtos.response.ProfileResponse;
import staport.finance.gBanque.exception.customExceptions.InvalidRefreshTokenException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Sessions utilisateur : JWT d'accès court ({@code jwt.access-token-minutes}) renouvelé en silence
 * par un jeton de rafraîchissement ({@code jwt.refresh-token-hours}, durée maximale de la session).
 * Les deux voyagent en cookies {@code HttpOnly} posés par le controller (cf. {@link AuthCookieService}).
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final LoginAttemptService loginAttemptService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsServiceImpl userDetailsService;
    private final long accessTokenMinutes;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 JwtEncoder jwtEncoder,
                                 LoginAttemptService loginAttemptService,
                                 RefreshTokenService refreshTokenService,
                                 UserDetailsServiceImpl userDetailsService,
                                 @Value("${jwt.access-token-minutes}") long accessTokenMinutes) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.loginAttemptService = loginAttemptService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    /** Jetons à poser en cookies et profil à renvoyer au client. */
    public record Session(String accessToken, RefreshTokenService.IssuedRefreshToken refreshToken, ProfileResponse profile) {
    }

    /**
     * Protection brute-force : un compte verrouillé ({@link LoginAttemptService}) lève
     * avant même de solliciter l'{@code AuthenticationManager} ; chaque échec incrémente
     * le compteur, chaque succès le réinitialise.
     */
    public Session login(String username, String password) {

        loginAttemptService.checkNotLocked(username);

        Authentication authentication;
        try {
            authentication = this.authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(username);
            log.warn("Authentification échouée pour username={}", username);
            throw ex;
        }
        loginAttemptService.recordSuccess(username);
        log.info("Connexion réussie username={}", username);

        List<String> authorities = toNames(authentication.getAuthorities());
        return new Session(
                generateAccessToken(username, authorities),
                refreshTokenService.issueForNewSession(username),
                new ProfileResponse(username, authorities));
    }

    /**
     * Renouvelle la session à partir du jeton de rafraîchissement. Les droits sont relus en base :
     * un changement de rôle ou une désactivation du compte s'applique au plus tard au rafraîchissement suivant.
     *
     * @return la nouvelle session, ou vide si une autre requête de la même session vient de la renouveler
     *         (les cookies du navigateur sont alors déjà à jour)
     * @throws InvalidRefreshTokenException si la session est terminée ou le compte désactivé
     */
    public Optional<Session> refresh(String rawRefreshToken) {
        RefreshTokenService.RefreshOutcome outcome = refreshTokenService.rotate(rawRefreshToken);
        if (!(outcome instanceof RefreshTokenService.RefreshOutcome.Rotated rotated)) {
            return Optional.empty();
        }

        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(rotated.username());
        } catch (RuntimeException e) {
            refreshTokenService.revokeSession(rotated.newToken().rawToken());
            throw new InvalidRefreshTokenException("Votre session a expiré, veuillez vous reconnecter.");
        }
        if (!user.isEnabled()) {
            refreshTokenService.revokeSession(rotated.newToken().rawToken());
            log.warn("Rafraîchissement refusé : compte désactivé username={}", rotated.username());
            throw new InvalidRefreshTokenException("Votre session a expiré, veuillez vous reconnecter.");
        }

        List<String> authorities = toNames(user.getAuthorities());
        return Optional.of(new Session(
                generateAccessToken(user.getUsername(), authorities),
                rotated.newToken(),
                new ProfileResponse(user.getUsername(), authorities)));
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeSession(rawRefreshToken);
    }

    /** JWT signé HS256, valable {@code jwt.access-token-minutes} (variable {@code JWT_ACCESS_TOKEN_MINUTES}). */
    private String generateAccessToken(String username, List<String> authorities) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(accessTokenMinutes, ChronoUnit.MINUTES))
                .subject(username)
                .claim("authorities", authorities)
                .build();

        JwtEncoderParameters jwtEncoderParameters = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), jwtClaimsSet);

        return jwtEncoder.encode(jwtEncoderParameters).getTokenValue();
    }

    public ProfileResponse getAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated or authentication object is null.");
        }
        return new ProfileResponse(authentication.getName(), toNames(authentication.getAuthorities()));
    }

    private static List<String> toNames(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }
}
