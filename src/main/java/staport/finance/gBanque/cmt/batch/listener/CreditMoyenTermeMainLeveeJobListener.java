package staport.finance.gBanque.cmt.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.emails.EmailService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class CreditMoyenTermeMainLeveeJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(CreditMoyenTermeMainLeveeJobListener.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EmailService emailService;

    @Value("${spring.mail.notification.batch-job.recipients}")
    private String notificationEmail;

    @Value("${spring.mail.notification.batch-job.enabled:true}")
    private boolean emailNotificationEnabled;

    @Autowired
    public CreditMoyenTermeMainLeveeJobListener(EmailService emailService) {
        this.emailService = emailService;
        log.info("CreditMoyenTermeMainLeveeJobListener initialisé avec succès");
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("================================================================================");
        log.info("DÉMARRAGE DU JOB BATCH: {}", jobExecution.getJobInstance().getJobName());
        log.info("ID de l'instance du job: {}", jobExecution.getJobInstance().getInstanceId());
        log.info("ID d'exécution du job: {}", jobExecution.getJobId());
        log.info("Heure de début: {}", formatDateTime(jobExecution.getStartTime()));
        log.info("Paramètres du job: {}", jobExecution.getJobParameters());
        log.info("================================================================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        BatchStatus status = jobExecution.getStatus();
        Duration duration = calculateJobDuration(jobExecution);

        logJobCompletion(jobExecution, status, duration);
        handleJobOutcome(jobExecution, status, duration);

        log.info("================================================================================");
    }

    private Duration calculateJobDuration(JobExecution jobExecution) {
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();

        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime);
        } else {
            log.warn("Impossible de calculer la durée du job - heure de début ou de fin nulle");
            return Duration.ZERO;
        }
    }

    private void logJobCompletion(JobExecution jobExecution, BatchStatus status, Duration duration) {
        log.info("================================================================================");
        log.info("JOB BATCH TERMINÉ: {}", jobExecution.getJobInstance().getJobName());
        log.info("================================================================================");
        log.info("ID d'exécution du job: {}", jobExecution.getJobId());
        log.info("Statut final: {}", status);
        log.info("Code de sortie: {}", jobExecution.getExitStatus().getExitCode());
        log.info("Heure de début: {}", formatDateTime(jobExecution.getStartTime()));
        log.info("Heure de fin: {}", formatDateTime(jobExecution.getEndTime()));
        log.info("Durée totale: {} secondes ({} minutes)",
                duration.getSeconds(),
                String.format("%.2f", duration.getSeconds() / 60.0));
        log.info("Total d'éléments traités: {}", getTotalItemsProcessed(jobExecution));

        if (!jobExecution.getExitStatus().getExitDescription().isEmpty()) {
            log.info("Description de sortie: {}", jobExecution.getExitStatus().getExitDescription());
        }
    }

    private void handleJobOutcome(JobExecution jobExecution, BatchStatus status, Duration duration) {
        if (status == BatchStatus.FAILED || status == BatchStatus.STOPPED) {
            log.error("❌ Le job '{}' a ÉCHOUÉ avec le statut: {}. Démarrage de la notification d'échec.",
                    jobExecution.getJobInstance().getJobName(), status);
            sendFailureNotification(jobExecution, duration);
        } else if (status == BatchStatus.COMPLETED) {
            long totalProcessed = getTotalItemsProcessed(jobExecution);
            log.info("✅ Le job '{}' s'est terminé avec SUCCÈS. Total d'éléments traités: {}",
                    jobExecution.getJobInstance().getJobName(), totalProcessed);
        } else {
            log.warn("⚠️ Le job '{}' s'est terminé avec un statut inattendu: {}",
                    jobExecution.getJobInstance().getJobName(), status);
        }
    }

    private void sendFailureNotification(JobExecution jobExecution, Duration duration) {
        if (!emailNotificationEnabled) {
            log.info("Les notifications par email sont désactivées dans la configuration. Notification d'échec ignorée.");
            return;
        }

        if (notificationEmail == null || notificationEmail.trim().isEmpty()) {
            log.warn("L'adresse email de notification n'est pas configurée. Impossible d'envoyer la notification d'échec.");
            return;
        }

        try {
            log.info("Préparation de l'email de notification d'échec pour le job: {}",
                    jobExecution.getJobInstance().getJobName());

            String subject = String.format("[ÉCHEC CRITIQUE] %s - %s",
                    jobExecution.getJobInstance().getJobName(),
                    formatDateTime(jobExecution.getEndTime()));

            List<Map<String, Object>> failureDetails = extractFailureDetails(jobExecution);
            String exitDescription = jobExecution.getExitStatus().getExitDescription();

            String[] emailAddresses = notificationEmail.split(",");
            int successCount = 0;
            int failureCount = 0;

            log.info("Envoi des notifications d'échec à {} destinataire(s)", emailAddresses.length);

            for (String email : emailAddresses) {
                String trimmedEmail = email.trim();

                if (trimmedEmail.isEmpty()) {
                    continue;
                }

                try {
                    emailService.sendBatchJobFailureNotification(
                            trimmedEmail,
                            subject,
                            jobExecution.getJobInstance().getJobName(),
                            jobExecution.getJobId(),
                            jobExecution.getStatus().toString(),
                            formatDateTime(jobExecution.getStartTime()),
                            formatDateTime(jobExecution.getEndTime()),
                            duration.getSeconds(),
                            buildStepStatistics(jobExecution),
                            failureDetails,
                            exitDescription
                    );

                    log.info("✅ Email de notification d'échec envoyé avec succès à: {}", trimmedEmail);
                    successCount++;

                } catch (Exception e) {
                    log.error("❌ Échec de l'envoi de l'email de notification à: {}. Erreur: {}",
                            trimmedEmail, e.getMessage(), e);
                    failureCount++;
                }
            }

            if (successCount > 0) {
                log.info("✅ {} email(s) de notification d'échec envoyé(s) avec succès", successCount);
            }
            if (failureCount > 0) {
                log.warn("⚠️ Échec de l'envoi de {} email(s) de notification", failureCount);
            }

        } catch (Exception e) {
            log.error("❌ Erreur critique lors du traitement des emails de notification d'échec. Erreur: {}",
                    e.getMessage(), e);
        }
    }

    private Map<String, Map<String, Object>> buildStepStatistics(JobExecution jobExecution) {
        Map<String, Map<String, Object>> stepStatistics = new LinkedHashMap<>();

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("Statut", stepExecution.getStatus().toString());
            stats.put("Lignes lues", stepExecution.getReadCount());
            stats.put("Lignes traitées (écrites)", stepExecution.getWriteCount());
            stats.put("Lignes filtrées", stepExecution.getFilterCount());
            stats.put("Commits réussis", stepExecution.getCommitCount());
            stats.put("Rollbacks", stepExecution.getRollbackCount());
            stats.put("Erreurs de lecture", stepExecution.getReadSkipCount());
            stats.put("Erreurs de traitement", stepExecution.getProcessSkipCount());
            stats.put("Erreurs d'écriture", stepExecution.getWriteSkipCount());
            stats.put("Durée (secondes)", Duration.between(
                    Objects.requireNonNull(stepExecution.getStartTime()),
                    stepExecution.getEndTime()).getSeconds());

            stepStatistics.put(stepExecution.getStepName(), stats);
        }

        return stepStatistics;
    }

    private List<Map<String, Object>> extractFailureDetails(JobExecution jobExecution) {
        List<Map<String, Object>> failureDetails = new ArrayList<>();

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if (stepExecution.getStatus() == BatchStatus.FAILED ||
                    !stepExecution.getFailureExceptions().isEmpty()) {

                for (Throwable throwable : stepExecution.getFailureExceptions()) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("numero", stepExecution.getStepName());
                    detail.put("reason", String.format("%s: %s",
                            throwable.getClass().getSimpleName(),
                            throwable.getMessage() != null ? throwable.getMessage() : "Aucun message d'erreur disponible"));
                    failureDetails.add(detail);
                }
            }
        }

        if (failureDetails.isEmpty() &&
                (jobExecution.getStatus() == BatchStatus.FAILED ||
                        jobExecution.getStatus() == BatchStatus.STOPPED)) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("numero", "Exécution Générale du Job");
            detail.put("reason", jobExecution.getExitStatus().getExitDescription());
            failureDetails.add(detail);
        }

        return failureDetails;
    }

    private long getTotalItemsProcessed(JobExecution jobExecution) {
        return jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "N/A";
    }
}