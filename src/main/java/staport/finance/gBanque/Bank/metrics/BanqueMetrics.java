package staport.finance.gBanque.Bank.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;

/**
 * Métriques Micrometer pour le module Bank.
 * Convention de nommage : {@code gbanque.bank.<objet>.<mesure>}
 */
@Component
public class BanqueMetrics {

    private final MeterRegistry registry;
    private final BanqueRepository banqueRepository;

    public BanqueMetrics(MeterRegistry registry, BanqueRepository banqueRepository) {
        this.registry = registry;
        this.banqueRepository = banqueRepository;
    }

    @PostConstruct
    public void registerMetrics() {

        Gauge.builder("gbanque.bank.total",
                        banqueRepository,
                        repo -> (double) repo.count())
                .description("Nombre total de comptes bancaires configurés")
                .register(registry);
    }
}
