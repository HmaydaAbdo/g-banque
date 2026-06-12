package staport.finance.gBanque.leasing.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.leasing.batch.dto.ProcessedContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.notification.service.NotificationService;

@Component
public class ContratLeasingMainLeveeWriter implements ItemWriter<ProcessedContratLeasing> {

    private static final Logger log = LoggerFactory.getLogger(ContratLeasingMainLeveeWriter.class);

    private final NotificationService notificationService;

    @Autowired
    public ContratLeasingMainLeveeWriter(NotificationService notificationService) {
        this.notificationService = notificationService;
        log.info("ContratLeasingMainLeveeWriter initialized with NotificationService.");
    }

    @Override
    @Transactional
    public void write(Chunk<? extends ProcessedContratLeasing> chunk) throws Exception {
        log.info("Starting to write chunk of {} processed contracts.", chunk.getItems().size());

        try {
            sendNotifications(chunk);
            log.info("Finished writing chunk successfully.");
        } catch (Exception e) {
            log.error("Error occurred while writing chunk. Transaction will be rolled back.", e);
            throw e;
        }
    }

    private void sendNotifications(Chunk<? extends ProcessedContratLeasing> chunk) {
        int notificationsSent = 0;

        for (ProcessedContratLeasing processedContrat : chunk.getItems()) {
            if (processedContrat.hasNotification()) {
                try {
                    sendSingleNotification(processedContrat);
                    notificationsSent++;
                } catch (Exception e) {
                    ContratLeasing contrat = processedContrat.getContratLeasing();
                    log.error("Failed to send notification for ContratLeasing ID: {}. " +
                                    "Numéro: '{}', Type: {}. Error: {}",
                            contrat.getId(),
                            contrat.getNumeroContrat(),
                            processedContrat.getNotificationType(),
                            e.getMessage(), e);
                    throw e;
                }
            } else {
                log.debug("No notification required for ContratLeasing ID: {}.",
                        processedContrat.getContratLeasing().getId());
            }
        }

        if (notificationsSent > 0) {
            log.info("Successfully sent {} notification(s) in this chunk.", notificationsSent);
        }
    }

    private void sendSingleNotification(ProcessedContratLeasing processedContrat) {
        ContratLeasing contrat = processedContrat.getContratLeasing();

        notificationService.sendPublicNotification(
                processedContrat.getNotificationTitle(),
                processedContrat.getNotificationContent(),
                processedContrat.getNotificationType()
        );

        log.info("Notification sent successfully for ContratLeasing ID: {} (Numéro: {}, Organisme: {}). Type: {}",
                contrat.getId(),
                contrat.getNumeroContrat(),
                contrat.getOrganismeLeasing() != null ? contrat.getOrganismeLeasing().getNom() : "N/A",
                processedContrat.getNotificationType());
    }
}