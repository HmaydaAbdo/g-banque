package staport.finance.gBanque.initializers;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import staport.finance.gBanque.security.entities.Role;
import staport.finance.gBanque.security.entities.User;
import staport.finance.gBanque.security.enums.RoleName;
import staport.finance.gBanque.security.repositories.RoleRepository;
import staport.finance.gBanque.security.repositories.UserRepository;

import java.util.List;

@Configuration
public class RolesAndUsersInitializer {

        @Bean
        CommandLineRunner userAndRolesInitializer(UserRepository userRepo, RoleRepository roleRepo, PasswordEncoder encoder) {
            return args -> {
                // Initialize Roles
                Role userRole = roleRepo.findByRoleName(RoleName.USER).orElseGet(() -> roleRepo.save(new Role(RoleName.USER)));
                Role adminRole = roleRepo.findByRoleName(RoleName.ADMIN).orElseGet(() -> roleRepo.save(new Role(RoleName.ADMIN)));
                Role superAdminRole = roleRepo.findByRoleName(RoleName.SUPER_ADMIN).orElseGet(() -> roleRepo.save(new Role(RoleName.SUPER_ADMIN)));

                // Initialize Users if they don't exist
                if (userRepo.findByUsername("oumaima").isEmpty()) {
                    userRepo.save(new User("oumaima", encoder.encode("12345"), true, List.of(userRole)));
                }
                if (userRepo.findByUsername("amal").isEmpty()) {
                    userRepo.save(new User("amal", encoder.encode("12345"), true, List.of(userRole)));
                }
                if (userRepo.findByUsername("mehdi").isEmpty()) {
                    userRepo.save(new User("mehdi", encoder.encode("12345"), true, List.of(userRole, adminRole)));
                }
                if (userRepo.findByUsername("hmayda").isEmpty()) {
                    userRepo.save(new User("hmayda", encoder.encode("12345"), true, List.of(userRole, adminRole, superAdminRole)));
                }
            };
        }
}
