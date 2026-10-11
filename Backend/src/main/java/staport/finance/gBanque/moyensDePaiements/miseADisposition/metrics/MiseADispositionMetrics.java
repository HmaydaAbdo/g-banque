package staport.finance.gBanque.moyensDePaiements.miseADisposition.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Métriques Micrometer pour le module Mise à Disposition.
 * Convention de nommage : {@code gbanque.miseADisposition.<objet>.<mesure>}
 */
@Component
public class MiseADispositionMetrics {

    private final MeterRegistry registry;
    private final MiseADispositionRepository miseADispositionRepository;
    private Counter passagesRegle;

    public MiseADispositionMetrics(MeterRegistry registry, MiseADispositionRepository miseADispositionRepository) {
        this.registry = registry;
        this.miseADispositionRepository = miseADispositionRepository;
    }

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("gbanque.miseADisposition.encours.count",
                        miseADispositionRepository,
                        repo -> (double) repo.count(org.springframework.data.jpa.domain.Specification
                                .where((root, query, cb) -> cb.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS))))
                .description("Nombre de mises à disposition actuellement ENCOURS")
                .register(registry);

        passagesRegle = Counter.builder("gbanque.miseADisposition.regle.count")
                .description("Nombre de mises à disposition passées à l'état REGLE")
                .register(registry);
    }

    public void incrementerPassageRegle() {
        passagesRegle.increment();
    }
}
