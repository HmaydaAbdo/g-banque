package staport.finance.gBanque.leasing.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.repositories.OrganismeLeasingRepository;

import java.time.LocalDate;

/**
 * Métriques Micrometer pour le module Leasing.
 * Convention de nommage : gbanque.leasing.<objet>.<mesure>
 */
@Component
public class LeasingMetrics {

    private final MeterRegistry registry;
    private final ContratLeasingRepository contratLeasingRepository;
    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final OrganismeLeasingRepository organismeLeasingRepository;

    public LeasingMetrics(MeterRegistry registry,
                          ContratLeasingRepository contratLeasingRepository,
                          EcheanceLeasingRepository echeanceLeasingRepository,
                          OrganismeLeasingRepository organismeLeasingRepository) {
        this.registry = registry;
        this.contratLeasingRepository = contratLeasingRepository;
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.organismeLeasingRepository = organismeLeasingRepository;
    }

    @PostConstruct
    public void registerMetrics() {

        // Contrats actifs (completed = false)
        Gauge.builder("gbanque.leasing.contrat.actifs",
                        contratLeasingRepository,
                        repo -> (double) repo.countByCompleted(false))
                .description("Nombre de contrats de leasing actifs (non clôturés)")
                .register(registry);

        // Contrats en attente de main levée
        Gauge.builder("gbanque.leasing.contrat.attente_main_levee",
                        contratLeasingRepository,
                        repo -> (double) repo.countByCompleted(true))
                .description("Nombre de contrats de leasing clôturés (main levée effectuée)")
                .register(registry);

        // Organismes de leasing
        Gauge.builder("gbanque.leasing.organisme.total",
                        organismeLeasingRepository,
                        repo -> (double) repo.count())
                .description("Nombre total d'organismes de leasing configurés")
                .register(registry);

        // Échéances PREVU
        Gauge.builder("gbanque.leasing.echeance.prevu",
                        echeanceLeasingRepository,
                        repo -> (double) repo.findByStatutPaiement(StatutEcheanceLeasing.PREVU).size())
                .description("Nombre d'échéances de leasing en attente de paiement (PREVU)")
                .register(registry);

        // Échéances PAYE
        Gauge.builder("gbanque.leasing.echeance.paye",
                        echeanceLeasingRepository,
                        repo -> (double) repo.findByStatutPaiement(StatutEcheanceLeasing.PAYE).size())
                .description("Nombre d'échéances de leasing déjà payées (PAYE)")
                .register(registry);

        // Échéances à venir dans les 7 prochains jours (PREVU)
        Gauge.builder("gbanque.leasing.echeance.a_venir_j7",
                        echeanceLeasingRepository,
                        repo -> (double) repo.countByStatutPaiementAndDateEcheanceBetween(
                                StatutEcheanceLeasing.PREVU,
                                LocalDate.now(),
                                LocalDate.now().plusDays(7)))
                .description("Nombre d'échéances PREVU dont la date tombe dans les 7 prochains jours")
                .register(registry);

        // Échéances à venir dans les 30 prochains jours (PREVU)
        Gauge.builder("gbanque.leasing.echeance.a_venir_j30",
                        echeanceLeasingRepository,
                        repo -> (double) repo.countByStatutPaiementAndDateEcheanceBetween(
                                StatutEcheanceLeasing.PREVU,
                                LocalDate.now(),
                                LocalDate.now().plusDays(30)))
                .description("Nombre d'échéances PREVU dont la date tombe dans les 30 prochains jours")
                .register(registry);
    }
}
