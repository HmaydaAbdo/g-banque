package staport.finance.gBanque.batch.jobs.leasing.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import staport.finance.gBanque.batch.jobs.leasing.dto.ProcessedContratLeasing;
import staport.finance.gBanque.batch.jobs.leasing.listeners.ContratLeasingMainLeveeJobListener;
import staport.finance.gBanque.batch.jobs.leasing.processor.ContratLeasingMainLeveeProcessor;
import staport.finance.gBanque.batch.jobs.leasing.reader.ContratLeasingMainLeveeReader;
import staport.finance.gBanque.batch.jobs.leasing.writer.ContratLeasingMainLeveeWriter;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;

@Configuration
public class ContratLeasingMainLeveeJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ContratLeasingMainLeveeJobConfig.class);

    private static final String JOB_NAME = "contratLeasingMainLeveeJob";
    private static final String STEP_NAME = "contratLeasingMainLeveeStep";
    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ContratLeasingMainLeveeReader reader;
    private final ContratLeasingMainLeveeProcessor processor;
    private final ContratLeasingMainLeveeWriter writer;
    private final ContratLeasingMainLeveeJobListener jobListener;

    @Autowired
    public ContratLeasingMainLeveeJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ContratLeasingMainLeveeReader reader,
            ContratLeasingMainLeveeProcessor processor,
            ContratLeasingMainLeveeWriter writer,
            ContratLeasingMainLeveeJobListener jobListener) {

        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.reader = reader;
        this.processor = processor;
        this.writer = writer;
        this.jobListener = jobListener;

        log.info("ContratLeasingMainLeveeJobConfig initialized successfully.");
    }

    /**
     * Définit le Step principal du batch
     * Traite les contrats de leasing par chunks de 10
     */
    @Bean(name = STEP_NAME)
    public Step contratLeasingMainLeveeStep() {
        log.info("Configuring step: {} with chunk size: {}", STEP_NAME, CHUNK_SIZE);

        return new StepBuilder(STEP_NAME, jobRepository)
                .<ContratLeasing, ProcessedContratLeasing>chunk(CHUNK_SIZE, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(5)
                .skip(Exception.class)
                .noSkip(RuntimeException.class)
                .build();
    }

    /**
     * Définit le Job de vérification des mains levées
     * S'exécute pour identifier les contrats nécessitant une main levée
     */
    @Bean(name = JOB_NAME)
    public Job contratLeasingMainLeveeJob() {
        log.info("Configuring job: {}", JOB_NAME);

        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobListener)
                .start(contratLeasingMainLeveeStep())
                .build();
    }
}