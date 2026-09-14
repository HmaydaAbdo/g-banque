package staport.finance.gBanque.cmt.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.cmt.config.services.interfaces.ICmtConfigService;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.repositories.CreditMoyenTermeRepository;

import java.time.LocalDate;

/**
 * Métriques Micrometer pour le module CMT (Crédit Moyen Terme).
 * Convention de nommage : gbanque.cmt.<objet>.<mesure>
 */
@Component
public class CmtMetrics {

    private final MeterRegistry registry;
    private final CreditMoyenTermeRepository creditMoyenTermeRepository;
    private final EcheanceCMTRepository echeanceCMTRepository;
    private final ICmtConfigService cmtConfigService;

    public CmtMetrics(MeterRegistry registry,
                       CreditMoyenTermeRepository creditMoyenTermeRepository,
                       EcheanceCMTRepository echeanceCMTRepository,
                       ICmtConfigService cmtConfigService) {
        this.registry = registry;
        this.creditMoyenTermeRepository = creditMoyenTermeRepository;
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.cmtConfigService = cmtConfigService;
    }

    @PostConstruct
    public void registerMetrics() {

        // Crédits actifs (completed = false)
        Gauge.builder("gbanque.cmt.credit.actifs",
                        creditMoyenTermeRepository,
                        repo -> (double) repo.countByCompleted(false))
                .description("Nombre de crédits à moyen terme actifs (non clôturés)")
                .register(registry);

        // Crédits archivés (main levée effectuée)
        Gauge.builder("gbanque.cmt.credit.archives",
                        creditMoyenTermeRepository,
                        repo -> (double) repo.countByCompleted(true))
                .description("Nombre de crédits à moyen terme archivés (main levée effectuée)")
                .register(registry);

        // Échéances PREVU
        Gauge.builder("gbanque.cmt.echeance.prevu",
                        echeanceCMTRepository,
                        repo -> (double) repo.countByStatutPaiement(StatutEcheanceCMT.PREVU))
                .description("Nombre d'échéances CMT en attente de paiement (PREVU)")
                .register(registry);

        // Échéances PAYE
        Gauge.builder("gbanque.cmt.echeance.paye",
                        echeanceCMTRepository,
                        repo -> (double) repo.countByStatutPaiement(StatutEcheanceCMT.PAYE))
                .description("Nombre d'échéances CMT déjà payées (PAYE)")
                .register(registry);

        // Échéances à venir à court terme (PREVU) — fenêtre administrable via /cmt-config
        Gauge.builder("gbanque.cmt.echeance.a_venir_j7",
                        echeanceCMTRepository,
                        repo -> (double) repo.countByStatutPaiementAndDateEcheanceBetween(
                                StatutEcheanceCMT.PREVU,
                                LocalDate.now(),
                                LocalDate.now().plusDays(cmtConfigService.getAlerteEcheanceJoursCourtTerme())))
                .description("Nombre d'échéances CMT PREVU dont la date tombe dans la fenêtre d'alerte court terme (jours administrables via /cmt-config)")
                .register(registry);

        // Échéances à venir à long terme (PREVU) — fenêtre administrable via /cmt-config
        Gauge.builder("gbanque.cmt.echeance.a_venir_j30",
                        echeanceCMTRepository,
                        repo -> (double) repo.countByStatutPaiementAndDateEcheanceBetween(
                                StatutEcheanceCMT.PREVU,
                                LocalDate.now(),
                                LocalDate.now().plusDays(cmtConfigService.getAlerteEcheanceJoursLongTerme())))
                .description("Nombre d'échéances CMT PREVU dont la date tombe dans la fenêtre d'alerte long terme (jours administrables via /cmt-config)")
                .register(registry);
    }
}
