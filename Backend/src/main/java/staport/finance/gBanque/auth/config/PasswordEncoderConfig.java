package staport.finance.gBanque.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration isolée du {@link PasswordEncoder}.
 *
 * <p>Séparé de {@link SecurityConfig} pour éviter une dépendance circulaire :
 * {@code SecurityConfig} → {@code UserDetailsServiceImpl} → {@code UserService}
 * → {@code PasswordEncoder}. En isolant ce bean ici, Spring peut le résoudre
 * indépendamment, sans que {@code SecurityConfig} soit entièrement créé.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
