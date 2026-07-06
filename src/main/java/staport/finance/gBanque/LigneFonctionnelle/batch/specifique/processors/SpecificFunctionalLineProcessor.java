package staport.finance.gBanque.LigneFonctionnelle.batch.specifique.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.batch.specifique.dtos.ProcessedSpecificFunctionalLine;
import staport.finance.gBanque.exception.customExceptions.ProcessingException;
import staport.finance.gBanque.notification.entities.NotificationType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * ItemProcessor for the specific functional line expiration batch job.
 * Processes active LigneFonctionnelleSpecifique entities to:
 * 1. Deactivate expired lines
 * 2. Generate alerts for lines expiring within alert threshold
 * 3. Generate warnings for lines expiring within warning threshold
 */
@Component
public class SpecificFunctionalLineProcessor implements ItemProcessor<LigneFonctionnelleSpecifique, ProcessedSpecificFunctionalLine> {

    private static final Logger log = LoggerFactory.getLogger(SpecificFunctionalLineProcessor.class);

    @Value("${functional.line.expiration.alert.threshold.months}")
    private long alertThresholdMonths;

    @Value("${functional.line.expiration.warning.threshold.months}")
    private long warningThresholdMonths;

    @Override
    public ProcessedSpecificFunctionalLine process(LigneFonctionnelleSpecifique item) {
        LocalDate currentProcessingTime = LocalDate.now();

        log.debug("Processing LigneFonctionnelleSpecifique ID: {} (Marché: {}, Bank: {}, EndDate: {}) at {}",
                item.getId(),
                getMarcheName(item),
                getBankName(item),
                item.getDateFin(),
                currentProcessingTime);

        LocalDate endDate = item.getDateFin();
        String notificationTitle = null;
        String notificationContent = null;
        NotificationType notificationType = null;
        boolean needsPersistence = false;

        String marcheNom = getMarcheName(item);
        String banqueNom = getBankName(item);
        String natureLabel = getNatureLabel(item);

        try {
            // Règle 1: Ligne expirée
            if (endDate.isBefore(currentProcessingTime)) {
                if (item.isActif()) {

                    notificationTitle = String.format("Ligne Spécifique %s Expirée - Marché %s",
                            natureLabel, marcheNom);
                    notificationContent = buildExpiredNotification(item, marcheNom, banqueNom, endDate);
                    notificationType = NotificationType.ALERT;

                    log.warn("LigneFonctionnelleSpecifique ID: {} deactivated (expired on {})",
                            item.getId(), endDate);
                }
            }
            // Règle 2: Ligne active à venir
            else {
                long remainingMonths = ChronoUnit.MONTHS.between(currentProcessingTime, endDate);

                if (remainingMonths <= alertThresholdMonths) {
                    notificationTitle = String.format("Ligne Spécifique %s - Expiration Imminente - Marché %s",
                            natureLabel, marcheNom);
                    notificationContent = buildUrgentNotification(item, marcheNom, banqueNom, endDate, remainingMonths);
                    notificationType = NotificationType.ALERT;

                    log.warn("ALERT: LigneFonctionnelleSpecifique ID: {} expires in {} months",
                            item.getId(), remainingMonths);
                }
                else if (remainingMonths <= warningThresholdMonths) {
                    notificationTitle = String.format("Ligne Spécifique %s - Expiration Prochaine - Marché %s",
                            natureLabel, marcheNom);
                    notificationContent = buildWarningNotification(item, marcheNom, banqueNom, endDate, remainingMonths);
                    notificationType = NotificationType.WARNING;

                    log.info("WARNING: LigneFonctionnelleSpecifique ID: {} expires in {} months",
                            item.getId(), remainingMonths);
                }
            }
        } catch (Exception e) {
            log.error("Error processing LigneFonctionnelleSpecifique ID: {}", item.getId(), e);
            throw new ProcessingException("Failed to process specific line " + item.getId(), e);
        }

        return new ProcessedSpecificFunctionalLine(
                item,
                notificationTitle,
                notificationContent,
                notificationType,
                needsPersistence
        );
    }

    private String buildExpiredNotification(LigneFonctionnelleSpecifique item, String marcheNom,
                                            String banqueNom, LocalDate endDate) {
        return String.format(
                "La ligne spécifique ID '%d' (Banque: '%s', Marché: '%s', Nature: '%s') " +
                        "a expiré le %s.",
                item.getId(),
                banqueNom,
                marcheNom,
                getNatureLabel(item),
                endDate
        );
    }

    private String buildUrgentNotification(LigneFonctionnelleSpecifique item, String marcheNom,
                                           String banqueNom, LocalDate endDate, long months) {
        return String.format(
                "URGENT: La ligne spécifique ID '%d' (Banque: '%s', Marché: '%s', Nature: '%s') " +
                        "expire le %s. Il reste %d mois. Action immédiate requise!",
                item.getId(),
                banqueNom,
                marcheNom,
                getNatureLabel(item),
                endDate,
                months
        );
    }

    private String buildWarningNotification(LigneFonctionnelleSpecifique item, String marcheNom,
                                            String banqueNom, LocalDate endDate, long months) {
        return String.format(
                "La ligne spécifique ID '%d' (Banque: '%s', Marché: '%s', Nature: '%s') " +
                        "expire le %s. Il reste %d mois. Planifiez son renouvellement.",
                item.getId(),
                banqueNom,
                marcheNom,
                getNatureLabel(item),
                endDate,
                months
        );
    }

    private String getMarcheName(LigneFonctionnelleSpecifique item) {
        return item.getMarche() != null ? item.getMarche().getNom() : "N/A";
    }

    private String getBankName(LigneFonctionnelleSpecifique item) {
        return item.getMarche() != null && item.getMarche().getBanque() != null
                ? item.getMarche().getBanque().getNom()
                : "N/A";
    }

    private String getNatureLabel(LigneFonctionnelleSpecifique item) {
        return item.getNatureLigneFonctionnelle() != null
                ? item.getNatureLigneFonctionnelle().getLabel()
                : "N/A";
    }
}