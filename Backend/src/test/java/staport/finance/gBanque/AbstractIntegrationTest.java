package staport.finance.gBanque;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base des tests d'intégration : un seul PostgreSQL 18 jetable (Testcontainers) et un seul contexte Spring
 * pour toute la suite. Toutes les classes d'intégration en héritent sans rien ajouter à la configuration,
 * sinon Spring recrée un contexte (plusieurs minutes) — cf. conventions/testing.md.
 *
 * <p>Schéma construit par Flyway puis validé par Hibernate (comme en production) ; le profil {@code test}
 * ne fait que couper les jobs planifiés.
 *
 * <p>Le conteneur est démarré une fois pour la JVM (pas de {@code @Container}, qui l'arrêterait après
 * chaque classe alors que le contexte mis en cache l'utilise encore) ; Testcontainers le supprime en fin de JVM.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /** Même image et même initialisation (locale ICU fr-FR) que le service postgres de docker-compose.yml. */
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6-alpine")
            .withEnv("POSTGRES_INITDB_ARGS", "--locale-provider=icu --icu-locale=fr-FR --encoding=UTF8");

    static {
        POSTGRES.start();
    }
}
