package staport.finance.gBanque;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base des tests d'intégration : un seul MySQL 8 jetable (Testcontainers) et un seul contexte Spring
 * pour toute la suite. Toutes les classes d'intégration en héritent sans rien ajouter à la configuration,
 * sinon Spring recrée un contexte (plusieurs minutes) — cf. conventions/testing.md.
 *
 * <p>Le conteneur est démarré une fois pour la JVM (pas de {@code @Container}, qui l'arrêterait après
 * chaque classe alors que le contexte mis en cache l'utilise encore) ; Testcontainers le supprime en fin de JVM.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    static {
        MYSQL.start();
    }
}
