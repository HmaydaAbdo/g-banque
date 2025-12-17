package staport.finance.gBanque.batchJobs.specifiqueFunctionalLines.writers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.batchJobs.specifiqueFunctionalLines.dtos.ProcessedSpecificFunctionalLine;
import staport.finance.gBanque.notification.service.NotificationService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ItemWriter for the specific functional line expiration batch job.
 * Responsible for:
 * 1. Persisting modified specific functional lines (deactivated lines)
 * 2. Sending notifications based on processor analysis
 */
@Component
public class SpecificFunctionalLineWriter implements ItemWriter<ProcessedSpecificFunctionalLine> {

    private static final Logger log = LoggerFactory.getLogger(SpecificFunctionalLineWriter.class);

    private final NotificationService notificationService;
    private final LigneFonctionnelleSpecifiqueRepository repository;

    @Autowired
    public SpecificFunctionalLineWriter(
            NotificationService notificationService,
            LigneFonctionnelleSpecifiqueRepository repository) {
        this.notificationService = notificationService;
        this.repository = repository;
        log.info("SpecificFunctionalLineWriter initialized with NotificationService and Repository.");
    }

    @Override
    @Transactional
    public void write(Chunk<? extends ProcessedSpecificFunctionalLine> chunk) throws Exception {
        log.info("Starting to write chunk of {} processed specific functional lines.", chunk.getItems().size());

        try {
            // Étape 1: Persister les lignes modifiées
            persistModifiedLines(chunk);

            // Étape 2: Envoyer les notifications
            sendNotifications(chunk);

            log.info("Finished writing chunk successfully.");
        } catch (Exception e) {
            log.error("Error occurred while writing chunk. Transaction will be rolled back.", e);
            throw e;
        }
    }

    /**
     * Persists specific functional lines marked for persistence (deactivated lines).
     */
    private void persistModifiedLines(Chunk<? extends ProcessedSpecificFunctionalLine> chunk) {
        List<LigneFonctionnelleSpecifique> linesToPersist = chunk.getItems().stream()
                .filter(ProcessedSpecificFunctionalLine::needsPersistence)
                .map(ProcessedSpecificFunctionalLine::getLigneFonctionnelleSpecifique)
                .collect(Collectors.toList());

        if (linesToPersist.isEmpty()) {
            log.debug("No specific functional lines need persistence in this chunk.");
            return;
        }

        try {
            repository.saveAll(linesToPersist);
            log.info("Successfully persisted {} modified specific functional line(s).", linesToPersist.size());

            if (log.isDebugEnabled()) {
                linesToPersist.forEach(line ->
                        log.debug("Persisted LigneFonctionnelleSpecifique ID: {}, actif: {}",
                                line.getId(), line.isActif())
                );
            }
        } catch (Exception e) {
            log.error("Failed to persist {} specific functional line(s). Error: {}",
                    linesToPersist.size(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Sends notifications for processed lines that have notification details.
     */
    private void sendNotifications(Chunk<? extends ProcessedSpecificFunctionalLine> chunk) {
        int notificationsSent = 0;
        int notificationsFailed = 0;

        for (ProcessedSpecificFunctionalLine processedLine : chunk.getItems()) {
            if (processedLine.hasNotification()) {
                try {
                    sendSingleNotification(processedLine);
                    notificationsSent++;
                } catch (Exception e) {
                    notificationsFailed++;
                    log.error("Failed to send notification for LigneFonctionnelleSpecifique ID: {}. Error: {}",
                            processedLine.getLigneFonctionnelleSpecifique().getId(), e.getMessage(), e);
                    throw e; // Rollback transaction if notification fails
                }
            } else {
                log.debug("No notification required for LigneFonctionnelleSpecifique ID: {}.",
                        processedLine.getLigneFonctionnelleSpecifique().getId());
            }
        }

        if (notificationsSent > 0) {
            log.info("Successfully sent {} notification(s) in this chunk.", notificationsSent);
        }
    }

    /**
     * Sends a single notification for a processed specific functional line.
     */
    private void sendSingleNotification(ProcessedSpecificFunctionalLine processedLine) {
        LigneFonctionnelleSpecifique ligne = processedLine.getLigneFonctionnelleSpecifique();

        notificationService.sendPublicNotification(
                processedLine.getNotificationTitle(),
                processedLine.getNotificationContent(),
                processedLine.getNotificationType()
        );

        log.info("Notification sent successfully for LigneFonctionnelleSpecifique ID: {} (Marché: {}). Type: {}",
                ligne.getId(),
                ligne.getMarche() != null ? ligne.getMarche().getNom() : "N/A",
                processedLine.getNotificationType());
    }
}