package staport.finance.gBanque.batchJobs.scheduler;

import jakarta.annotation.PostConstruct;
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
 * Unified Scheduler for Functional Line Expiration Jobs.
 * This scheduler centrally manages the execution of both:
 * 1. General Functional Lines Expiration Job (functionalLineExpirationJob)
 * 2. Specific Functional Lines Expiration Job (specificFunctionalLineExpirationJob)
 * Features:
 * - Sequential execution to prevent database lock conflicts
 * - Concurrent execution prevention per job
 * - Configurable scheduling via CRON expressions
 * - Manual trigger capability for testing/maintenance
 * - Comprehensive error handling and logging
 * - Smart abort logic (if general job fails, specific job is skipped)
 * Default Schedule: Every Sunday at 12:00 PM
 * Execution Order: General Job → 60s delay → Specific Job
 * @author Gbanque Development Team
 * @version 1.0
 * @since 2025-12-17
 */
@Service
public class FunctionalLinesJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(FunctionalLinesJobScheduler.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final long INTER_JOB_DELAY_MS = 60000; // 2 seconds between jobs

    private final JobLauncher jobLauncher;
    private final Job functionalLineExpirationJob;
    private final Job specificFunctionalLineExpirationJob;

    // Flags to prevent concurrent executions
    private final AtomicBoolean isGeneralJobRunning = new AtomicBoolean(false);
    private final AtomicBoolean isSpecificJobRunning = new AtomicBoolean(false);

    // Configuration properties
    @Value("${functional.line.jobs.cron:0 0 12 ? * SUN}")
    private String jobsCron;

    @Value("${functional.line.jobs.enabled:true}")
    private boolean jobsEnabled;

    @Value("${functional.line.job.general.enabled:true}")
    private boolean generalJobEnabled;

    @Value("${functional.line.job.specific.enabled:true}")
    private boolean specificJobEnabled;

    @Value("${functional.line.jobs.sequential:true}")
    private boolean runSequentially;

    @Value("${functional.line.jobs.abort-on-general-failure:true}")
    private boolean abortOnGeneralFailure;

    /**
     * Constructor with dependency injection.
     * Uses @Qualifier to distinguish between the two job beans.
     *
     * @param jobLauncher Spring Batch JobLauncher
     * @param functionalLineExpirationJob General functional lines job
     * @param specificFunctionalLineExpirationJob Specific functional lines job
     */
    public FunctionalLinesJobScheduler(
            JobLauncher jobLauncher,
            @Qualifier("functionalLineExpirationJob") Job functionalLineExpirationJob,
            @Qualifier("specificFunctionalLineExpirationJob") Job specificFunctionalLineExpirationJob) {
        this.jobLauncher = jobLauncher;
        this.functionalLineExpirationJob = functionalLineExpirationJob;
        this.specificFunctionalLineExpirationJob = specificFunctionalLineExpirationJob;
    }

    /**
     * Logs the scheduler configuration after bean initialization.
     * Useful for verifying configuration at application startup.
     */
    @PostConstruct
    public void logConfiguration() {
        log.info("================================================================================");
        log.info("Functional Lines Jobs Scheduler Configuration:");
        log.info("  - Scheduler Enabled: {}", jobsEnabled);
        log.info("  - CRON Expression: {}", jobsCron);
        log.info("  - General Job Enabled: {}", generalJobEnabled);
        log.info("  - Specific Job Enabled: {}", specificJobEnabled);
        log.info("  - Sequential Execution: {}", runSequentially);
        log.info("  - Abort on General Failure: {}", abortOnGeneralFailure);
        log.info("  - Inter-Job Delay: {} ms", INTER_JOB_DELAY_MS);
        log.info("  - Next Scheduled Run: Every Sunday at 12:00 PM (unless configured otherwise)");
        log.info("================================================================================");
    }

    /**
     * Scheduled execution of both functional line expiration jobs.
     * Default: Every Sunday at 12:00 PM
     * CRON: "0 0 12 ? * SUN"
     * Configuration property: functional.line.jobs.cron
     * Execution order when sequential mode is enabled (default):
     * 1. General Functional Lines Job
     * 2. 2-second delay
     * 3. Specific Functional Lines Job (only if general job succeeded or abortOnGeneralFailure=false)
     */
    @Scheduled(cron = "${functional.line.jobs.cron:0 0 12 ? * SUN}")
    public void runScheduledJobs() {
        if (!jobsEnabled) {
            log.info("Functional Lines Jobs Scheduler is disabled in configuration. Skipping scheduled execution.");
            return;
        }

        log.info("================================================================================");
        log.info("SCHEDULED EXECUTION TRIGGERED");
        log.info("Trigger Time: {}", LocalDateTime.now().format(DATE_FORMATTER));
        log.info("Sequential Mode: {}", runSequentially);
        log.info("================================================================================");

        if (runSequentially) {
            runAllJobsSequentially("SCHEDULED");
        } else {
            log.warn("Non-sequential mode is not recommended and may cause database conflicts.");
            runJobsIndependently("SCHEDULED");
        }

        log.info("================================================================================");
        log.info("Scheduled execution completed at {}", LocalDateTime.now().format(DATE_FORMATTER));
        log.info("================================================================================");
    }

    /**
     * Runs both jobs sequentially (recommended approach).
     * This ensures no database lock conflicts and consistent processing order.
     *
     * @param triggerType Type of trigger (SCHEDULED, MANUAL, etc.)
     */
    private void runAllJobsSequentially(String triggerType) {
        log.info("Running jobs in SEQUENTIAL mode (Trigger: {})...", triggerType);

        JobExecution generalExecution = null;
        JobExecution specificExecution = null;

        // Step 1: Execute General Functional Lines Job
        if (generalJobEnabled) {
            log.info("→ Step 1/2: Executing General Functional Lines Job...");
            generalExecution = runJobInternal(
                    functionalLineExpirationJob,
                    "General Functional Lines Job",
                    isGeneralJobRunning,
                    triggerType + "_SEQUENTIAL_1"
            );

            if (generalExecution != null) {
                log.info("✓ General job completed with status: {}", generalExecution.getStatus());
            } else {
                log.error("✗ General job could not be started.");
            }

            // Check if we should abort specific job execution
            if (abortOnGeneralFailure && generalExecution != null &&
                    generalExecution.getStatus() == BatchStatus.FAILED) {
                log.error("⚠ General job FAILED. Aborting specific job execution as per configuration.");
                log.error("Set 'functional.line.jobs.abort-on-general-failure=false' to run specific job anyway.");
                return;
            }
        } else {
            log.info("→ Step 1/2: General Functional Lines Job is disabled. Skipping.");
        }

        // Delay between jobs
        if (generalJobEnabled && specificJobEnabled) {
            log.info("⏸ Pausing {} ms between jobs to ensure clean separation...", INTER_JOB_DELAY_MS);
            sleepBetweenJobs();
        }

        // Step 2: Execute Specific Functional Lines Job
        if (specificJobEnabled) {
            log.info("→ Step 2/2: Executing Specific Functional Lines Job...");
            specificExecution = runJobInternal(
                    specificFunctionalLineExpirationJob,
                    "Specific Functional Lines Job",
                    isSpecificJobRunning,
                    triggerType + "_SEQUENTIAL_2"
            );

            if (specificExecution != null) {
                log.info("✓ Specific job completed with status: {}", specificExecution.getStatus());
            } else {
                log.error("✗ Specific job could not be started.");
            }
        } else {
            log.info("→ Step 2/2: Specific Functional Lines Job is disabled. Skipping.");
        }

        // Summary
        logExecutionSummary(generalExecution, specificExecution);
    }

    /**
     * Runs jobs independently (not recommended, kept for flexibility).
     *
     * @param triggerType Type of trigger
     */
    private void runJobsIndependently(String triggerType) {
        log.info("Running jobs in INDEPENDENT mode (Trigger: {})...", triggerType);

        if (generalJobEnabled) {
            runJobInternal(
                    functionalLineExpirationJob,
                    "General Functional Lines Job",
                    isGeneralJobRunning,
                    triggerType + "_INDEPENDENT"
            );
        }

        if (specificJobEnabled) {
            runJobInternal(
                    specificFunctionalLineExpirationJob,
                    "Specific Functional Lines Job",
                    isSpecificJobRunning,
                    triggerType + "_INDEPENDENT"
            );
        }
    }

    /**
     * Manual trigger for general functional lines job only.
     *
     * @return JobExecution result, or null if job couldn't be started
     */
    public JobExecution runGeneralJobManually() {
        if (!generalJobEnabled) {
            log.warn("Cannot run general job manually - job is disabled in configuration.");
            return null;
        }

        log.info("================================================================================");
        log.info("MANUAL EXECUTION TRIGGERED: General Functional Lines Job");
        log.info("Trigger Time: {}", LocalDateTime.now().format(DATE_FORMATTER));
        log.info("================================================================================");

        return runJobInternal(
                functionalLineExpirationJob,
                "General Functional Lines Job",
                isGeneralJobRunning,
                "MANUAL"
        );
    }

    /**
     * Manual trigger for specific functional lines job only.
     *
     * @return JobExecution result, or null if job couldn't be started
     */
    public JobExecution runSpecificJobManually() {
        if (!specificJobEnabled) {
            log.warn("Cannot run specific job manually - job is disabled in configuration.");
            return null;
        }

        log.info("================================================================================");
        log.info("MANUAL EXECUTION TRIGGERED: Specific Functional Lines Job");
        log.info("Trigger Time: {}", LocalDateTime.now().format(DATE_FORMATTER));
        log.info("================================================================================");

        return runJobInternal(
                specificFunctionalLineExpirationJob,
                "Specific Functional Lines Job",
                isSpecificJobRunning,
                "MANUAL"
        );
    }

    /**
     * Manual trigger for both jobs sequentially.
     * Follows the same logic as scheduled execution.
     */
    public void runAllJobsManually() {
        if (!jobsEnabled) {
            log.warn("Cannot run jobs manually - scheduler is disabled in configuration.");
            return;
        }

        log.info("================================================================================");
        log.info("MANUAL EXECUTION TRIGGERED: ALL JOBS");
        log.info("Trigger Time: {}", LocalDateTime.now().format(DATE_FORMATTER));
        log.info("================================================================================");

        runAllJobsSequentially("MANUAL");

        log.info("================================================================================");
    }

    /**
     * Internal method to execute a job with comprehensive error handling.
     *
     * @param jobToLaunch The Spring Batch Job to execute
     * @param jobDisplayName Human-readable job name for logging
     * @param runningFlag Atomic flag to prevent concurrent executions of this specific job
     * @param triggerType Type of trigger (SCHEDULED, MANUAL, etc.)
     * @return JobExecution result, or null if job couldn't be started
     */
    private JobExecution runJobInternal(Job jobToLaunch, String jobDisplayName,
                                        AtomicBoolean runningFlag, String triggerType) {
        // Prevent concurrent executions of the same job
        if (!runningFlag.compareAndSet(false, true)) {
            log.warn("⚠ {} is already running. Skipping this {} execution.", jobDisplayName, triggerType);
            return null;
        }

        JobExecution jobExecution = null;
        long startTime = System.currentTimeMillis();

        try {
            log.info("▶ Starting {} (Trigger: {})...", jobDisplayName, triggerType);

            // Generate unique JobParameters to ensure a new JobInstance for each run
            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("runDate", new Date())
                    .addLong("runTime", System.currentTimeMillis())
                    .addString("triggerType", triggerType)
                    .addString("jobName", jobToLaunch.getName())
                    .toJobParameters();

            // Launch the job
            jobExecution = jobLauncher.run(jobToLaunch, jobParameters);
            long duration = System.currentTimeMillis() - startTime;

            // Log launch result
            log.info("✓ {} launched successfully", jobDisplayName);
            log.info("  Job Execution ID: {}", jobExecution.getId());
            log.info("  Job Status: {}", jobExecution.getStatus());
            log.info("  Launch Duration: {} ms", duration);

            return jobExecution;

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("⚠ {} is already running (caught by Spring Batch).", jobDisplayName);
            log.warn("This should not happen due to our AtomicBoolean lock mechanism.");
            log.warn("Possible cause: Multiple scheduler instances or distributed environment.");
            return null;

        } catch (JobRestartException e) {
            log.error("✗ Failed to restart {}", jobDisplayName, e);
            log.error("This typically indicates a configuration issue with job restartability.");
            log.error("Verify that the job is configured to allow restarts.");
            return null;

        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("✗ {} instance is already complete", jobDisplayName, e);
            log.error("This suggests an issue with unique job parameters generation.");
            log.error("Current timestamp: {}, Job name: {}", System.currentTimeMillis(), jobToLaunch.getName());
            log.error("Verify JobParameters uniqueness in runJobInternal method.");
            return null;

        } catch (JobParametersInvalidException e) {
            log.error("✗ Invalid job parameters for {}", jobDisplayName, e);
            log.error("Please verify job configuration and parameter validators.");
            return null;

        } catch (Exception e) {
            log.error("✗ Unexpected error occurred while launching {}", jobDisplayName, e);
            log.error("Error Type: {}", e.getClass().getSimpleName());
            log.error("Error Message: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Full stack trace:", e);
            }
            return null;

        } finally {
            // Always release the lock, even if an exception occurred
            runningFlag.set(false);

            long totalDuration = System.currentTimeMillis() - startTime;

            if (jobExecution != null) {
                log.info("■ {} execution finished. Final Status: {}, Total Time: {} ms",
                        jobDisplayName, jobExecution.getStatus(), totalDuration);
            } else {
                log.error("■ {} execution failed to start. Total Time: {} ms",
                        jobDisplayName, totalDuration);
            }

            log.info("--------------------------------------------------------------------------------");
        }
    }

    /**
     * Sleeps between job executions to ensure clean separation.
     */
    private void sleepBetweenJobs() {
        try {
            Thread.sleep(INTER_JOB_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted during inter-job delay: {}", e.getMessage());
        }
    }

    /**
     * Logs a summary of both job executions.
     */
    private void logExecutionSummary(JobExecution generalExecution, JobExecution specificExecution) {
        log.info("════════════════════════════════════════════════════════════════════════════════");
        log.info("EXECUTION SUMMARY");
        log.info("════════════════════════════════════════════════════════════════════════════════");

        if (generalExecution != null) {
            log.info("General Job:");
            log.info("  Status: {}", generalExecution.getStatus());
            log.info("  Execution ID: {}", generalExecution.getId());
            log.info("  Exit Code: {}", generalExecution.getExitStatus().getExitCode());
        } else {
            log.info("General Job: NOT EXECUTED");
        }

        log.info("--------------------------------------------------------------------------------");

        if (specificExecution != null) {
            log.info("Specific Job:");
            log.info("  Status: {}", specificExecution.getStatus());
            log.info("  Execution ID: {}", specificExecution.getId());
            log.info("  Exit Code: {}", specificExecution.getExitStatus().getExitCode());
        } else {
            log.info("Specific Job: NOT EXECUTED");
        }

        log.info("════════════════════════════════════════════════════════════════════════════════");
    }

    // ========================================================================================
    // Public utility methods for external monitoring and control
    // ========================================================================================

    /**
     * Checks if the general job is currently running.
     */
    public boolean isGeneralJobRunning() {
        return isGeneralJobRunning.get();
    }

    /**
     * Checks if the specific job is currently running.
     */
    public boolean isSpecificJobRunning() {
        return isSpecificJobRunning.get();
    }

    /**
     * Checks if any job is currently running.
     */
    public boolean isAnyJobRunning() {
        return isGeneralJobRunning.get() || isSpecificJobRunning.get();
    }

    /**
     * Gets the configured CRON expression.
     */
    public String getJobsCron() {
        return jobsCron;
    }

    /**
     * Checks if the general job is enabled.
     */
    public boolean isGeneralJobEnabled() {
        return generalJobEnabled;
    }

    /**
     * Checks if the specific job is enabled.
     */
    public boolean isSpecificJobEnabled() {
        return specificJobEnabled;
    }

    /**
     * Checks if the scheduler is enabled.
     */
    public boolean isSchedulerEnabled() {
        return jobsEnabled;
    }

    /**
     * Checks if sequential mode is enabled.
     */
    public boolean isSequentialMode() {
        return runSequentially;
    }

    /**
     * Checks if abort-on-failure mode is enabled.
     */
    public boolean isAbortOnGeneralFailure() {
        return abortOnGeneralFailure;
    }
}