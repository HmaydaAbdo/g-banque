package staport.finance.gBanque.Agios.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.repository.AgiosRepository;
import staport.finance.gBanque.prefinancement.enums.Status;

/**
 * Métriques Micrometer du module Agios.
 *
 * <ul>
 *   <li>{@code gbanque.agios.prevu.count} — nombre d'agios en attente de paiement (PREVU)</li>
 *   <li>{@code gbanque.agios.paye.count}  — nombre d'agios réglés (PAYE)</li>
 * </ul>
 */
@Component
public class AgiosMetrics {

    private final MeterRegistry registry;
    private final AgiosRepository agiosRepository;

    public AgiosMetrics(MeterRegistry registry, AgiosRepository agiosRepository) {
        this.registry = registry;
        this.agiosRepository = agiosRepository;
    }

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("gbanque.agios.prevu.count", agiosRepository,
                        r -> r.countByStatus(Status.PREVU))
                .description("Nombre d'agios en attente de paiement")
                .register(registry);

        Gauge.builder("gbanque.agios.paye.count", agiosRepository,
                        r -> r.countByStatus(Status.PAYE))
                .description("Nombre d'agios réglés")
                .register(registry);
    }
}
