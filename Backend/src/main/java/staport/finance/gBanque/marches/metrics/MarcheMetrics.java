package staport.finance.gBanque.marches.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.marches.repositories.MarcheRepository;

/**
 * Métriques Micrometer pour le module Marches.
 * Convention de nommage : gbanque.marche.<objet>.<mesure>
 */
@Component
public class MarcheMetrics {

    private final MeterRegistry registry;
    private final MarcheRepository marcheRepository;

    public MarcheMetrics(MeterRegistry registry, MarcheRepository marcheRepository) {
        this.registry = registry;
        this.marcheRepository = marcheRepository;
    }

    @PostConstruct
    public void registerMetrics() {

        // Marchés actifs (actif = true)
        Gauge.builder("gbanque.marche.actif.count",
                        marcheRepository,
                        repo -> (double) repo.countByActif(true))
                .description("Nombre de marchés actifs")
                .register(registry);

        // Marchés archivés (actif = false)
        Gauge.builder("gbanque.marche.archive.count",
                        marcheRepository,
                        repo -> (double) repo.countByActif(false))
                .description("Nombre de marchés archivés")
                .register(registry);
    }
}
