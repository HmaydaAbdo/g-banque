package staport.finance.gBanque.rapprochement.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;

/**
 * Métriques Micrometer pour le module Rapprochement.
 * Convention de nommage : gbanque.rapprochement.&lt;objet&gt;.&lt;mesure&gt;
 *
 * <p>"Rapprochements par statut" (metrics.md) : un Gauge par valeur de {@link StatutRapprochement}.
 * "Rapprochements par banque" : plutôt qu'un tag dynamique par banque (cardinalité variable, aucun
 * précédent {@code MultiGauge} dans ce repo), interprété comme le nombre de banques ayant
 * actuellement un rapprochement EN_COURS — sert de garde-fou de cohérence : la règle métier garantit
 * au plus un EN_COURS par banque à tout instant, donc cette valeur doit toujours être égale au
 * Gauge {@code gbanque.rapprochement.encours.count} ; une divergence signale une anomalie de données.
 */
@Component
public class RapprochementMetrics {

    private final MeterRegistry registry;
    private final RapprochementRepository rapprochementRepository;

    public RapprochementMetrics(MeterRegistry registry, RapprochementRepository rapprochementRepository) {
        this.registry = registry;
        this.rapprochementRepository = rapprochementRepository;
    }

    @PostConstruct
    public void registerMetrics() {

        // ── Par statut ───────────────────────────────────────────────────────
        Gauge.builder("gbanque.rapprochement.encours.count",
                        rapprochementRepository,
                        repo -> (double) repo.countByStatut(StatutRapprochement.EN_COURS))
                .description("Nombre de rapprochements EN_COURS")
                .register(registry);

        Gauge.builder("gbanque.rapprochement.provisoire.count",
                        rapprochementRepository,
                        repo -> (double) repo.countByStatut(StatutRapprochement.CLOTURE_PROVISOIRE))
                .description("Nombre de rapprochements en clôture provisoire")
                .register(registry);

        Gauge.builder("gbanque.rapprochement.definitif.count",
                        rapprochementRepository,
                        repo -> (double) repo.countByStatut(StatutRapprochement.CLOTURE_DEFINITIVE))
                .description("Nombre de rapprochements clôturés définitivement")
                .register(registry);

        // ── Par banque (garde-fou de cohérence — voir Javadoc de la classe) ────
        Gauge.builder("gbanque.rapprochement.banques_avec_encours.count",
                        rapprochementRepository,
                        repo -> (double) repo.countDistinctBanquesByStatut(StatutRapprochement.EN_COURS))
                .description("Nombre de banques ayant actuellement un rapprochement EN_COURS")
                .register(registry);
    }
}
