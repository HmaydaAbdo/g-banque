package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.jobs.scheduler;

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
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SpecificFunctionalLineJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(SpecificFunctionalLineJobScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job functionalLineExpirationJob;
    private final Job specificFunctionalLineExpirationJob;

    public SpecificFunctionalLineJobScheduler(JobLauncher jobLauncher,
                                      Job functionalLineExpirationJob,
                                      Job specificFunctionalLineExpirationJob) {
        this.jobLauncher = jobLauncher;
        this.functionalLineExpirationJob = functionalLineExpirationJob;
        this.specificFunctionalLineExpirationJob = specificFunctionalLineExpirationJob;
    }

    // =========================================================================================
    // 1. DEVELOPMENT Profile Schedule: Run every 5 minutes
    // =========================================================================================
    @Profile("dev")
    @Scheduled(cron = "0 0/10 * * * ?")
    public void runAllJobsDev() { // <--- Renamed to reflect running all jobs
        log.info("Launching ALL expiration jobs (DEV profile) - Running Every 5 Minutes.");

        // Launch LigneFonctionnelle Job
        runJobInternal(functionalLineExpirationJob, "FunctionalLineExpirationJob");

        // Launch LigneFonctionnelleSpecifique Job
        runJobInternal(specificFunctionalLineExpirationJob, "SpecificFunctionalLineExpirationJob");
    }

    // =========================================================================================
    // 2. STAGING/PRODUCTION Profile Schedule: Run every Sunday at 12:00 PM
    // =========================================================================================
    @Profile("!dev")
    @Scheduled(cron = "0 0 13 ? * SUN")
    public void runAllJobsStgProd() {
        log.info("Launching ALL expiration jobs (STG/PROD profile) - Running Every Sunday at 12:00 PM.");

        // Launch LigneFonctionnelle Job
        runJobInternal(functionalLineExpirationJob, "FunctionalLineExpirationJob");

        // Launch LigneFonctionnelleSpecifique Job
        runJobInternal(specificFunctionalLineExpirationJob, "SpecificFunctionalLineExpirationJob");
    }

    /**
     * Internal method to execute a job, keeping the launch logic DRY.
     */
    private void runJobInternal(Job jobToLaunch, String jobName) {
        JobParameters jobParameters = new JobParametersBuilder()
                .addDate("runDate", new Date())
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        try {
            jobLauncher.run(jobToLaunch, jobParameters);
            log.info("{} launched successfully.", jobName);
        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("{} is already running. Skipping this scheduled execution.", jobName);
        } catch (JobRestartException e) {
            log.error("{} failed to restart: {}", jobName, e.getMessage(), e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("{} instance is already complete. Check job parameters.", jobName, e);
        } catch (JobParametersInvalidException e) {
            log.error("Invalid job parameters for {}: {}", jobName, e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred while launching {}: {}", jobName, e.getMessage(), e);
        }
    }
}