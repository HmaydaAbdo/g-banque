
package staport.finance.gBanque.batchJobs.generalFunctionalLines.config;


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
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.batchJobs.generalFunctionalLines.dtos.ProcessedFunctionalLine;
import staport.finance.gBanque.batchJobs.generalFunctionalLines.listeners.FunctionalLineJobListener;
import staport.finance.gBanque.batchJobs.generalFunctionalLines.processors.FunctionalLineProcessor;
import staport.finance.gBanque.batchJobs.generalFunctionalLines.writers.FunctionalLineWriter;


/**
 * Spring Batch Configuration for the Functional Line Expiration Job.
 * This class defines the entire batch job flow, including the ItemReader, ItemProcessor,
 * ItemWriter, and the Step(s) and Job.
 * It uses JpaPagingItemReader for efficient database reading and auto-wires
 * the processor and writer as they are Spring Components.
 *
 * Adheres to SRP by primarily defining the batch flow and injecting specialized components.
 */
@Configuration
public class LigneFonctionnelleJobConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleJobConfiguration.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    // FunctionalLineProcessor and FunctionalLineWriter are @Component, so Spring injects them directly.
    private final FunctionalLineProcessor functionalLineProcessor;
    private final FunctionalLineWriter functionalLineWriter;
    private final FunctionalLineJobListener functionalLineJobListener;

    /**
     * Constructor for injecting required Spring Batch infrastructure components
     * and custom batch components.
     *
     * @param jobRepository The Spring Batch JobRepository.
     * @param transactionManager The Spring transaction manager.
     * @param entityManagerFactory The JPA EntityManagerFactory for database operations.
     * @param functionalLineProcessor The custom ItemProcessor for functional lines.
     * @param functionalLineWriter The custom ItemWriter for functional lines and notifications.
     */
    public LigneFonctionnelleJobConfiguration(JobRepository jobRepository,
                                              PlatformTransactionManager transactionManager,
                                              EntityManagerFactory entityManagerFactory,
                                              FunctionalLineProcessor functionalLineProcessor,
                                              FunctionalLineWriter functionalLineWriter, FunctionalLineJobListener functionalLineJobListener) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
        this.functionalLineProcessor = functionalLineProcessor;
        this.functionalLineWriter = functionalLineWriter;
        this.functionalLineJobListener = functionalLineJobListener;
        log.info("LigneFonctionnelleJobConfiguration initialized.");
    }



    /**
     * Defines the main Spring Batch Job for functional line expiration.
     * This job simply executes the `processFunctionalLinesStep`.
     *
     * @return A configured Spring Batch Job.
     */
    @Bean
    public Job functionalLineExpirationJob() {
        log.info("Configuring 'functionalLineExpirationJob'.");
        return new JobBuilder("functionalLineExpirationJob", jobRepository)
                .listener(functionalLineJobListener)
                .start(processFunctionalLinesStep()) // The starting step of this job
                .build();
    }

    /**
     * Defines the single step for our functional line expiration job.
     * This step is chunk-oriented, reading `LigneFonctionnelle` entities,
     * processing them with `FunctionalLineProcessor`, and writing results/notifications
     * with `FunctionalLineWriter`.
     *
     * @return A configured Spring Batch Step.
     */
    @Bean
    public Step processFunctionalLinesStep() {
        log.info("Configuring 'processFunctionalLinesStep'.");
        return new StepBuilder("processFunctionalLinesStep", jobRepository)
                // Configure chunk-oriented processing: reads LigneFonctionnelle, processes into ProcessedFunctionalLine
                .<LigneFonctionnelle, ProcessedFunctionalLine>chunk(100, transactionManager)
                .reader(functionalLineReader())      // Assigns the ItemReader
                .processor(functionalLineProcessor)  // Assigns the ItemProcessor
                .writer(functionalLineWriter)        // Assigns the ItemWriter
                .build();
    }


    /**
     * Defines the ItemReader for fetching active LigneFonctionnelle entities.
     * It uses JpaPagingItemReader to efficiently read data in pages,
     * specifically fetching active lines ordered by their end date.
     *
     * @return A configured JpaPagingItemReader.
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<LigneFonctionnelle> functionalLineReader() {
        log.info("Configuring JpaPagingItemReader for active LigneFonctionnelle entities.");
        // JPQL query to select active functional lines, ordered by dateFin ascending.
        // This order is crucial for the business logic in the processor (e.g., handling expired lines first).
        String jpqlQuery = "SELECT l FROM LigneFonctionnelle l WHERE l.actif = true ORDER BY l.dateFin ASC";

        return new JpaPagingItemReaderBuilder<LigneFonctionnelle>()
                .name("functionalLineReader") // Unique name for the reader bean
                .entityManagerFactory(entityManagerFactory) // Provides the JPA context
                .queryString(jpqlQuery) // The JPQL query to fetch data
                .pageSize(100) // Number of items to read in a single page/chunk
                .build();
    }
}
