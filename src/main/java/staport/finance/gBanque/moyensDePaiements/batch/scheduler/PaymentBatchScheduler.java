package staport.finance.gBanque.moyensDePaiements.batch.scheduler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Scheduler pour lancer automatiquement le job de synchronisation des paiements.
 * Le job s'exécute deux fois par jour (13h et 20h) du lundi au vendredi uniquement.
 */
@Component
public class PaymentBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentBatchScheduler.class);
    private static final DateTimeFormatter JOB_PARAM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final JobLauncher jobLauncher;
    private final Job importPaymentsJob;

    public PaymentBatchScheduler(JobLauncher jobLauncher,
                                 @Qualifier("importPaymentsJob") Job importPaymentsJob) {
        this.jobLauncher = jobLauncher;
        this.importPaymentsJob = importPaymentsJob;
    }

    /**
     * Exécution programmée à 13h00 du lundi au vendredi.
     * Expression cron: "0 0 13 * * MON-FRI"
     * - Seconde: 0
     * - Minute: 0
     * - Heure: 13
     * - Jour du mois: * (tous les jours)
     * - Mois: * (tous les mois)
     * - Jour de la semaine: MON-FRI (lundi à vendredi)
     */
    @Scheduled(cron = "0 0 13 * * MON-FRI", zone = "Africa/Casablanca")
    public void runPaymentImportJobAt1PM() {
        log.info("=== Déclenchement programmé du job de synchronisation des paiements à 13h00 ===");
        executeJob("13h00");
    }

    /**
     * Exécution programmée à 20h00 du lundi au vendredi.
     * Expression cron: "0 0 20 * * MON-FRI"
     * - Seconde: 0
     * - Minute: 0
     * - Heure: 20
     * - Jour du mois: * (tous les jours)
     * - Mois: * (tous les mois)
     * - Jour de la semaine: MON-FRI (lundi à vendredi)
     */
    @Scheduled(cron = "0 0 20 * * MON-FRI", zone = "Africa/Casablanca")
    public void runPaymentImportJobAt8PM() {
        log.info("=== Déclenchement programmé du job de synchronisation des paiements à 20h00 ===");
        executeJob("20h00");
    }

    /**
     * Méthode commune pour exécuter le job avec vérification du jour de la semaine.
     *
     * @param scheduledTime L'heure programmée pour les logs
     */
    private void executeJob(String scheduledTime) {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        // Double vérification : bien que le cron exclue les weekends,
        // on vérifie par sécurité
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            log.warn("Tentative d'exécution du job un weekend ({} à {}). Job annulé.", dayOfWeek, scheduledTime);
            return;
        }

        log.info("Jour de la semaine: {} - Heure programmée: {}", dayOfWeek, scheduledTime);

        try {
            // Création de paramètres uniques pour chaque exécution
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("launchTime", OffsetDateTime.now().format(JOB_PARAM_DATE_FORMATTER))
                    .addString("scheduledTime", scheduledTime)
                    .addString("executionDate", now.toLocalDate().toString())
                    .toJobParameters();

            log.info("Lancement du job 'importPaymentsJob' avec les paramètres: {}", jobParameters);

            JobExecution jobExecution = jobLauncher.run(importPaymentsJob, jobParameters);

            log.info("Job 'importPaymentsJob' lancé avec succès.");
            log.info("ID de l'exécution: {}", jobExecution.getId());
            log.info("Statut: {}", jobExecution.getStatus());
            log.info("Heure de début: {}", jobExecution.getStartTime());

        } catch (Exception e) {
            log.error("Erreur lors du lancement programmé du job 'importPaymentsJob' à {}: {}",
                    scheduledTime, e.getMessage(), e);
            // Le job continuera à s'exécuter aux prochaines heures programmées
        }
    }
}