package staport.finance.gBanque.droitConstate.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.prefinancement.enums.Status;

/**
 * Métriques Micrometer pour le module Droit Constaté.
 * Convention de nommage : gbanque.droit_constate.<objet>.<mesure>
 */
@Component
public class DroitConstateMetrics {

    private final MeterRegistry registry;
    private final DroitConstateRepository droitConstateRepository;

    public DroitConstateMetrics(MeterRegistry registry, DroitConstateRepository droitConstateRepository) {
        this.registry = registry;
        this.droitConstateRepository = droitConstateRepository;
    }

    @PostConstruct
    public void registerMetrics() {

        Gauge.builder("gbanque.droit_constate.prevu.count",
                        droitConstateRepository,
                        repo -> (double) repo.countByStatus(Status.PREVU))
                .description("Nombre de droits constatés au statut PRÉVU")
                .register(registry);

        Gauge.builder("gbanque.droit_constate.paye.count",
                        droitConstateRepository,
                        repo -> (double) repo.countByStatus(Status.PAYE))
                .description("Nombre de droits constatés au statut PAYÉ")
                .register(registry);
    }
}
