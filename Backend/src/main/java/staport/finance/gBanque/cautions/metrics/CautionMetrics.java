package staport.finance.gBanque.cautions.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.cautions.repository.CautionRepository;

/**
 * Métriques Micrometer pour le module Caution.
 * Convention de nommage : gbanque.caution.<objet>.<mesure>
 */
@Component
public class CautionMetrics {

    private final MeterRegistry registry;
    private final CautionRepository cautionRepository;

    public CautionMetrics(MeterRegistry registry, CautionRepository cautionRepository) {
        this.registry = registry;
        this.cautionRepository = cautionRepository;
    }

    @PostConstruct
    public void registerMetrics() {

        // Cautions actives (actif = true)
        Gauge.builder("gbanque.caution.active.count",
                        cautionRepository,
                        repo -> (double) repo.countByActif(true))
                .description("Nombre de cautions actives")
                .register(registry);

        // Cautions expirées (actif = false)
        Gauge.builder("gbanque.caution.expired.count",
                        cautionRepository,
                        repo -> (double) repo.countByActif(false))
                .description("Nombre de cautions expirées (clôturées)")
                .register(registry);
    }
}
