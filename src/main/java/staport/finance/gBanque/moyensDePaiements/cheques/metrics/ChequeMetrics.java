package staport.finance.gBanque.moyensDePaiements.cheques.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

/**
 * Métriques Micrometer pour le module Chèque.
 * Convention de nommage : {@code gbanque.cheque.<objet>.<mesure>}
 */
@Component
public class ChequeMetrics {

    private final MeterRegistry registry;
    private final ChequeRepository chequeRepository;
    private Counter creationsEncaissement;
    private Counter creationsDecaissement;
    private Counter passagesRegle;

    public ChequeMetrics(MeterRegistry registry, ChequeRepository chequeRepository) {
        this.registry = registry;
        this.chequeRepository = chequeRepository;
    }

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("gbanque.cheque.encours.count",
                        chequeRepository,
                        repo -> (double) repo.count(org.springframework.data.jpa.domain.Specification
                                .where((root, query, cb) -> cb.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS))))
                .description("Nombre de chèques actuellement ENCOURS")
                .register(registry);

        creationsEncaissement = Counter.builder("gbanque.cheque.creations.count")
                .tag("type", "ENCAISSEMENT")
                .description("Nombre de chèques créés, par type")
                .register(registry);

        creationsDecaissement = Counter.builder("gbanque.cheque.creations.count")
                .tag("type", "DECAISSEMENT")
                .description("Nombre de chèques créés, par type")
                .register(registry);

        passagesRegle = Counter.builder("gbanque.cheque.regle.count")
                .description("Nombre de chèques passés à l'état REGLE")
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
