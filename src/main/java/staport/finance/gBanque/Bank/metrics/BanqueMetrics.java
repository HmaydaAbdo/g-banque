package staport.finance.gBanque.Bank.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;

/**
 * Métriques Micrometer du module Bank.
 *
 * Métriques exposées :
 *   gbanque.banque.total — nb total de banques configurées dans le système
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
        Gauge.builder("gbanque.banque.total", banqueRepository, repo -> (double) repo.count())
                .description("Nombre total de banques configurées dans le système")
                .register(registry);
    }
}
