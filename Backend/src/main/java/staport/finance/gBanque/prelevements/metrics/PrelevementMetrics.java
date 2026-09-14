package staport.finance.gBanque.prelevements.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;

/**
 * Métriques Micrometer pour le module Prélèvements.
 * Convention de nommage : {@code gbanque.prelevement.<objet>.<mesure>}
 */
@Component
public class PrelevementMetrics {

    private final MeterRegistry registry;
    private final PrelevementRepository prelevementRepository;
    private Counter creations;
    private Counter passagesPaye;

    public PrelevementMetrics(MeterRegistry registry, PrelevementRepository prelevementRepository) {
        this.registry = registry;
        this.prelevementRepository = prelevementRepository;
    }

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("gbanque.prelevement.encours.count",
                        prelevementRepository,
                        repo -> (double) repo.count(org.springframework.data.jpa.domain.Specification
                                .where((root, query, cb) -> cb.equal(root.get("status"), Status.PREVU))))
                .description("Nombre de prélèvements actuellement PREVU")
                .register(registry);

        creations = Counter.builder("gbanque.prelevement.creations.count")
                .description("Nombre de prélèvements créés")
                .register(registry);

        passagesPaye = Counter.builder("gbanque.prelevement.paye.count")
                .description("Nombre de prélèvements passés à l'état PAYE")
                .register(registry);
    }

    public void incrementerCreation() {
        creations.increment();
    }

    public void incrementerPassagePaye() {
        passagesPaye.increment();
    }
}
