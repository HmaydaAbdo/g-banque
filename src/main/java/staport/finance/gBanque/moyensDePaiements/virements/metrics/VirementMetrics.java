package staport.finance.gBanque.moyensDePaiements.virements.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

/**
 * Métriques Micrometer pour le module Virement.
 * Convention de nommage : {@code gbanque.virement.<objet>.<mesure>}
 */
@Component
public class VirementMetrics {

    private final MeterRegistry registry;
    private final VirementRepository virementRepository;
    private Counter creationsEncaissement;
    private Counter creationsDecaissement;
    private Counter passagesRegle;

    public VirementMetrics(MeterRegistry registry, VirementRepository virementRepository) {
        this.registry = registry;
        this.virementRepository = virementRepository;
    }

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("gbanque.virement.encours.count",
                        virementRepository,
                        repo -> (double) repo.count(org.springframework.data.jpa.domain.Specification
                                .where((root, query, cb) -> cb.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS))))
                .description("Nombre de virements actuellement ENCOURS")
                .register(registry);

        creationsEncaissement = Counter.builder("gbanque.virement.creations.count")
                .tag("type", "ENCAISSEMENT")
                .description("Nombre de virements créés, par type")
                .register(registry);

        creationsDecaissement = Counter.builder("gbanque.virement.creations.count")
                .tag("type", "DECAISSEMENT")
                .description("Nombre de virements créés, par type")
                .register(registry);

        passagesRegle = Counter.builder("gbanque.virement.regle.count")
                .description("Nombre de virements passés à l'état REGLE")
                .register(registry);
    }

    public void incrementerCreation(Type type) {
        if (type == Type.DECAISSEMENT) {
            creationsDecaissement.increment();
        } else {
            creationsEncaissement.increment();
        }
    }

    public void incrementerPassageRegle() {
        passagesRegle.increment();
    }
}
