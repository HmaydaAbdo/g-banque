package staport.finance.gBanque.moyensDePaiements.synchronisation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.moyensDePaiements.batch.metrics.FunPaiementSyncMetrics;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EtasDesEncoursService {

    private static final Logger log = LoggerFactory.getLogger(EtasDesEncoursService.class);
    private static final DateTimeFormatter JOB_PARAM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final JobLauncher jobLauncher;
    private final FunPaiementSyncMetrics funPaiementSyncMetrics;


    private final Job importPaymentsJob;

    public EtasDesEncoursService(JobLauncher jobLauncher, FunPaiementSyncMetrics funPaiementSyncMetrics, @Qualifier("importPaymentsJob") Job importPaymentsJob) {
        this.jobLauncher = jobLauncher;
        this.funPaiementSyncMetrics = funPaiementSyncMetrics;
        this.importPaymentsJob = importPaymentsJob;
    }

    /**
     * Launches the Spring Batch job to import payment data.
     * Each job launch needs unique JobParameters. We use a timestamp for this.
     *
     * @return The JobExecution object representing the launched job instance.
     * @throws JobExecutionException If the job fails to start or encounters a critical error during execution.
     */
    public JobExecution launchPaymentImportJob() throws JobExecutionException {
        // Create unique JobParameters for each execution
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("launchTime", OffsetDateTime.now().format(JOB_PARAM_DATE_FORMATTER))
                .toJobParameters();

        log.info("Attempting to launch importPaymentsJob with parameters: {}", jobParameters);

        long debut = System.nanoTime();
        JobExecution jobExecution = null;
        try {
            jobExecution = jobLauncher.run(importPaymentsJob, jobParameters);
            log.info("Job importPaymentsJob launched successfully. JobExecution ID: {}", jobExecution.getId());
            log.info("Job Status: {}", jobExecution.getStatus());

            Duration duree = Duration.ofNanos(System.nanoTime() - debut);
            if (jobExecution.getStatus().isUnsuccessful()) {
                funPaiementSyncMetrics.enregistrerEchec(duree);
            } else {
                funPaiementSyncMetrics.enregistrerSucces(duree);
            }
        } catch (JobExecutionException e) {
            log.error("Failed to launch importPaymentsJob: {}", e.getMessage(), e);
            funPaiementSyncMetrics.enregistrerEchec(Duration.ofNanos(System.nanoTime() - debut));
            throw e; // Re-throw the exception for upstream handling
        }
        return jobExecution;
    }




}