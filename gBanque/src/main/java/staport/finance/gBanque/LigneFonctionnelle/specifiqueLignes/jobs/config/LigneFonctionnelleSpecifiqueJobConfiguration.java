package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.jobs.config;

import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.jobs.dtos.ProcessedSpecificFunctionalLine;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.jobs.processors.SpecificFunctionalLineProcessor;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.jobs.writers.SpecificFunctionalLineWriter;

@Configuration
public class LigneFonctionnelleSpecifiqueJobConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleSpecifiqueJobConfiguration.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final SpecificFunctionalLineProcessor processor;
    private final SpecificFunctionalLineWriter writer;

    public LigneFonctionnelleSpecifiqueJobConfiguration(JobRepository jobRepository,
                                                        PlatformTransactionManager transactionManager,
                                                        EntityManagerFactory entityManagerFactory,
                                                        SpecificFunctionalLineProcessor processor,
                                                        SpecificFunctionalLineWriter writer) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
        this.processor = processor;
        this.writer = writer;
        log.info("LigneFonctionnelleSpecifiqueJobConfiguration initialized.");
    }


    /**
     * Defines the main Spring Batch Job for specific functional line expiration.
     */
    @Bean
    public Job specificFunctionalLineExpirationJob() {
        log.info("Configuring 'specificFunctionalLineExpirationJob'.");
        return new JobBuilder("specificFunctionalLineExpirationJob", jobRepository)
                .start(processSpecificFunctionalLinesStep())
                .build();
    }

    /**
     * Defines the ItemReader for fetching active LigneFonctionnelleSpecifique entities.
     */
    @Bean
    public JpaPagingItemReader<LigneFonctionnelleSpecifique> specificFunctionalLineReader() {
        log.info("Configuring JpaPagingItemReader for active LigneFonctionnelleSpecifique entities.");
        String jpqlQuery = "SELECT l FROM LigneFonctionnelleSpecifique l WHERE l.actif = true ORDER BY l.dateFin ASC";

        return new JpaPagingItemReaderBuilder<LigneFonctionnelleSpecifique>()
                .name("specificFunctionalLineReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(jpqlQuery)
                .pageSize(100)
                .build();
    }

    /**
     * Defines the single step for the specific functional line expiration job.
     */
    @Bean
    public Step processSpecificFunctionalLinesStep() {
        log.info("Configuring 'processSpecificFunctionalLinesStep'.");
        return new StepBuilder("processSpecificFunctionalLinesStep", jobRepository)
                .<LigneFonctionnelleSpecifique, ProcessedSpecificFunctionalLine>chunk(100, transactionManager)
                .reader(specificFunctionalLineReader())
                .processor(processor)
                .writer(writer)
                .build();
    }


}