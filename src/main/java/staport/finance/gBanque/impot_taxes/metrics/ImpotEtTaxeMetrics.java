package staport.finance.gBanque.impot_taxes.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.prefinancement.enums.Status;

/**
 * Métriques Micrometer pour le module Impôts et Taxes.
 * Convention de nommage : gbanque.impotetaxe.<objet>.<mesure>
 */
@Component
public class ImpotEtTaxeMetrics {

    private final MeterRegistry registry;
    private final ImpotEtTaxeRepository impotEtTaxeRepository;

    public ImpotEtTaxeMetrics(MeterRegistry registry, ImpotEtTaxeRepository impotEtTaxeRepository) {
        this.registry = registry;
        this.impotEtTaxeRepository = impotEtTaxeRepository;
    }

    @PostConstruct
    public void registerMetrics() {

        Gauge.builder("gbanque.impotetaxe.prevu.count",
                        impotEtTaxeRepository,
                        repo -> (double) repo.countByStatus(Status.PREVU))
                .description("Nombre d'impôts/taxes au statut PRÉVU")
                .register(registry);

        Gauge.builder("gbanque.impotetaxe.paye.count",
                        impotEtTaxeRepository,
                        repo -> (double) repo.countByStatus(Status.PAYE))
                .description("Nombre d'impôts/taxes au statut PAYÉ")
                .register(registry);
    }
}
