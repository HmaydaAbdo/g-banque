package staport.finance.gBanque.cmt.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.cmt.batch.dto.ProcessedCreditMoyenTerme;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.notification.service.NotificationService;

@Component
public class CreditMoyenTermeMainLeveeWriter implements ItemWriter<ProcessedCreditMoyenTerme> {

    private static final Logger log = LoggerFactory.getLogger(CreditMoyenTermeMainLeveeWriter.class);

    private final NotificationService notificationService;

    @Autowired
    public CreditMoyenTermeMainLeveeWriter(NotificationService notificationService) {
        this.notificationService = notificationService;
        log.info("Initialisation de CreditMoyenTermeMainLeveeWriter avec NotificationService");
    }

    @Override
    @Transactional
    public void write(Chunk<? extends ProcessedCreditMoyenTerme> chunk) throws Exception {
        log.info("Début de l'écriture d'un chunk de {} crédits traités", chunk.getItems().size());

        try {
            sendNotifications(chunk);
            log.info("Écriture du chunk terminée avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de l'écriture du chunk - La transaction sera annulée", e);
            throw e;
        }
    }

    private void sendNotifications(Chunk<? extends ProcessedCreditMoyenTerme> chunk) {
        int notificationsSent = 0;

        for (ProcessedCreditMoyenTerme processedCredit : chunk.getItems()) {
            if (processedCredit.hasNotification()) {
                try {
                    sendSingleNotification(processedCredit);
                    notificationsSent++;
                } catch (Exception e) {
                    CreditMoyenTerme credit = processedCredit.getCreditMoyenTerme();
                    log.error("Échec de l'envoi de notification pour le crédit CMT ID: {}. " +
                                    "Désignation: '{}', Type: {}. Erreur: {}",
                            credit.getId(),
                            credit.getDesignation(),
                            processedCredit.getNotificationType(),
                            e.getMessage(), e);
                    throw e;
                }
            } else {
                log.debug("Aucune notification requise pour le crédit CMT ID: {}",
                        processedCredit.getCreditMoyenTerme().getId());
            }
        }

        if (notificationsSent > 0) {
            log.info(" {} notification(s) envoyée(s) avec succès dans ce chunk", notificationsSent);
        }
    }

    private void sendSingleNotification(ProcessedCreditMoyenTerme processedCredit) {
        CreditMoyenTerme credit = processedCredit.getCreditMoyenTerme();

        notificationService.sendPublicNotification(
                processedCredit.getNotificationTitle(),
                processedCredit.getNotificationContent(),
                processedCredit.getNotificationType()
        );

        log.info(" Notification envoyée avec succès pour le crédit CMT ID: {} (Désignation: {}, Banque: {}). Type: {}",
                credit.getId(),
                credit.getDesignation(),
                credit.getBanque() != null ? credit.getBanque().getNom() : "N/A",
                processedCredit.getNotificationType());
    }
}