package staport.finance.gBanque.batchJobs.generalFunctionalLines.writers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.batchJobs.generalFunctionalLines.dtos.ProcessedFunctionalLine;
import staport.finance.gBanque.notification.service.NotificationService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ItemWriter for the functional line expiration batch job.
 * This writer is responsible for two main tasks:
 * 1. Persisting LigneFonctionnelle entities that were explicitly marked for persistence
 *    by the ItemProcessor (e.g., lines that were deactivated due to expiration).
 * 2. Sending out notifications based on the details prepared by the ItemProcessor.
 * The write operation is transactional, ensuring that both persistence and notifications
 * succeed or fail together for each chunk.
 * This class adheres to the Single Responsibility Principle by focusing solely on
 * writing data and triggering side-effects (notifications).
 */
@Component
public class FunctionalLineWriter implements ItemWriter<ProcessedFunctionalLine> {

    private static final Logger log = LoggerFactory.getLogger(FunctionalLineWriter.class);

    private final NotificationService notificationService;
    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;

    /**
     * Constructs a FunctionalLineWriter with required dependencies.
     *
     * @param notificationService The service to send notifications.
     * @param ligneFonctionnelleRepository The repository to persist functional lines.
     */
    @Autowired
    public FunctionalLineWriter(
            NotificationService notificationService,
            LigneFonctionnelleRepository ligneFonctionnelleRepository) {
        this.notificationService = notificationService;
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        log.info("FunctionalLineWriter initialized with NotificationService and LigneFonctionnelleRepository.");
    }

    /**
     * Writes a chunk of processed functional lines. This method is transactional,
     * ensuring that all database updates (for modified lines) and notification generations
     * for a given chunk are committed together or rolled back if an error occurs.
     * The write process follows this order:
     * 1. Persist all modified functional lines (those marked with needsPersistence = true)
     * 2. Send notifications for all processed lines that require them
     * This ordering ensures data consistency: we first save the state changes,
     * then notify users about those changes.
     *
     * @param chunk The chunk of ProcessedFunctionalLine items to write.
     * @throws Exception if persistence or notification sending fails, triggering transaction rollback.
     */
    @Override
    @Transactional // Ensures atomicity: all saves and notifications within the chunk succeed or fail together.
    public void write(Chunk<? extends ProcessedFunctionalLine> chunk) throws Exception {
        log.info("Starting to write chunk of {} processed functional lines.", chunk.getItems().size());

        try {
            // Step 1: Persist modified functional lines
            persistModifiedLines(chunk);

            // Step 2: Send notifications
            sendNotifications(chunk);

            log.info("Finished writing chunk successfully.");
        } catch (Exception e) {
            log.error("Error occurred while writing chunk. Transaction will be rolled back.", e);
            throw e; // Re-throw to trigger Spring Batch error handling and transaction rollback
        }
    }

    /**
     * Persists all functional lines that were marked for persistence by the processor.
     * Typically, these are lines that have been deactivated due to expiration.
     *
     * @param chunk The chunk of processed functional lines.
     */
    private void persistModifiedLines(Chunk<? extends ProcessedFunctionalLine> chunk) {
        // Filter lines that need persistence (e.g., deactivated expired lines)
        List<LigneFonctionnelle> linesToPersist = chunk.getItems().stream()
                .filter(ProcessedFunctionalLine::needsPersistence)
                .map(ProcessedFunctionalLine::getLigneFonctionnelle)
                .collect(Collectors.toList());

        if (linesToPersist.isEmpty()) {
            log.debug("No functional lines need persistence in this chunk.");
            return;
        }

        try {
            // Batch save all modified lines for efficiency
            ligneFonctionnelleRepository.saveAll(linesToPersist);
            log.info("Successfully persisted {} modified functional line(s).", linesToPersist.size());

            // Log details of persisted lines at debug level
            if (log.isDebugEnabled()) {
                linesToPersist.forEach(line ->
                        log.debug("Persisted LigneFonctionnelle ID: {}, actif: {}, dateModification: {}",
                                line.getId(), line.isActif(), line.getDateModification())
                );
            }
        } catch (Exception e) {
            log.error("Failed to persist {} functional line(s). Error: {}",
                    linesToPersist.size(), e.getMessage(), e);
            throw e; // Re-throw to rollback transaction
        }
    }

    /**
     * Sends notifications for all processed lines that have notification details.
     * This includes both deactivation alerts and expiration warnings.
     *
     * @param chunk The chunk of processed functional lines.
     */
    private void sendNotifications(Chunk<? extends ProcessedFunctionalLine> chunk) {
        int notificationsSent = 0;
        int notificationsFailed = 0;

        for (ProcessedFunctionalLine processedLine : chunk.getItems()) {
            if (processedLine.hasNotification()) {
                try {
                    sendSingleNotification(processedLine);
                    notificationsSent++;
                } catch (Exception e) {
                    notificationsFailed++;
                    LigneFonctionnelle ligne = processedLine.getLigneFonctionnelle();
                    log.error("Failed to send notification for LigneFonctionnelle ID: {}. " +
                                    "Title: '{}', Type: {}. Error: {}",
                            ligne.getId(),
                            processedLine.getNotificationTitle(),
                            processedLine.getNotificationType(),
                            e.getMessage(), e);
                    // Re-throw to rollback the entire chunk's transaction
                    throw e;
                }
            } else {
                log.debug("No notification required for LigneFonctionnelle ID: {}.",
                        processedLine.getLigneFonctionnelle().getId());
            }
        }

        if (notificationsSent > 0) {
            log.info("Successfully sent {} notification(s) in this chunk.", notificationsSent);
        }
    }

    /**
     * Sends a single notification for a processed functional line.
     *
     * @param processedLine The processed functional line containing notification details.
     */
    private void sendSingleNotification(ProcessedFunctionalLine processedLine) {
        LigneFonctionnelle ligne = processedLine.getLigneFonctionnelle();

        notificationService.sendPublicNotification(
                processedLine.getNotificationTitle(),
                processedLine.getNotificationContent(),
                processedLine.getNotificationType()
        );

        log.info("Notification sent successfully for LigneFonctionnelle ID: {} (Bank: {}, Nature: {}). Type: {}",
                ligne.getId(),
                ligne.getBanque() != null ? ligne.getBanque().getNom() : "N/A",
                ligne.getNatureLigneFonctionnelle() != null ? ligne.getNatureLigneFonctionnelle().getLabel() : "N/A",
                processedLine.getNotificationType());
    }
}