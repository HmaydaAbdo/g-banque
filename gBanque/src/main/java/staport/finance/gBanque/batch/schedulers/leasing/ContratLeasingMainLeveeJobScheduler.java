package staport.finance.gBanque.batch.schedulers.leasing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler for Contrat Leasing Main Levée Job.
 * This scheduler manages the execution of the main levée verification job
 * which identifies leasing contracts requiring a "main levée" (lien release).
 * Features:
 * - Prevents concurrent executions
 * - Configurable scheduling via CRON expressions
 * - Manual trigger capability for testing/maintenance
 * - Comprehensive error handling and logging
 * - Scheduled to avoid conflicts with Functional Lines jobs
 * Default Schedule: Every Sunday at 2:00 PM (2 hours after Functional Lines jobs)
 *
 * @author Gbanque Development Team
 * @version 1.0
 * @since 2025-12-19
 */
@Service
public class ContratLeasingMainLeveeJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(ContratLeasingMainLeveeJobScheduler.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JobLauncher jobLauncher;
    private final Job contratLeasingMainLeveeJob;

    // Flag to prevent concurrent executions
    private final AtomicBoolean isJobRunning = new AtomicBoolean(false);

    // Configuration properties
    @Value("${leasing.main-levee.job.cron:0 0 14 ? * SUN}")
    private String jobCron;

    @Value("${leasing.main-levee.job.enabled:true}")
    private boolean jobEnabled;

    /**
     * Constructor with dependency injection.
     *
     * @param jobLauncher Spring Batch JobLauncher
     * @param contratLeasingMainLeveeJob Main levée verification job
     */
    public ContratLeasingMainLeveeJobScheduler(JobLauncher jobLauncher, @Qualifier("contratLeasingMainLeveeJob") Job contratLeasingMainLeveeJob) {
        this.jobLauncher = jobLauncher;
        this.contratLeasingMainLeveeJob = contratLeasingMainLeveeJob;
    }



    /**
     * Scheduled execution of the main levée verification job.
     * Default: Every Sunday at 2:00 PM
     * CRON: "0 0 14 ? * SUN"
     * Configuration property: leasing.main-levee.job.cron
     * Schedule rationale:
     * - Runs 2 hours after Functional Lines jobs (12:00 PM)
     * - Avoids database lock conflicts
     * - Allows time for any database maintenance between jobs
     */
    @Scheduled(cron = "${leasing.main-levee.job.cron:0 0 14 ? * SUN}")
    public void runScheduledJob() {
        if (!jobEnabled) {
            log.info("Contrat Leasing Main Levée Job Scheduler is disabled in configuration. Skipping scheduled execution.");
            return;
        }

        log.info("================================================================================");
        log.info("SCHEDULED EXECUTION TRIGGERED: Contrat Leasing Main Levée Job");
        log.info("Trigger Time: {}", LocalDateTime.now().format(DATE_FORMATTER));
        log.info("================================================================================");

        JobExecution jobExecution = runJobInternal("SCHEDULED");

        if (jobExecution != null) {
            log.info("✓ Scheduled job completed with status: {}", jobExecution.getStatus());
        } else {
            log.error("✗ Scheduled job could not be started.");
        }

        log.info("================================================================================");
        log.info("Scheduled execution completed at {}", LocalDateTime.now().format(DATE_FORMATTER));
        log.info("================================================================================");
    }

    /**
     * Manual trigger for the main levée verification job.
     * Useful for testing, maintenance, or on-demand executions.
     *
     * @return JobExecution result, or null if job couldn't be started
     */
    public JobExecution runJobManually() {

        if (!jobEnabled) {
            log.warn("Cannot run job manually - scheduler is disabled in configuration.");
            log.warn("Enable the job by setting 'leasing.main-levee.job.enabled=true' in application properties.");
            return null;
        }

      return runJobInternal("MANUAL");
    }

    /**
     * Internal method to execute the job with comprehensive error handling.
     *
     * @param triggerType Type of trigger (SCHEDULED, MANUAL, etc.)
     * @return JobExecution result, or null if job couldn't be started
     */
    private JobExecution runJobInternal(String triggerType) {
        // Prevent concurrent executions
        if (!isJobRunning.compareAndSet(false, true)) {
            log.warn("⚠ Contrat Leasing Main Levée Job is already running. Skipping this {} execution.", triggerType);
            log.warn("If you believe the job is stuck, check the database for hanging transactions.");
            return null;
        }

        JobExecution jobExecution = null;
        long startTime = System.currentTimeMillis();

        try {
            log.info("▶ Starting Contrat Leasing Main Levée Job (Trigger: {})...", triggerType);

            // Generate unique JobParameters to ensure a new JobInstance for each run
            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("runDate", new Date())
                    .addLong("runTime", System.currentTimeMillis())
                    .addString("triggerType", triggerType)
                    .addString("jobName", contratLeasingMainLeveeJob.getName())
                    .toJobParameters();

            // Launch the job
            jobExecution = jobLauncher.run(contratLeasingMainLeveeJob, jobParameters);
            long duration = System.currentTimeMillis() - startTime;

            // Log launch result
            log.info("✓ Contrat Leasing Main Levée Job launched successfully");
            log.info("  Job Execution ID: {}", jobExecution.getId());
            log.info("  Job Instance ID: {}", jobExecution.getJobInstance().getInstanceId());
            log.info("  Job Status: {}", jobExecution.getStatus());
            log.info("  Launch Duration: {} ms", duration);

            return jobExecution;

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("⚠ Contrat Leasing Main Levée Job is already running (caught by Spring Batch).");
            log.warn("This should not happen due to our AtomicBoolean lock mechanism.");
            log.warn("Possible cause: Multiple scheduler instances or distributed environment.");
            log.warn("Solution: Ensure only one application instance runs the scheduler, or use distributed locks.");
            return null;

        } catch (JobRestartException e) {
            log.error("✗ Failed to restart Contrat Leasing Main Levée Job", e);
            log.error("This typically indicates a configuration issue with job restartability.");
            log.error("Verify that the job is configured to allow restarts.");
            log.error("Check if previous job instance completed with STOPPED status.");
            return null;

        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("✗ Contrat Leasing Main Levée Job instance is already complete", e);
            log.error("This suggests an issue with unique job parameters generation.");
            log.error("Current timestamp: {}, Job name: {}", System.currentTimeMillis(), contratLeasingMainLeveeJob.getName());
            log.error("Verify JobParameters uniqueness in runJobInternal method.");
            return null;

        } catch (JobParametersInvalidException e) {
            log.error("✗ Invalid job parameters for Contrat Leasing Main Levée Job", e);
            log.error("Please verify job configuration and parameter validators.");
            log.error("Check if any custom validators are rejecting the provided parameters.");
            return null;

        } catch (Exception e) {
            log.error("✗ Unexpected error occurred while launching Contrat Leasing Main Levée Job", e);
            log.error("Error Type: {}", e.getClass().getSimpleName());
            log.error("Error Message: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Full stack trace:", e);
            }
            return null;

        } finally {
            // Always release the lock, even if an exception occurred
            isJobRunning.set(false);

            long totalDuration = System.currentTimeMillis() - startTime;

            if (jobExecution != null) {
                log.info("■ Contrat Leasing Main Levée Job execution finished.");
                log.info("  Final Status: {}", jobExecution.getStatus());
                log.info("  Exit Code: {}", jobExecution.getExitStatus().getExitCode());
                log.info("  Total Time: {} ms ({} seconds)", totalDuration, totalDuration / 1000);

            } else {
                log.error("■ Contrat Leasing Main Levée Job execution failed to start.");
                log.error("  Total Time: {} ms", totalDuration);
            }

            log.info("--------------------------------------------------------------------------------");
        }
    }



}