package staport.finance.gBanque.cmt.batch.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.cmt.batch.dto.ProcessedCreditMoyenTerme;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.exception.customExceptions.ProcessingException;
import staport.finance.gBanque.notification.entities.NotificationType;

@Component
public class CreditMoyenTermeMainLeveeProcessor implements ItemProcessor<CreditMoyenTerme, ProcessedCreditMoyenTerme> {

    private static final Logger log = LoggerFactory.getLogger(CreditMoyenTermeMainLeveeProcessor.class);

    @Override
    public ProcessedCreditMoyenTerme process(CreditMoyenTerme credit) {
        log.debug("Traitement du crédit CMT ID: {}, Désignation: {}", credit.getId(), credit.getDesignation());

        if (credit.isCompleted()) {
            log.debug("Crédit {} déjà terminé, ignoré", credit.getDesignation());
            return null;
        }

        if (credit.getEcheanceCMTS() == null || credit.getEcheanceCMTS().isEmpty()) {
            log.debug("Crédit {} sans échéances, ignoré", credit.getDesignation());
            return null;
        }

        try {
            boolean allPaid = credit.getEcheanceCMTS().stream()
                    .allMatch(echeance -> echeance.getStatutPaiement() == StatutEcheanceCMT.PAYE);

            if (allPaid) {
                log.warn(" ALERTE: Crédit CMT {} nécessite une main levée - Toutes les échéances payées", credit.getDesignation());

                String notificationTitle = "Main Levée Requise - Crédit à Moyen Terme";
                String notificationContent = buildNotificationContent(credit);
                NotificationType notificationType = NotificationType.ALERT;

                return new ProcessedCreditMoyenTerme(
                        credit,
                        notificationTitle,
                        notificationContent,
                        notificationType
                );
            }

            log.debug("Crédit {} : Échéances non toutes payées", credit.getDesignation());
            return null;

        } catch (Exception e) {
            log.error("Erreur lors du traitement du crédit CMT ID: {}", credit.getId(), e);
            throw new ProcessingException("Échec du traitement du crédit " + credit.getId(), e);
        }
    }

    private String buildNotificationContent(CreditMoyenTerme credit) {
        return String.format(
                "Une action est requise concernant le crédit à moyen terme '%s' de la banque %s. " +
                        "Toutes les échéances de ce crédit ont été intégralement payées. " +
                        "Nous vous prions de bien vouloir obtenir la main levée auprès de la banque " +
                        "et de procéder à la clôture administrative du crédit dans le système.",
                credit.getDesignation(),
                getBankName(credit)
        );
    }

    private String getBankName(CreditMoyenTerme credit) {
        return credit.getBanque() != null
                ? credit.getBanque().getNom()
                : "N/A";
    }
}