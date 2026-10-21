package staport.finance.gBanque.cautions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import staport.finance.gBanque.AbstractIntegrationTest;
import staport.finance.gBanque.cautions.commissions.service.IGenerationCommissionCautionService;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Batch de commissions de caution sur PostgreSQL réel : facturation trimestrielle de BP (trimestre civil, prorata d'entrée
 * daté du jour de prise) et prorata d'entrée d'une banque mensuelle (CDM) généré par le batch et non plus à la création.
 */
class GenerationCommissionCautionIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicLong UNICITE = new AtomicLong(800_000);

    @Autowired private IGenerationCommissionCautionService batch;
    @Autowired private JdbcTemplate jdbc;

    /** Les tests partagent une base : chaque test repart sans caution ni banque BP / CDM (la stratégie se résout par nom de banque). */
    @BeforeEach
    void nettoyer() {
        jdbc.update("DELETE FROM commission WHERE caution_id IS NOT NULL");
        jdbc.update("DELETE FROM cautions");
        jdbc.update("DELETE FROM caution_commission_config");
        jdbc.update("DELETE FROM banque WHERE nom IN ('BP', 'CDM')");
    }

    private long banque(String nom, String minimum) {
        long n = UNICITE.incrementAndGet();
        long id = jdbc.queryForObject("INSERT INTO banque (date_creation, date_modification, numero_compte, agence, nom, address, devise, "
                + "logo_data) VALUES (CURRENT_DATE, CURRENT_DATE, 'CA" + n + "', 'Agence', '" + nom + "', 'Adresse', 'MAD', "
                + "decode('00', 'hex')) RETURNING id", Long.class);
        jdbc.update("INSERT INTO caution_commission_config (banque_id, diviseur, montant_minimum) VALUES (" + id + ", 36000, " + minimum + ")");
        return id;
    }

    private long nature() {
        long n = UNICITE.incrementAndGet();
        return jdbc.queryForObject("INSERT INTO nature_ligne_fonctionnelle (date_creation, date_modification, code, portee, description, label) "
                + "VALUES (CURRENT_DATE, CURRENT_DATE, 'CAUT" + n + "', 'GENERALE', 'Caution " + n + "', 'Caution " + n + "') RETURNING id", Long.class);
    }

    private long caution(long banque, long nature, String datePrise) {
        return jdbc.queryForObject("INSERT INTO cautions (created_at, updated_at, date_de_prise, montant, taux, banque_id, nature_id, designation, "
                + "numero_caution, organisme, actif) VALUES (CURRENT_DATE, CURRENT_DATE, DATE '" + datePrise + "', 1000000, 1.2, " + banque + ", "
                + nature + ", 'Test', 'C" + UNICITE.incrementAndGet() + "', 'Organisme', true) RETURNING id", Long.class);
    }

    private List<Map<String, Object>> commissions(long caution) {
        return jdbc.queryForList("SELECT type_commission, date_emission, montant, libelle FROM commission WHERE caution_id = " + caution
                + " ORDER BY date_emission");
    }

    @Test
    @DisplayName("BP : commission trimestrielle au 31/12 pour une caution antérieure, prorata d'entrée daté du jour de prise pour une prise du trimestre")
    void bp_finDeTrimestre() {
        long bp = banque("BP", "0");
        long nature = nature();
        long ancienne = caution(bp, nature, "2026-08-10");
        long recente = caution(bp, nature, "2026-11-15");

        batch.generer(YearMonth.of(2026, 12), false);

        List<Map<String, Object>> a = commissions(ancienne);
        assertThat(a).hasSize(1);
        assertThat(a.get(0)).containsEntry("type_commission", "TRIMESTRIELLE");
        assertThat(a.get(0).get("date_emission")).hasToString("2026-12-31");
        assertThat(a.get(0).get("montant")).hasToString("3066.6700");   // 92 jours

        List<Map<String, Object>> r = commissions(recente);
        assertThat(r).hasSize(1);   // prorata seul : pas de commission trimestrielle en plus pour le même trimestre
        assertThat(r.get(0)).containsEntry("type_commission", "PRORATA_ENTREE");
        assertThat(r.get(0).get("date_emission")).hasToString("2026-11-15");
        assertThat(r.get(0).get("montant")).hasToString("1566.6700");   // 47 jours
    }

    @Test
    @DisplayName("BP : rien n'est facturé hors fin de trimestre, et rejouer le batch ne crée aucun doublon")
    void bp_horsTrimestre_etIdempotence() {
        long bp = banque("BP", "0");
        long caution = caution(bp, nature(), "2026-08-10");

        batch.generer(YearMonth.of(2026, 11), false);
        assertThat(commissions(caution)).isEmpty();

        batch.generer(YearMonth.of(2026, 12), false);
        batch.generer(YearMonth.of(2026, 12), false);
        assertThat(commissions(caution)).hasSize(1);
    }

    @Test
    @DisplayName("CDM (mensuelle) : le prorata d'entrée est généré par le batch du 1er, daté du jour de prise, avec la commission du mois")
    void cdm_prorataGenereParLeBatch() {
        long cdm = banque("CDM", "70");
        long caution = caution(cdm, nature(), "2026-11-15");
        assertThat(commissions(caution)).isEmpty();   // plus de prorata à la création

        batch.generer(YearMonth.of(2026, 12), true);

        List<Map<String, Object>> c = commissions(caution);
        assertThat(c).hasSize(2);
        assertThat(c.get(0)).containsEntry("type_commission", "PRORATA_ENTREE");
        assertThat(c.get(0).get("date_emission")).hasToString("2026-11-15");
        assertThat(c.get(1)).containsEntry("type_commission", "MENSUELLE");
        assertThat(c.get(1).get("date_emission")).hasToString("2026-12-01");
    }

    @Test
    @DisplayName("Un prorata déjà créé à la date de prise (ancien fonctionnement) est ignoré : aucun doublon")
    void prorataExistant_ignore() {
        long cdm = banque("CDM", "70");
        long caution = caution(cdm, nature(), "2026-11-15");
        jdbc.update("INSERT INTO commission (banque_id, caution_id, montant, operation, status, type_commission, libelle, observation, date_emission) "
                + "VALUES (" + cdm + ", " + caution + ", 100, 'CAUTION', 'PREVU', 'PRORATA_ENTREE', 'ancien prorata', 'CAUTION', DATE '2026-11-15')");

        batch.generer(YearMonth.of(2026, 12), true);

        assertThat(commissions(caution).stream().filter(m -> "PRORATA_ENTREE".equals(m.get("type_commission"))).count()).isEqualTo(1);
    }
}
