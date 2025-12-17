package staport.finance.gBanque.batchJobs.generalFunctionalLines.listeners;

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

/**
 * Professional JobExecutionListener for the Functional Line Expiration Job.
 * This listener provides comprehensive monitoring capabilities including:
 * - Detailed logging of job execution lifecycle
 * - Automatic email notifications on job failures
 * - Statistical analysis of job performance
 * - Step-level execution metrics
 * The listener follows enterprise best practices for batch job monitoring
 * and alerting, ensuring that technical teams are promptly notified of any
 * issues during automated job executions.
 *
 * @author Gbanque Development Team
 * @version 1.0
 * @since 2025-12-17
 */
@Component
public class FunctionalLineJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(FunctionalLineJobListener.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EmailService emailService;

    @Value("${spring.mail.notification.batch-job.recipients}")
    private String notificationEmail;

    @Value("${spring.mail.notification.batch-job.enabled:true}")
    private boolean emailNotificationEnabled;



    /**
     * Constructs a FunctionalLineJobListener with required dependencies.
     *
     * @param emailService The email service for sending notifications.
     */
    @Autowired
    public FunctionalLineJobListener(EmailService emailService) {
        this.emailService = emailService;
        log.info("FunctionalLineJobListener initialized successfully.");
    }

    /**
     * Invoked before the job starts execution.
     * Logs comprehensive job startup information for audit and debugging purposes.
     *
     * @param jobExecution The job execution context.
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("================================================================================");
        log.info("STARTING BATCH JOB: {}", jobExecution.getJobInstance().getJobName());
        log.info("Job Instance ID: {}", jobExecution.getJobInstance().getInstanceId());
        log.info("Job Execution ID: {}", jobExecution.getJobId());
        log.info("Start Time: {}", formatDateTime(jobExecution.getStartTime()));
        log.info("Job Parameters: {}", jobExecution.getJobParameters());
        log.info("================================================================================");
    }

    /**
     * Invoked after the job completes (success, failure, or stopped).
     * Performs comprehensive post-execution analysis including:
     * - Logging execution statistics
     * - Calculating job duration
     * - Analyzing step-level metrics
     * - Sending email notifications on failures
     *
     * @param jobExecution The job execution context with complete results.
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        BatchStatus status = jobExecution.getStatus();

        // Calculate job duration safely
        Duration duration = calculateJobDuration(jobExecution);

        // Log comprehensive job completion information
        logJobCompletion(jobExecution, status, duration);

        // Log detailed step-level statistics
        logStepStatistics(jobExecution);

        // Handle job outcome appropriately
        handleJobOutcome(jobExecution, status, duration);

        log.info("================================================================================");
    }

    /**
     * Calculates the duration of job execution safely handling null dates.
     *
     * @param jobExecution The job execution context.
     * @return The duration of the job execution.
     */
    private Duration calculateJobDuration(JobExecution jobExecution) {
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();

        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime);
        } else {
            log.warn("Unable to calculate job duration - start or end time is null");
            return Duration.ZERO;
        }
    }

    /**
     * Logs comprehensive job completion information.
     *
     * @param jobExecution The job execution context.
     * @param status The final job status.
     * @param duration The job duration.
     */
    private void logJobCompletion(JobExecution jobExecution, BatchStatus status, Duration duration) {
        log.info("================================================================================");
        log.info("BATCH JOB COMPLETED: {}", jobExecution.getJobInstance().getJobName());
        log.info("================================================================================");
        log.info("Job Execution ID: {}", jobExecution.getJobId());
        log.info("Final Status: {}", status);
        log.info("Exit Code: {}", jobExecution.getExitStatus().getExitCode());
        log.info("Start Time: {}", formatDateTime(jobExecution.getStartTime()));
        log.info("End Time: {}", formatDateTime(jobExecution.getEndTime()));
        log.info("Total Duration: {} seconds ({} minutes)",
                duration.getSeconds(),
                String.format("%.2f", duration.getSeconds() / 60.0));
        log.info("Total Items Processed: {}", getTotalItemsProcessed(jobExecution));

        if (jobExecution.getExitStatus().getExitDescription() != null &&
                !jobExecution.getExitStatus().getExitDescription().isEmpty()) {
            log.info("Exit Description:     {}", jobExecution.getExitStatus().getExitDescription());
        }
    }

    /**
     * Logs detailed statistics for each step in the job execution.
     *
     * @param jobExecution The job execution context.
     */
    private void logStepStatistics(JobExecution jobExecution) {
        log.info("--------------------------------------------------------------------------------");
        log.info("STEP-LEVEL EXECUTION STATISTICS");
        log.info("--------------------------------------------------------------------------------");

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            log.info("Step Name: '{}'", stepExecution.getStepName());
            log.info("  Status: {}", stepExecution.getStatus());
            log.info("  Read Count: {}", stepExecution.getReadCount());
            log.info("  Write Count: {}", stepExecution.getWriteCount());
            log.info("  Commit Count: {}", stepExecution.getCommitCount());
            log.info("  Rollback Count: {}", stepExecution.getRollbackCount());
            log.info("  Read Skip Count: {}", stepExecution.getReadSkipCount());
            log.info("  Process Skip Count: {}", stepExecution.getProcessSkipCount());
            log.info("  Write Skip Count: {}", stepExecution.getWriteSkipCount());
            log.info("  Filter Count: {}", stepExecution.getFilterCount());
            log.info("  Duration: {} seconds",
                    Duration.between(
                            stepExecution.getStartTime(),
                            stepExecution.getEndTime()
                    ).getSeconds());

            // Log failures if any
            if (stepExecution.getStatus() == BatchStatus.FAILED &&
                    !stepExecution.getFailureExceptions().isEmpty()) {
                log.error("  Failure Exceptions:");
                stepExecution.getFailureExceptions().forEach(throwable ->
                        log.error("    - {}: {}",
                                throwable.getClass().getSimpleName(),
                                throwable.getMessage())
                );
            }

            log.info("  ---");
        }
    }

    /**
     * Handles job outcome based on final status.
     * Sends email notifications for failures and logs success metrics.
     *
     * @param jobExecution The job execution context.
     * @param status The final job status.
     * @param duration The job duration.
     */
    private void handleJobOutcome(JobExecution jobExecution, BatchStatus status, Duration duration) {
        if (status == BatchStatus.FAILED || status == BatchStatus.STOPPED) {
            log.error("❌ Job '{}' FAILED with status: {}. Initiating failure notification.",
                    jobExecution.getJobInstance().getJobName(), status);
            sendFailureNotification(jobExecution, duration);
        } else if (status == BatchStatus.COMPLETED) {
            long totalProcessed = getTotalItemsProcessed(jobExecution);
            log.info("✅ Job '{}' completed SUCCESSFULLY. Total items processed: {}",
                    jobExecution.getJobInstance().getJobName(), totalProcessed);
        } else {
            log.warn("⚠️ Job '{}' completed with unexpected status: {}",
                    jobExecution.getJobInstance().getJobName(), status);
        }
    }

    /**
     * Sends a detailed failure notification email to configured recipients.
     * This method prepares comprehensive failure information including
     * job statistics, step metrics, and error details.
     * Supports multiple recipients separated by commas.
     *
     * @param jobExecution The failed job execution context.
     * @param duration The job execution duration.
     */
    private void sendFailureNotification(JobExecution jobExecution, Duration duration) {
        // Pre-flight checks
        if (!emailNotificationEnabled) {
            log.info("Email notifications are disabled in configuration. Skipping failure notification.");
            return;
        }

        if (notificationEmail == null || notificationEmail.trim().isEmpty()) {
            log.warn("Notification email address is not configured. Cannot send failure notification. " +
                    "Please set 'spring.mail.notification.batch-job.recipients' in application properties.");
            return;
        }

        try {
            log.info("Preparing failure notification email for job: {}",
                    jobExecution.getJobInstance().getJobName());

            // Prepare email subject
            String subject = String.format(
                    "[ÉCHEC CRITIQUE] %s - %s",
                    jobExecution.getJobInstance().getJobName(),
                    formatDateTime(jobExecution.getEndTime())
            );

            // Prepare step statistics
            Map<String, Map<String, Object>> stepStatistics = buildStepStatistics(jobExecution);

            // Prepare failure details
            List<Map<String, Object>> failureDetails = extractFailureDetails(jobExecution);

            // Get exit description
            String exitDescription = jobExecution.getExitStatus().getExitDescription();

            // ✅ NOUVEAU : Support pour plusieurs destinataires séparés par des virgules
            String[] emailAddresses = notificationEmail.split(",");
            int successCount = 0;
            int failureCount = 0;

            log.info("Sending failure notifications to {} recipient(s)", emailAddresses.length);

            for (String email : emailAddresses) {
                String trimmedEmail = email.trim();

                // Skip empty emails (caused by trailing commas or double commas)
                if (trimmedEmail.isEmpty()) {
                    continue;
                }

                try {
                    // Send the email to this recipient
                    emailService.sendBatchJobFailureNotification(
                            trimmedEmail,
                            subject,
                            jobExecution.getJobInstance().getJobName(),
                            jobExecution.getJobId(),
                            jobExecution.getStatus().toString(),
                            formatDateTime(jobExecution.getStartTime()),
                            formatDateTime(jobExecution.getEndTime()),
                            duration.getSeconds(),
                            stepStatistics,
                            failureDetails,
                            exitDescription
                    );

                    log.info("✉️ Failure notification email sent successfully to: {}", trimmedEmail);
                    successCount++;

                } catch (Exception e) {
                    log.error("❌ Failed to send failure notification email to: {}. Error: {}",
                            trimmedEmail, e.getMessage(), e);
                    failureCount++;
                    // Continue to next recipient even if this one fails
                }
            }

            // Log summary of email sending
            if (successCount > 0) {
                log.info("✅ Successfully sent {} failure notification email(s)", successCount);
            }
            if (failureCount > 0) {
                log.warn("⚠️ Failed to send {} failure notification email(s)", failureCount);
            }

        } catch (Exception e) {
            log.error("❌ Critical error while processing failure notification emails. Error: {}",
                    e.getMessage(), e);
            // Don't re-throw - we don't want email failures to impact job execution recording
        }}

    /**
     * Builds a comprehensive map of step statistics for email reporting.
     *
     * @param jobExecution The job execution context.
     * @return Map of step names to their statistics.
     */
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
            stats.put("Durée (secondes)",
                    Duration.between(stepExecution.getStartTime(), stepExecution.getEndTime()).getSeconds());

            stepStatistics.put(stepExecution.getStepName(), stats);
        }

        return stepStatistics;
    }

    /**
     * Extracts detailed failure information from the job execution.
     * This method collects all exceptions from failed steps and formats
     * them for email reporting.
     *
     * @param jobExecution The job execution context.
     * @return List of failure detail maps.
     */
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
                            throwable.getMessage() != null ? throwable.getMessage() : "No error message available"));
                    failureDetails.add(detail);
                }
            }
        }

        // If no specific step failures but job failed, add general failure
        if (failureDetails.isEmpty() &&
                (jobExecution.getStatus() == BatchStatus.FAILED ||
                        jobExecution.getStatus() == BatchStatus.STOPPED)) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("numero", "Exécution Générale du Job");
            detail.put("reason",
                    jobExecution.getExitStatus().getExitDescription() != null
                            ? jobExecution.getExitStatus().getExitDescription()
                            : "Le job a échoué sans message d'erreur explicite. Consultez les logs pour plus de détails.");
            failureDetails.add(detail);
        }

        return failureDetails;
    }

    /**
     * Calculates the total number of items successfully processed across all steps.
     *
     * @param jobExecution The job execution context.
     * @return Total number of items written (processed successfully).
     */
    private long getTotalItemsProcessed(JobExecution jobExecution) {
        return jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();
    }

    /**
     * Formats a LocalDateTime to a readable French format string.
     *
     * @param dateTime The datetime to format.
     * @return Formatted date string or "N/A" if null.
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "N/A";
    }
}