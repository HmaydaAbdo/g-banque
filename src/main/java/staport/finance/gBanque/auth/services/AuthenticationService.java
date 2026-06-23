package staport.finance.gBanque.auth.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.auth.dtos.LoginResponse;
import staport.finance.gBanque.auth.dtos.ProfileResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final LoginAttemptService loginAttemptService;


    public AuthenticationService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder,
                                  LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.loginAttemptService = loginAttemptService;

    }

    /**
     * Protection brute-force : un compte verrouillé ({@link LoginAttemptService}) lève
     * avant même de solliciter l'{@code AuthenticationManager} ; chaque échec incrémente
     * le compteur, chaque succès le réinitialise.
     */
    public LoginResponse login (String username, String password){

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

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String accessToken = this.generateToken(Instant.now(), username, authorities);

        return new LoginResponse(accessToken, username, authorities);
    }

    public String generateToken(Instant dateSystem, String username, List<String> authorities){

        JwtClaimsSet  jwtClaimsSet= JwtClaimsSet.builder()
                .issuedAt(dateSystem)
                .expiresAt(dateSystem.plus(4, ChronoUnit.HOURS))
                .subject(username)
                .claim("authorities", authorities)
                .build();


        JwtEncoderParameters jwtEncoderParameters=JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),jwtClaimsSet);

        return  jwtEncoder.encode(jwtEncoderParameters).getTokenValue();

    }

    public ProfileResponse getAuthentication(Authentication authentication) {


        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated or authentication object is null.");
        }

        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());


        return new ProfileResponse(username, roles);

    }
}