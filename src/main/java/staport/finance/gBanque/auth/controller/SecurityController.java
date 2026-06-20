package staport.finance.gBanque.auth.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.auth.dtos.LoginRequest;
import staport.finance.gBanque.auth.dtos.LoginResponse;
import staport.finance.gBanque.auth.dtos.ProfileResponse;
import staport.finance.gBanque.auth.services.AuthenticationService;



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
      * DIRECTOR, SYSTEM_ADMIN) — pas de {@code @PreAuthorize} ici, le filtre
      * global {@code anyRequest().authenticated()} suffit.
      */
     @GetMapping("/profile")
     public ProfileResponse authentication(Authentication authentication){
        return this.authenticationService.getAuthentication(authentication);
     }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest){
        return this.authenticationService.login(loginRequest.getUsername(), loginRequest.getPassword());
    }

}
