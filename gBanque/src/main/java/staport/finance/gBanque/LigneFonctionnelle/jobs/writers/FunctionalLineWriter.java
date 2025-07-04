package staport.finance.gBanque.LigneFonctionnelle.jobs.writers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.jobs.dtos.ProcessedFunctionalLine;
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.notification.service.NotificationService;

import java.util.ArrayList;
import java.util.List;

/**
 * ItemWriter for the functional line expiration batch job.
 * This writer is responsible for two main tasks:
 * 1. Persisting *only* the LigneFonctionnelle entities that were explicitly marked for persistence
 * by the ItemProcessor (e.g., due to deactivation).
 * 2. Sending out notifications based on the details prepared by the ItemProcessor.
 *
 * This class adheres to the Single Responsibility Principle by focusing solely on writing data
 * and triggering side-effects (notifications).
 */
@Component
public class FunctionalLineWriter implements ItemWriter<ProcessedFunctionalLine> {

    private static final Logger log = LoggerFactory.getLogger(FunctionalLineWriter.class);

    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    private final NotificationService notificationService;

    /**
     * Constructs a FunctionalLineWriter.
     *
     * @param ligneFonctionnelleRepository The repository to save LigneFonctionnelle entities.
     * @param notificationService The service to send notifications.
     */
    @Autowired
    public FunctionalLineWriter(LigneFonctionnelleRepository ligneFonctionnelleRepository,
                                NotificationService notificationService) {
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        this.notificationService = notificationService;
        log.info("FunctionalLineWriter initialized.");
    }

    /**
     * Writes a chunk of processed functional lines. This method is transactional,
     * ensuring that all database updates (for deactivated lines) and notification generations
     * for a given chunk are committed together or rolled back if an error occurs.
     *
     * @param chunk The chunk of ProcessedFunctionalLine items to write.
     * @throws Exception if any error occurs during writing or notification sending.
     */
    @Override
    @Transactional // Ensures atomicity: all saves and notifications within the chunk succeed or fail together.
    public void write(Chunk<? extends ProcessedFunctionalLine> chunk) throws Exception {
        log.info("Starting to write chunk of {} processed functional lines.", chunk.getItems().size());

        List<LigneFonctionnelle> linesToSave = new ArrayList<>();

        for (ProcessedFunctionalLine processedLine : chunk.getItems()) {
            LigneFonctionnelle ligne = processedLine.getLigneFonctionnelle();

            // Add the line to the list for batch saving ONLY if it was marked for persistence
            if (processedLine.getNeedsPersistence()) {
                linesToSave.add(ligne);
                log.debug("LigneFonctionnelle ID: {} added to save list (NeedsPersistence = true). Actif status: {}.", ligne.getId(), ligne.isActif());
            } else {
                log.debug("LigneFonctionnelle ID: {} not added to save list (NeedsPersistence = false).", ligne.getId());
            }

            // Send notification if one was prepared by the processor, notifications are always persisted
            if (processedLine.hasNotification()) {
                try {
                    notificationService.sendPublicNotification(
                            processedLine.getNotificationTitle(),
                            processedLine.getNotificationContent(),
                            processedLine.getNotificationType()
                    );
                    log.info("Notification sent successfully for LigneFonctionnelle ID: {}. Type: {}", ligne.getId(), processedLine.getNotificationType());
                } catch (Exception e) {
                    log.error("Failed to send notification for LigneFonctionnelle ID: {}. Title: '{}', Type: {}. Error: {}",
                            ligne.getId(), processedLine.getNotificationTitle(), processedLine.getNotificationType(), e.getMessage(), e);
                    // Re-throw to rollback the current chunk's transaction if notification sending fails
                    throw e;
                }
            } else {
                log.debug("No notification generated for LigneFonctionnelle ID: {}.", ligne.getId());
            }
        }

        // Save all modified/processed functional lines in a single batch operation for efficiency
        if (!linesToSave.isEmpty()) {
            try {
                ligneFonctionnelleRepository.saveAll(linesToSave);
                log.info("Successfully saved {} LigneFonctionnelle entities that required persistence.", linesToSave.size());
            }
            catch (Exception e) {
                log.error("Failed to save LigneFonctionnelle entities in chunk. Error: {}", e.getMessage(), e);
                // Re-throw to trigger transaction rollback for the entire chunk
                throw e;
            }
        } else {
            log.info("No LigneFonctionnelle entities explicitly marked for persistence in this chunk.");
        }
        log.info("Finished writing chunk.");
    }
}
