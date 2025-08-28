
package staport.finance.gBanque.LigneFonctionnelle.jobs.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.jobs.dtos.ProcessedFunctionalLine;
import staport.finance.gBanque.notification.entities.NotificationType;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * ItemProcessor for the functional line expiration batch job.
 * This processor applies the core business logic to each active LigneFonctionnelle:
 * 1. Determines if the line's end date has passed and marks it for deactivation.
 * 2. Calculates remaining time until expiration and determines if a warning or alert notification
 * needs to be generated based on configured monthly thresholds.
 *
 * It takes a LigneFonctionnelle as input and outputs a ProcessedFunctionalLine,
 * which may contain a modified LigneFonctionnelle entity and/or notification details,
 * and a flag indicating if the entity needs explicit persistence.
 * This class adheres to the Single Responsibility Principle by focusing solely on the processing logic.
 */
@Component
public class FunctionalLineProcessor implements ItemProcessor<LigneFonctionnelle, ProcessedFunctionalLine> {

    private static final Logger log = LoggerFactory.getLogger(FunctionalLineProcessor.class);

    // The current time for processing, captured once per processor instance for consistency.
    private final OffsetDateTime currentProcessingTime = OffsetDateTime.now();

    // Configurable thresholds for monthly notifications
    @Value("${functional.line.expiration.alert.threshold.months}")
    private long alertThresholdMonths;

    @Value("${functional.line.expiration.warning.threshold.months}")
    private long warningThresholdMonths;


    /**
     * Processes a single LigneFonctionnelle item.
     *
     * @param item The LigneFonctionnelle entity to be processed.
     * @return A ProcessedFunctionalLine object containing the updated LigneFonctionnelle
     * and potential notification details, along with a flag for persistence.
     * @throws Exception if any processing error occurs.
     */
    @Override
    public ProcessedFunctionalLine process(LigneFonctionnelle item) throws Exception {
        log.debug("Processing LigneFonctionnelle ID: {} (Bank: {}, Nature: {}, EndDate: {}) at {}.",
                item.getId(),
                item.getBanque() != null ? item.getBanque().getNom() : "N/A",
                item.getNatureLigneFonctionnelle() != null ? item.getNatureLigneFonctionnelle().getLabel() : "N/A",
                item.getDateFin(),
                currentProcessingTime);

        OffsetDateTime endDate = item.getDateFin();
        boolean needsPersistence = false; // Flag to indicate if the item needs to be saved
        String notificationTitle = null;
        String notificationContent = null;
        NotificationType notificationType = null;

        try {
            // Rule 1: If end date has already passed
            if (endDate.isBefore(currentProcessingTime)) {
                if (item.isActif()) { // Only deactivate if it's currently active

                    // Prepare an ALERT notification for immediate action
                    notificationTitle = "Une ligne Fonctionnelle a Expirée ";
                    notificationContent = String.format("La ligne fonctionnelle avec ID '%d' (Banque: '%s', Nature: '%s') est expirée le %s. Elle faut la désactiver.",
                            item.getId(),
                            item.getBanque() != null ? item.getBanque().getNom() : "N/A",
                            item.getNatureLigneFonctionnelle() != null ? item.getNatureLigneFonctionnelle().getLabel() : "N/A",
                            endDate.toLocalDate().toString());
                    notificationType = NotificationType.ALERT;
                } else {
                    log.debug("LigneFonctionnelle ID: {} is already inactive, no deactivation needed. End date: {}", item.getId(), endDate.toLocalDate());
                }
            }
            // Rule 2: If end date has not yet arrived
            else {
                // Calculate remaining time in months. Using toLocalDate() for ChronoUnit.MONTHS to get a pure month difference.
                long remainingMonths = ChronoUnit.MONTHS.between(currentProcessingTime.toLocalDate(), endDate.toLocalDate());

                log.debug("LigneFonctionnelle ID: {}: Remaining time - {} months.", item.getId(), remainingMonths);

                // Priority check: ALERT for remaining time less than or equal to `alertThresholdMonths` (e.g., 1 month or less)
                if (remainingMonths <= alertThresholdMonths) {
                    log.warn("LigneFonctionnelle ID: {} meets ALERT criteria ({} months remaining). Immediate action required!", item.getId(), remainingMonths);
                    notificationTitle = "Ligne Fonctionnelle va expirer prochainement ";
                    notificationContent = String.format("La ligne fonctionnelle ID '%d' (Banque: '%s', Nature: '%s') expirera très bientôt le %s. Il reste environ %d mois. Une action URGENTE est nécessaire !",
                            item.getId(),
                            item.getBanque() != null ? item.getBanque().getNom() : "N/A",
                            item.getNatureLigneFonctionnelle() != null ? item.getNatureLigneFonctionnelle().getLabel() : "N/A",
                            endDate.toLocalDate().toString(),
                            remainingMonths);
                    notificationType = NotificationType.ALERT;
                }
                // Secondary check: WARNING for remaining time less than or equal to `warningThresholdMonths`
                // and greater than `alertThresholdMonths` (e.g., more than 1 month, but 2 months or less)
                else if (remainingMonths <= warningThresholdMonths) {
                    log.info("LigneFonctionnelle ID: {} meets WARNING criteria ({} months remaining).", item.getId(), remainingMonths);
                    notificationTitle = " Une Ligne Fonctionnelle expirera prochainement";
                    notificationContent = String.format("La ligne fonctionnelle ID '%d' (Banque: '%s', Nature: '%s') expirera le %s. Il reste environ %d mois. Veuillez planifier son renouvellement.",
                            item.getId(),
                            item.getBanque() != null ? item.getBanque().getNom() : "N/A",
                            item.getNatureLigneFonctionnelle() != null ? item.getNatureLigneFonctionnelle().getLabel() : "N/A",
                            endDate.toLocalDate().toString(),
                            remainingMonths);
                    notificationType = NotificationType.WARNING;
                } else {
                    log.debug("LigneFonctionnelle ID: {}: No specific notification criteria met for remaining time ({} months).", item.getId(), remainingMonths);
                }
            }
        } catch (Exception e) {
            log.error("Error processing LigneFonctionnelle ID: {}. Error: {}", item.getId(), e.getMessage(), e);
            throw e; // Re-throw the exception to trigger Spring Batch error handling
        }

        // Construct the ProcessedFunctionalLine with all determined values
        ProcessedFunctionalLine result = new ProcessedFunctionalLine(item, notificationTitle, notificationContent, notificationType, needsPersistence);
        log.debug("Finished processing LigneFonctionnelle ID: {}. Notification status: {}, Needs Persistence: {}",
                item.getId(), result.hasNotification() ? result.getNotificationType() : "NONE", result.getNeedsPersistence());
        return result;
    }
}
