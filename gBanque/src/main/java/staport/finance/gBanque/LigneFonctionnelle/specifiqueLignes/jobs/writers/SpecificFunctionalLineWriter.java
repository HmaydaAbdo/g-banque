package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.jobs.writers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.jobs.dtos.ProcessedSpecificFunctionalLine;
import staport.finance.gBanque.notification.service.NotificationService;


@Component
public class SpecificFunctionalLineWriter implements ItemWriter<ProcessedSpecificFunctionalLine> {

    private static final Logger log = LoggerFactory.getLogger(SpecificFunctionalLineWriter.class);


    private final NotificationService notificationService;

    public SpecificFunctionalLineWriter(NotificationService notificationService) {
        this.notificationService = notificationService;
        log.info("SpecificFunctionalLineWriter initialized.");
    }

    @Override
    @Transactional
    public void write(Chunk<? extends ProcessedSpecificFunctionalLine> chunk)   {
        log.info("Starting to write chunk of {} processed specific functional lines.", chunk.getItems().size());

        for (ProcessedSpecificFunctionalLine processedLine : chunk.getItems()) {
            LigneFonctionnelleSpecifique ligne = processedLine.getLigneFonctionnelleSpecifique();


            if (processedLine.hasNotification()) {
                try {
                    notificationService.sendPublicNotification(
                            processedLine.getNotificationTitle(),
                            processedLine.getNotificationContent(),
                            processedLine.getNotificationType()
                    );
                } catch (Exception e) {
                    log.error("Failed to send notification for LigneFonctionnelleSpecifique ID: {}.", ligne.getId(), e);
                    throw e; // Rollback transaction
                }
            }
        }


        log.info("Finished writing chunk.");
    }
}