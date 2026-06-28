package staport.finance.gBanque.leasing.batch.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.batch.dto.ProcessedContratLeasing;
import staport.finance.gBanque.exception.customExceptions.ProcessingException;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.notification.entities.NotificationType;



@Component
public class ContratLeasingMainLeveeProcessor implements ItemProcessor<ContratLeasing, ProcessedContratLeasing> {

    private static final Logger log = LoggerFactory.getLogger(ContratLeasingMainLeveeProcessor.class);

    @Override
    public ProcessedContratLeasing process(ContratLeasing contrat) {
        log.debug("Processing ContratLeasing ID: {}, Numéro: {}", contrat.getId(), contrat.getNumeroContrat());

        if (contrat.isCompleted()) {
            log.debug("Contrat {} déjà terminé, ignoré", contrat.getNumeroContrat());
            return null;
        }

        if (contrat.getEcheances() == null || contrat.getEcheances().isEmpty()) {
            log.debug("Contrat {} sans échéances, ignoré", contrat.getNumeroContrat());
            return null;
        }

        try {
            boolean allPaid = contrat.getEcheances().stream()
                    .allMatch(echeance -> echeance.getStatutPaiement() == StatutEcheanceLeasing.PAYE);

            if (allPaid) {
                log.warn(" ALERTE: Contrat {} nécessite une main levée - Toutes les échéances payées", contrat.getNumeroContrat());

                String notificationTitle = "Main Levée Requise - Contrat de Leasing";
                String notificationContent = buildNotificationContent(contrat);
                NotificationType notificationType = NotificationType.ALERT;

                return new ProcessedContratLeasing(
                        contrat,
                        notificationTitle,
                        notificationContent,
                        notificationType,
                        false
                );
            }

            log.debug("Contrat {} : Échéances non toutes payées", contrat.getNumeroContrat());
            return null;

        } catch (Exception e) {
            log.error("Error processing ContratLeasing ID: {}", contrat.getId(), e);
            throw new ProcessingException("Failed to process contrat " + contrat.getId(), e);
        }
    }

    private String buildNotificationContent(ContratLeasing contrat) {


        return String.format(
                "Une action est requise concernant le contrat N° %s de l'organisme %s. " +
                        "Toutes les échéances de ce contrat ont été intégralement payées. " +
                        "Nous vous prions de bien vouloir obtenir la main levée auprès de l'organisme " +
                        "et de procéder à la clôture administrative du contrat dans le système.",
                contrat.getNumeroContrat(),
                getOrganismeName(contrat)
        );
    }

    private String getOrganismeName(ContratLeasing contrat) {
        return contrat.getContratCadre() != null
                ? contrat.getContratCadre().getOrganisme().getNom()
                : "N/A";
    }
}