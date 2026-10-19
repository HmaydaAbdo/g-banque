package staport.finance.gBanque;

import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Socle des tests de migrations de données : une base jetable créée sur le PostgreSQL de test, amenée à une version
 * donnée, chargée de lignes « héritées », puis migrée jusqu'au bout. Les contrôles de clés étrangères se suspendent avec
 * {@code SET session_replication_role = replica} (le compte de test est superutilisateur) pour insérer des lignes minimales.
 */
public abstract class AbstractMigrationTest extends AbstractIntegrationTest {

    /** Crée une base vide sur le PostgreSQL de test et renvoie son URL JDBC. */
    protected static String baseJetable() throws SQLException {
        String nom = "mig_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE " + nom);
        }
        return POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + nom);
    }

    protected static Connection connexion(String url) throws SQLException {
        return DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    /**
     * Applique les migrations jusqu'à la version donnée (toutes si {@code null}).
     *
     * <p>Jusqu'à une version intermédiaire, seules les migrations versionnées sont appliquées : les migrations
     * répétables ({@code R__}) décrivent le schéma final (vue des encours) et échoueraient sur un schéma ancien.
     * Elles s'appliquent à la migration finale ({@code versionCible == null}), qui reprend les mêmes scripts.
     */
    protected static void migrer(String url, String versionCible) {
        var config = Flyway.configure().dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());
        if (versionCible == null) {
            config.locations("classpath:db/migration");
        } else {
            config.locations("filesystem:" + migrationsVersionnees()).target(versionCible);
        }
        config.load().migrate();
    }

    /** Copie les seules migrations versionnées ({@code V*}) dans un dossier temporaire. */
    private static Path migrationsVersionnees() {
        try {
            Path source = Path.of(AbstractMigrationTest.class.getClassLoader().getResource("db/migration").toURI());
            Path copie = Files.createTempDirectory("migrations-versionnees");
            try (Stream<Path> scripts = Files.list(source)) {
                for (Path script : scripts.filter(f -> f.getFileName().toString().startsWith("V")).toList()) {
                    Files.copy(script, copie.resolve(script.getFileName()));
                }
            }
            return copie;
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static List<LocalDate> dates(Statement st, String sql) throws SQLException {
        List<LocalDate> valeurs = new ArrayList<>();
        try (ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                valeurs.add(rs.getObject(1, LocalDate.class));
            }
        }
        return valeurs;
    }

    protected static String texte(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
