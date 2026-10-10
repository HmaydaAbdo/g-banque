package staport.finance.gBanque.auth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import staport.finance.gBanque.auth.services.UserDetailsServiceImpl;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;
    private final PasswordEncoder passwordEncoder;
    private final String secretKey;
    private final List<String> allowedOrigins;
    private final boolean secureCookies;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
                          JsonAccessDeniedHandler jsonAccessDeniedHandler,
                          PasswordEncoder passwordEncoder,
                          @Value("${jwt.secret}") String secretKey,
                          @Value("${cors.allowed-origins}") String allowedOrigins,
                          @Value("${auth.cookie.secure}") boolean secureCookies) {
        this.userDetailsService = userDetailsService;
        this.jsonAuthenticationEntryPoint = jsonAuthenticationEntryPoint;
        this.jsonAccessDeniedHandler = jsonAccessDeniedHandler;
        this.passwordEncoder = passwordEncoder;
        this.secretKey = secretKey;
        this.allowedOrigins = Arrays.asList(allowedOrigins.split(","));
        this.secureCookies = secureCookies;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                // Session par cookie => protection CSRF (en plus de SameSite=Strict) : jeton double-submit,
                // cookie XSRF-TOKEN lisible par Angular, renvoyé dans l'en-tête X-XSRF-TOKEN.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(csrfTokenRequestHandler())
                        // Connexion : pas encore de session à protéger. Appels avec Authorization: Bearer
                        // (Swagger, outils) : pas d'authentification par cookie, donc pas de CSRF possible.
                        .ignoringRequestMatchers("/auth/login")
                        .ignoringRequestMatchers(request -> request.getHeader("Authorization") != null)
                )
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints de session : leur propre contrôle repose sur le cookie de rafraîchissement
                        .requestMatchers("/auth/login", "/auth/refresh", "/auth/logout").permitAll()

                        // Actuator — health et prometheus publics (prometheus requis par le scraper)
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/prometheus"
                        ).permitAll()

                        .requestMatchers("/actuator/**").hasAuthority("SYSTEM_ADMIN")

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(new CookieBearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                )
                // MDC filter posé après BearerTokenAuthenticationFilter : SecurityContextHolder
                // est déjà peuplé, donc userId peut être lu depuis le principal JWT.
                .addFilterAfter(new MdcRequestFilter(), BearerTokenAuthenticationFilter.class)
                .build();
    }

    /** Cookie XSRF-TOKEN sur "/" : Angular ne lit que les cookies visibles depuis le chemin de la page. */
    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.path("/").secure(secureCookies).sameSite("Strict"));
        return repository;
    }

    /**
     * Jeton CSRF en clair (l'en-tête renvoyé par Angular est la valeur brute du cookie) et généré à
     * chaque requête (attribut null = pas de chargement différé), pour que le cookie existe dès la
     * première réponse.
     */
    private CsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName(null);
        return handler;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey.getBytes()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HMACSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKeySpec).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        return new ProviderManager(daoAuthenticationProvider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(allowedOrigins);
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
