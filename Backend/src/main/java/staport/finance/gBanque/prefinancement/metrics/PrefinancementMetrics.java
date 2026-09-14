package staport.finance.gBanque.prefinancement.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;

/**
 * Métriques Micrometer pour le module Préfinancement.
 * Convention de nommage : gbanque.prefinancement.<objet>.<mesure>
 */
@Component
public class PrefinancementMetrics {

    private final MeterRegistry registry;
    private final PrefinancementRepository prefinancementRepository;

    public PrefinancementMetrics(MeterRegistry registry, PrefinancementRepository prefinancementRepository) {
        this.registry = registry;
        this.prefinancementRepository = prefinancementRepository;
    }

    @PostConstruct
    public void registerMetrics() {

        Gauge.builder("gbanque.prefinancement.prevu.count",
                        prefinancementRepository,
                        repo -> (double) repo.countByStatus(Status.PREVU))
                .description("Nombre de préfinancements au statut PRÉVU")
                .register(registry);

        Gauge.builder("gbanque.prefinancement.paye.count",
                        prefinancementRepository,
                        repo -> (double) repo.countByStatus(Status.PAYE))
                .description("Nombre de préfinancements au statut PAYÉ")
                .register(registry);
    }
}
