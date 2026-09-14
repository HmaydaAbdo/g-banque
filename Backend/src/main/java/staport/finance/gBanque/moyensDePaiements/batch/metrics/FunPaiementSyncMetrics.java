package staport.finance.gBanque.moyensDePaiements.batch.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Métriques Micrometer pour le batch de synchronisation FunPaiement.
 * Convention de nommage : {@code gbanque.batch.funPaiement.<mesure>} (conventions/metrics.md,
 * section "Batch FunPaiement" : synchronisations réussies, synchronisations échouées, durée
 * d'exécution).
 */
@Component
public class FunPaiementSyncMetrics {

    private final MeterRegistry registry;
    private Counter synchronisationsReussies;
    private Counter synchronisationsEchouees;
    private Timer dureeExecution;

    public FunPaiementSyncMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void registerMetrics() {
        synchronisationsReussies = Counter.builder("gbanque.batch.funPaiement.synchronisations.reussies")
                .description("Nombre de synchronisations FunPaiement terminées avec succès")
                .register(registry);

        synchronisationsEchouees = Counter.builder("gbanque.batch.funPaiement.synchronisations.echouees")
                .description("Nombre de synchronisations FunPaiement en échec")
                .register(registry);

        dureeExecution = Timer.builder("gbanque.batch.funPaiement.duree")
                .description("Durée d'exécution du job de synchronisation FunPaiement")
                .register(registry);
    }

    public void enregistrerSucces(Duration duree) {
        synchronisationsReussies.increment();
        dureeExecution.record(duree.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void enregistrerEchec(Duration duree) {
        synchronisationsEchouees.increment();
        if (duree != null) {
            dureeExecution.record(duree.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
