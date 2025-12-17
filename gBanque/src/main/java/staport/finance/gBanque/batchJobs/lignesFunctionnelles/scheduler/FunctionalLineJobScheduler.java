package staport.finance.gBanque.batchJobs.lignesFunctionnelles.scheduler;

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
    private final Job functionalLineExpirationJob;

    public FunctionalLineJobScheduler(JobLauncher jobLauncher, Job functionalLineExpirationJob) {
        this.jobLauncher = jobLauncher;
        this.functionalLineExpirationJob = functionalLineExpirationJob;
    }



    // =========================================================================================
    // Run every Sunday at 12:00 PM (as originally planned)
    // Cron: "0 0 12 ? * SUN"
    // =========================================================================================
    @Scheduled(cron = "0 0 12 ? * SUN")
    public void runFunctionalLineExpirationJobStgProd() {
        log.info("Launching functionalLineExpirationJob (STG/PROD profile) - Running Every Sunday at 12:00 PM.");
        runJobInternal();
    }

    /**
     * Internal method to execute the job, keeping the launch logic DRY (Don't Repeat Yourself).
     */
    private void runJobInternal() {
        // Generate unique JobParameters to ensure a new JobInstance is created for each run.
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