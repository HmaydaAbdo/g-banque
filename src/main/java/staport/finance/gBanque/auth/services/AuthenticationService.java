package staport.finance.gBanque.auth.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;


    public AuthenticationService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder ) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;

    }

    public LoginResponse login (String username, String password){

        Authentication authentication= this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username,password));

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