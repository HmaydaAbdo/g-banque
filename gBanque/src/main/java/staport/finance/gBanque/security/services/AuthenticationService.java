package staport.finance.gBanque.security.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.security.dtos.LoginResponse;
import staport.finance.gBanque.security.dtos.ProfileResponse;
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

        String roles=authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" ")) ;

        String accessToken = this.generateToken(Instant.now(), username, roles);

        return new LoginResponse(accessToken);
    }

    public String generateToken(Instant dateSystem,String username,String roles){

        JwtClaimsSet  jwtClaimsSet= JwtClaimsSet.builder()
                .issuedAt(dateSystem)
                .expiresAt(dateSystem.plus(4, ChronoUnit.HOURS))
                .subject(username)
                .claim("roles",roles)
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