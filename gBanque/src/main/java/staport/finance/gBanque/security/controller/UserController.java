package staport.finance.gBanque.security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import staport.finance.gBanque.security.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/usernames")
    public ResponseEntity<List<String>> getDistinctUsernames() {
        return ResponseEntity.ok(userService.getAllDistinctUsernames());
    }

}
