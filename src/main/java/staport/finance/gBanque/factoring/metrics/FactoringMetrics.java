package staport.finance.gBanque.factoring.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.prefinancement.enums.Status;

/**
 * Métriques Micrometer pour le module Factoring.
 * Convention de nommage : gbanque.factoring.<objet>.<mesure>
 */
@Component
public class FactoringMetrics {

    private final MeterRegistry registry;
    private final FactoringRepository factoringRepository;

    public FactoringMetrics(MeterRegistry registry, FactoringRepository factoringRepository) {
        this.registry = registry;
        this.factoringRepository = factoringRepository;
    }

    @PostConstruct
    public void registerMetrics() {

        Gauge.builder("gbanque.factoring.prevu.count",
                        factoringRepository,
                        repo -> (double) repo.countByStatus(Status.PREVU))
                .description("Nombre de factorings au statut PRÉVU")
                .register(registry);

        Gauge.builder("gbanque.factoring.paye.count",
                        factoringRepository,
                        repo -> (double) repo.countByStatus(Status.PAYE))
                .description("Nombre de factorings au statut PAYÉ")
                .register(registry);
    }
}
