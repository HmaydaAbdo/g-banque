
package staport.finance.gBanque.batch.jobs.functionaLines.generalFunctionalLines.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.batch.jobs.functionaLines.generalFunctionalLines.dtos.ProcessedFunctionalLine;
import staport.finance.gBanque.exception.customExceptions.ProcessingException;
import staport.finance.gBanque.notification.entities.NotificationType;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * ItemProcessor for the functional line expiration batch job.
 * This processor applies the core business logic to each active LigneFonctionnelle:
 * 1. Determines if the line's end date has passed and marks it for deactivation.
 * 2. Calculates remaining time until expiration and determines if a warning or alert notification
 * needs to be generated based on configured monthly thresholds.
 * It takes a LigneFonctionnelle as input and outputs a ProcessedFunctionalLine,
 * which may contain a modified LigneFonctionnelle entity and/or notification details,
 * and a flag indicating if the entity needs explicit persistence.
 * This class adheres to the Single Responsibility Principle by focusing solely on the processing logic.
 */
@Component
public class FunctionalLineProcessor implements ItemProcessor<LigneFonctionnelle, ProcessedFunctionalLine> {

    private static final Logger log = LoggerFactory.getLogger(FunctionalLineProcessor.class);

    @Value("${functional.line.expiration.alert.threshold.months}")
    private long alertThresholdMonths;

    @Value("${functional.line.expiration.warning.threshold.months}")
    private long warningThresholdMonths;

    @Override
    public ProcessedFunctionalLine process(LigneFonctionnelle item) {

        OffsetDateTime currentProcessingTime = OffsetDateTime.now();

        log.debug("Processing LigneFonctionnelle ID: {} at {}", item.getId(), currentProcessingTime);

        OffsetDateTime endDate = item.getDateFin();
        String notificationTitle = null;
        String notificationContent = null;
        NotificationType notificationType = null;

        boolean needsPersistence = false;

        try {
            // Règle 1: Ligne expirée
            if (endDate.isBefore(currentProcessingTime)) {
                if (item.isActif()) {
                    item.setActif(false); //  Désactiver la ligne
                    needsPersistence = true; //  Marquer pour persistence

                    notificationTitle = "Ligne Fonctionnelle Expirée";
                    notificationContent = buildExpiredNotification(item, endDate);
                    notificationType = NotificationType.ALERT;

                    log.warn("LigneFonctionnelle ID: {} deactivated (expired on {})",
                            item.getId(), endDate.toLocalDate());
                }
            }
            // Règle 2: Ligne active à venir
            else {
                long remainingMonths = ChronoUnit.MONTHS.between(
                        currentProcessingTime.toLocalDate(),
                        endDate.toLocalDate()
                );

                if (remainingMonths <= alertThresholdMonths) {
                    notificationTitle = "Ligne Fonctionnelle - Expiration Imminente";
                    notificationContent = buildUrgentNotification(item, endDate, remainingMonths);
                    notificationType = NotificationType.ALERT;
                    log.warn("ALERT: LigneFonctionnelle ID: {} expires in {} months",
                            item.getId(), remainingMonths);
                }
                else if (remainingMonths <= warningThresholdMonths) {
                    notificationTitle = "Ligne Fonctionnelle - Expiration Prochaine";
                    notificationContent = buildWarningNotification(item, endDate, remainingMonths);
                    notificationType = NotificationType.WARNING;
                    log.info("WARNING: LigneFonctionnelle ID: {} expires in {} months",
                            item.getId(), remainingMonths);
                }
            }
        } catch (Exception e) {
            log.error("Error processing LigneFonctionnelle ID: {}", item.getId(), e);
            throw new ProcessingException("Failed to process line " + item.getId(), e);
        }

        return new ProcessedFunctionalLine(
                item,
                notificationTitle,
                notificationContent,
                notificationType,
                needsPersistence
        );
    }

    private String buildExpiredNotification(LigneFonctionnelle item, OffsetDateTime endDate) {
        return String.format(
                "La ligne fonctionnelle ID '%d' (Banque: '%s', Nature: '%s') " +
                        "a expiré le %s et a été automatiquement désactivée.",
                item.getId(),
                getBankName(item),
                getNatureName(item),
                endDate.toLocalDate()
        );
    }

    private String buildUrgentNotification(LigneFonctionnelle item, OffsetDateTime endDate, long months) {
        return String.format(
                "URGENT: La ligne fonctionnelle ID '%d' (Banque: '%s', Nature: '%s') " +
                        "expire le %s. Il reste %d mois. Action immédiate requise!",
                item.getId(),
                getBankName(item),
                getNatureName(item),
                endDate.toLocalDate(),
                months
        );
    }

    private String buildWarningNotification(LigneFonctionnelle item, OffsetDateTime endDate, long months) {
        return String.format(
                "La ligne fonctionnelle ID '%d' (Banque: '%s', Nature: '%s') " +
                        "expire le %s. Il reste %d mois. Planifiez son renouvellement.",
                item.getId(),
                getBankName(item),
                getNatureName(item),
                endDate.toLocalDate(),
                months
        );
    }

    private String getBankName(LigneFonctionnelle item) {
        return item.getBanque() != null ? item.getBanque().getNom() : "N/A";
    }

    private String getNatureName(LigneFonctionnelle item) {
        return item.getNatureLigneFonctionnelle() != null
                ? item.getNatureLigneFonctionnelle().getLabel()
                : "N/A";
    }
}