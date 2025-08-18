package staport.finance.gBanque.LigneFonctionnelle.jobs.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class FunctionalLineJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(FunctionalLineJobScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job functionalLineExpirationJob; // Inject  batch job

    public FunctionalLineJobScheduler(JobLauncher jobLauncher, Job functionalLineExpirationJob) {
        this.jobLauncher = jobLauncher;
        this.functionalLineExpirationJob = functionalLineExpirationJob;
    }

    /**
     * Schedules the functionalLineExpirationJob to run at 3:00 AM on the 1st day of every month.
     * The 'cron' expression "0 0 3 1 * ?" means:
     * - 0: Seconds (at the 0th second)
     * - 0: Minutes (at the 0th minute)
     * - 3: Hours (at 3 AM)
     * - 1: Day of the month (on the 1st day)
     * - *: Month (every month)
     * - ?: Day of the week (no specific day of the week, as it's already specified by day of month)
     *
     * It's crucial to pass unique JobParameters for each execution to ensure Spring Batch
     * creates a new JobInstance. A common approach is to use a timestamp.
     */

    @Scheduled(cron = "0 0 12 ? * SUN") // Run every Sunday at 12:00 PM
    //@Scheduled(cron = "0 * * * * ?") // Run at every minute
    public void runFunctionalLineExpirationJob() {
        log.info("Attempting to launch functionalLineExpirationJob...");

        // Generate unique JobParameters to ensure a new JobInstance is created for each run.
        // This is crucial for Spring Batch to track job executions correctly.
        JobParameters jobParameters = new JobParametersBuilder()
                .addDate("runDate", new Date())
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        try {
            jobLauncher.run(functionalLineExpirationJob, jobParameters);
            log.info("FunctionalLineExpirationJob launched successfully.");
        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("FunctionalLineExpirationJob is already running. Skipping this scheduled execution.");
        } catch (JobRestartException e) {
            log.error("FunctionalLineExpirationJob failed to restart: {}", e.getMessage(), e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("FunctionalLineExpirationJob instance is already complete. A new instance could not be created. This might indicate an issue with unique job parameters.", e);
        } catch (JobParametersInvalidException e) {
            log.error("Invalid job parameters for FunctionalLineExpirationJob: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred while launching FunctionalLineExpirationJob: {}", e.getMessage(), e);
        }
    }
}