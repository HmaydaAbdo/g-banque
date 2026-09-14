package staport.finance.gBanque.moyensDePaiements.virementInternational.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.virementInternational.repositories.VirementInternationalRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

/**
 * Métriques Micrometer pour le module Virement International.
 * Convention de nommage : {@code gbanque.virementInternational.<objet>.<mesure>}
 */
@Component
public class VirementInternationalMetrics {

    private final MeterRegistry registry;
    private final VirementInternationalRepository virementInternationalRepository;
    private Counter creationsEncaissement;
    private Counter creationsDecaissement;
    private Counter passagesRegle;

    public VirementInternationalMetrics(MeterRegistry registry, VirementInternationalRepository virementInternationalRepository) {
        this.registry = registry;
        this.virementInternationalRepository = virementInternationalRepository;
    }

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("gbanque.virementInternational.encours.count",
                        virementInternationalRepository,
                        repo -> (double) repo.count(org.springframework.data.jpa.domain.Specification
                                .where((root, query, cb) -> cb.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS))))
                .description("Nombre de virements internationaux actuellement ENCOURS")
                .register(registry);

        creationsEncaissement = Counter.builder("gbanque.virementInternational.creations.count")
                .tag("type", "ENCAISSEMENT")
                .description("Nombre de virements internationaux créés, par type")
                .register(registry);

        creationsDecaissement = Counter.builder("gbanque.virementInternational.creations.count")
                .tag("type", "DECAISSEMENT")
                .description("Nombre de virements internationaux créés, par type")
                .register(registry);

        passagesRegle = Counter.builder("gbanque.virementInternational.regle.count")
                .description("Nombre de virements internationaux passés à l'état REGLE")
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
