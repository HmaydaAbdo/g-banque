package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.jobs.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.jobs.dtos.ProcessedSpecificFunctionalLine;
import staport.finance.gBanque.notification.entities.NotificationType;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class SpecificFunctionalLineProcessor implements ItemProcessor<LigneFonctionnelleSpecifique, ProcessedSpecificFunctionalLine> {

    private static final Logger log = LoggerFactory.getLogger(SpecificFunctionalLineProcessor.class);

    private final OffsetDateTime currentProcessingTime = OffsetDateTime.now();

    @Value("${functional.line.expiration.alert.threshold.months}")
    private long alertThresholdMonths;

    @Value("${functional.line.expiration.warning.threshold.months}")
    private long warningThresholdMonths;

    @Override
    public ProcessedSpecificFunctionalLine process(LigneFonctionnelleSpecifique item)   {
        log.debug("Processing LigneFonctionnelleSpecifique ID: {} (Marché : {} Bank: {}, EndDate: {}) ",
                item.getId(),
                item.getMarche() != null ? item.getMarche().getNom() : "N/A",
                item.getMarche().getBanque() != null ? item.getMarche().getBanque().getNom() : "N/A",
                item.getDateFin());

        OffsetDateTime endDate = item.getDateFin();
        String notificationTitle = null;
        String notificationContent = null;
        NotificationType notificationType = null;

        String marcheNom = item.getMarche() != null ? item.getMarche().getNom() : "N/A";
        String banqueNom = item.getMarche().getBanque() != null ? item.getMarche().getBanque().getNom() : "N/A";


        if (endDate.isBefore(currentProcessingTime)) {
            if (item.isActif()) {

                notificationTitle = "La ligne spécifique " + item.getNatureLigneFonctionnelle().getLabel() + " pour le marché " + marcheNom + " a expirée";
                notificationContent = String.format(
                        "La ligne spécifique ID '%d' (Banque: '%s' , Marché: '%s') est expirée le %s. Elle doit être désactivée.",
                        item.getId(),
                        banqueNom,
                        marcheNom,
                        endDate.toLocalDate().toString()
                );
                notificationType = NotificationType.ALERT;
            }
        } else {
            // Rule 2 & 3: Check remaining time for warnings/alerts
            long remainingMonths = ChronoUnit.MONTHS.between(currentProcessingTime.toLocalDate(), endDate.toLocalDate());

            if (remainingMonths <= alertThresholdMonths) {
                // ALERT: very soon
                notificationTitle = "La ligne spécifique " + item.getNatureLigneFonctionnelle().getLabel() + " pour le marché " + marcheNom + " expirera prochainement";
                notificationContent = String.format(
                        "La ligne spécifique ID '%d' (Banque: '%s', Marché: '%s') expirera très bientôt le %s. Il reste environ %d mois. Action URGENTE requise!",
                        item.getId(),
                        banqueNom,
                        marcheNom,
                        endDate.toLocalDate().toString(),
                        remainingMonths
                );
                notificationType = NotificationType.ALERT;
            } else if (remainingMonths <= warningThresholdMonths) {
                // WARNING: soon
                notificationTitle = "La ligne spécifique " + item.getNatureLigneFonctionnelle().getLabel() + " pour le marché " + marcheNom + "  expirera prochainement";
                notificationContent = String.format(
                        "La ligne spécifique ID '%d' (Banque: '%s', Marché: '%s') expirera le %s. Il reste environ %d mois. Veuillez planifier son renouvellement.",
                        item.getId(),
                        banqueNom,
                        marcheNom,
                        endDate.toLocalDate().toString(),
                        remainingMonths
                );
                notificationType = NotificationType.WARNING;
            }
        }

        // Note: We don't need to return null because we read only active lines.
        return new ProcessedSpecificFunctionalLine(item, notificationTitle, notificationContent, notificationType);
    }
}