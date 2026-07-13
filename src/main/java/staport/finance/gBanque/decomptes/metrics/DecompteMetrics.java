package staport.finance.gBanque.decomptes.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.prefinancement.enums.Status;

/**
 * Métriques Micrometer pour le module Décompte.
 * Convention de nommage : gbanque.decompte.<objet>.<mesure>
 */
@Component
public class DecompteMetrics {

    private final MeterRegistry registry;
    private final DecompteRepository decompteRepository;

    public DecompteMetrics(MeterRegistry registry, DecompteRepository decompteRepository) {
        this.registry = registry;
        this.decompteRepository = decompteRepository;
    }

    @PostConstruct
    public void registerMetrics() {

        Gauge.builder("gbanque.decompte.prevu.count",
                        decompteRepository,
                        repo -> (double) repo.countByStatus(Status.PREVU))
                .description("Nombre de décomptes au statut PRÉVU")
                .register(registry);

        Gauge.builder("gbanque.decompte.paye.count",
                        decompteRepository,
                        repo -> (double) repo.countByStatus(Status.PAYE))
                .description("Nombre de décomptes au statut PAYÉ")
                .register(registry);
    }
}
