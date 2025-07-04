package staport.finance.gBanque.security.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.security.dtos.LoginRequest;
import staport.finance.gBanque.security.dtos.LoginResponse;
import staport.finance.gBanque.security.dtos.ProfileResponse;
import staport.finance.gBanque.security.services.AuthenticationService;



@RestController
@RequestMapping("/auth")
public class SecurityController {


    private final AuthenticationService authenticationService;


    public SecurityController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

     @GetMapping("/profile")
     @PreAuthorize("hasAuthority('ADMIN')")
     public ProfileResponse authentication(Authentication authentication){
        return this.authenticationService.getAuthentication(authentication);
     }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest){
        return this.authenticationService.login(loginRequest.getUsername(), loginRequest.getPassword());
    }

}
