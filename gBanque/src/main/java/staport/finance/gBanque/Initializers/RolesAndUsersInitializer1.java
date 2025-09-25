package staport.finance.gBanque.Initializers;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import staport.finance.gBanque.security.entities.Role;
import staport.finance.gBanque.security.entities.User;
import staport.finance.gBanque.security.enums.RoleName;
import staport.finance.gBanque.security.repositories.RoleRepository;
import staport.finance.gBanque.security.repositories.UserRepository;

import java.util.List;

@Configuration
public class RolesAndUsersInitializer1 {

    @Bean
    @Order(1)
    CommandLineRunner userAndRolesInitializer(
            UserRepository userRepo,
            RoleRepository roleRepo,
            PasswordEncoder encoder) {
        return args -> {

            // 👉 Skip if users already exist
            if (userRepo.count() > 0) {
                System.out.println("Users already exist. Skipping default initialization.");
                return;
            }

            // Initialize Roles
            Role userRole = roleRepo.findByRoleName(RoleName.USER)
                    .orElseGet(() -> roleRepo.save(new Role(RoleName.USER)));
            Role adminRole = roleRepo.findByRoleName(RoleName.ADMIN)
                    .orElseGet(() -> roleRepo.save(new Role(RoleName.ADMIN)));
            Role superAdminRole = roleRepo.findByRoleName(RoleName.SUPER_ADMIN)
                    .orElseGet(() -> roleRepo.save(new Role(RoleName.SUPER_ADMIN)));

            // Initialize Users (only first time)
            userRepo.save(new User("oumaima", encoder.encode("12345"), true, List.of(userRole)));
            userRepo.save(new User("amal", encoder.encode("12345"), true, List.of(userRole)));
            userRepo.save(new User("mehdi", encoder.encode("12345"), true, List.of(userRole, adminRole)));
            userRepo.save(new User("hmayda", encoder.encode("12345"), true, List.of(userRole, adminRole, superAdminRole)));

            System.out.println("Default roles and users created successfully.");
        };
    }

}
