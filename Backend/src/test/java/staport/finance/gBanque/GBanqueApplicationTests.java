package staport.finance.gBanque;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Vérifie que le contexte Spring démarre sur un MySQL 8 jetable (Testcontainers).
 * La connexion est injectée par {@code @ServiceConnection} : aucune base locale requise, seul Docker l'est.
 */
@SpringBootTest
@Testcontainers
class GBanqueApplicationTests {

	@Container
	@ServiceConnection
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

	@Test
	void contextLoads() {
	}

}
