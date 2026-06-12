package staport.finance.gBanque.LigneFonctionnelle.batch.specifique.config;

import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.batch.specifique.dtos.ProcessedSpecificFunctionalLine;
import staport.finance.gBanque.LigneFonctionnelle.batch.specifique.listeners.SpecificFunctionalLineJobListener;
import staport.finance.gBanque.LigneFonctionnelle.batch.specifique.processors.SpecificFunctionalLineProcessor;
import staport.finance.gBanque.LigneFonctionnelle.batch.specifique.writers.SpecificFunctionalLineWriter;

@Configuration
public class LigneFonctionnelleSpecifiqueJobConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleSpecifiqueJobConfiguration.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final SpecificFunctionalLineProcessor processor;
    private final SpecificFunctionalLineWriter writer;
    private final SpecificFunctionalLineJobListener listener;

    public LigneFonctionnelleSpecifiqueJobConfiguration(JobRepository jobRepository,
                                                        PlatformTransactionManager transactionManager,
                                                        EntityManagerFactory entityManagerFactory,
                                                        SpecificFunctionalLineProcessor processor,
                                                        SpecificFunctionalLineWriter writer, SpecificFunctionalLineJobListener listener) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
        this.processor = processor;
        this.writer = writer;
        this.listener = listener;
        log.info("LigneFonctionnelleSpecifiqueJobConfiguration initialized.");
    }


    /**
     * Defines the main Spring Batch Job for specific functional line expiration.
     */
    @Bean
    public Job specificFunctionalLineExpirationJob() {
        log.info("Configuring 'specificFunctionalLineExpirationJob'.");
        return new JobBuilder("specificFunctionalLineExpirationJob", jobRepository)
                .listener(listener)
                .start(processSpecificFunctionalLinesStep())
                .build();
    }

    /**
     * Defines the ItemReader for fetching active LigneFonctionnelleSpecifique entities.
     */
    @Bean
    @StepScope
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