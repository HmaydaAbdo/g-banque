package staport.finance.gBanque.batch.jobs.cmt.config;


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
import staport.finance.gBanque.batch.jobs.cmt.dto.ProcessedCreditMoyenTerme;
import staport.finance.gBanque.batch.jobs.cmt.listener.CreditMoyenTermeMainLeveeJobListener;
import staport.finance.gBanque.batch.jobs.cmt.processor.CreditMoyenTermeMainLeveeProcessor;
import staport.finance.gBanque.batch.jobs.cmt.reader.CreditMoyenTermeMainLeveeReader;
import staport.finance.gBanque.batch.jobs.cmt.writer.CreditMoyenTermeMainLeveeWriter;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;

@Configuration
public class CreditMoyenTermeMainLeveeJobConfig {

    private static final Logger log = LoggerFactory.getLogger(CreditMoyenTermeMainLeveeJobConfig.class);

    private static final String JOB_NAME = "creditMoyenTermeMainLeveeJob";
    private static final String STEP_NAME = "creditMoyenTermeMainLeveeStep";
    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CreditMoyenTermeMainLeveeReader reader;
    private final CreditMoyenTermeMainLeveeProcessor processor;
    private final CreditMoyenTermeMainLeveeWriter writer;
    private final CreditMoyenTermeMainLeveeJobListener jobListener;

    @Autowired
    public CreditMoyenTermeMainLeveeJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            CreditMoyenTermeMainLeveeReader reader,
            CreditMoyenTermeMainLeveeProcessor processor,
            CreditMoyenTermeMainLeveeWriter writer,
            CreditMoyenTermeMainLeveeJobListener jobListener) {

        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.reader = reader;
        this.processor = processor;
        this.writer = writer;
        this.jobListener = jobListener;

        log.info("Configuration de CreditMoyenTermeMainLeveeJobConfig initialisée avec succès");
    }

    /**
     * Définit le Step principal du batch
     * Traite les crédits CMT par chunks de 10
     */
    @Bean(name = STEP_NAME)
    public Step creditMoyenTermeMainLeveeStep() {
        log.info("Configuration du step: {} avec une taille de chunk: {}", STEP_NAME, CHUNK_SIZE);

        return new StepBuilder(STEP_NAME, jobRepository)
                .<CreditMoyenTerme, ProcessedCreditMoyenTerme>chunk(CHUNK_SIZE, transactionManager)
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
     * S'exécute pour identifier les crédits CMT nécessitant une main levée
     */
    @Bean(name = JOB_NAME)
    public Job creditMoyenTermeMainLeveeJob() {
        log.info("Configuration du job: {}", JOB_NAME);

        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobListener)
                .start(creditMoyenTermeMainLeveeStep())
                .build();
    }
}