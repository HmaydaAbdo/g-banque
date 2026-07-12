package staport.finance.gBanque.moyensDePaiements.lcn.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.time.LocalDate;

/**
 * Métriques Micrometer pour le module LCN.
 * Convention de nommage : {@code gbanque.lcn.<objet>.<mesure>}
 */
@Component
public class LcnMetrics {

    private final MeterRegistry registry;
    private final LcnRepository lcnRepository;
    private Counter creationsEncaissement;
    private Counter creationsDecaissement;
    private Counter passagesRegle;

    public LcnMetrics(MeterRegistry registry, LcnRepository lcnRepository) {
        this.registry = registry;
        this.lcnRepository = lcnRepository;
    }

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("gbanque.lcn.encours.count",
                        lcnRepository,
                        repo -> (double) repo.count(Specification
                                .<staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn>where(
                                        (root, query, cb) -> cb.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS))))
                .description("Nombre de LCN actuellement ENCOURS")
                .register(registry);

        Gauge.builder("gbanque.lcn.echus.count",
                        lcnRepository,
                        repo -> (double) repo.count(Specification
                                .<staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn>where((root, query, cb) -> cb.and(
                                        cb.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS),
                                        cb.lessThan(root.get("dateEcheance"), LocalDate.now())))))
                .description("Nombre de LCN passés à l'état échu (ENCOURS et date d'échéance dépassée), tous sens confondus")
                .register(registry);

        creationsEncaissement = Counter.builder("gbanque.lcn.creations.count")
                .tag("type", "ENCAISSEMENT")
                .description("Nombre de LCN créés, par type")
                .register(registry);

        creationsDecaissement = Counter.builder("gbanque.lcn.creations.count")
                .tag("type", "DECAISSEMENT")
                .description("Nombre de LCN créés, par type")
                .register(registry);

        passagesRegle = Counter.builder("gbanque.lcn.regle.count")
                .description("Nombre de LCN passés à l'état REGLE")
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
