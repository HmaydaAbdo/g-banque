package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.config;

import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.listeners.ChequeProcessingSkipListener;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.reader.PaymentItemReader;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.writer.PaymentItemWriter;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor.PaymentTypeClassifier;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.ClassifierCompositeItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Cheque; // Ensure this is the consistent Cheque entity package

/**
 * Spring Batch configuration for the FunPayment Job.
 * This class defines the Job, Step, and wires up the ItemReader,
 * ClassifierCompositeItemProcessor, and PaymentItemWriter.
 * Configured for Cheque-only processing for current testing.
 * This version addresses "assigned but never accessed" warnings by
 * injecting dependencies directly into @Bean method parameters where possible.
 */
@Configuration
public class PaymentBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final PaymentItemReader paymentItemReader;
    // REMOVED: private final ChequePaymentProcessor chequePaymentProcessor; // No longer needed as a field
    private final PaymentItemWriter paymentItemWriter;
    private final ChequeProcessingSkipListener chequeProcessingSkipListener;
    // REMOVED: private final PaymentTypeClassifier paymentTypeClassifier; // No longer needed as a field

    // Only inject fields that are directly accessed using 'this.' within the class
    public PaymentBatchConfig(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              PaymentItemReader paymentItemReader,
                              // Removed ChequePaymentProcessor from constructor args as it's not a field anymore
                              PaymentItemWriter paymentItemWriter,
                              // Removed PaymentTypeClassifier from constructor args as it's not a field anymore
                              ChequeProcessingSkipListener chequeProcessingSkipListener) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.paymentItemReader = paymentItemReader;
        this.paymentItemWriter = paymentItemWriter;
        this.chequeProcessingSkipListener = chequeProcessingSkipListener;
    }

    // --- Define the Classifier for ItemProcessor ---
    // Since PaymentTypeClassifier is @Component, Spring will create it.
    // Its constructor will automatically receive ChequePaymentProcessor.
    // We don't need to define it as a @Bean here or hold it as a field.

    // --- Define the Composite Processor using the Classifier ---
    @Bean
    public ItemProcessor<FunPaymentDto, Cheque> compositePaymentProcessor(
            PaymentTypeClassifier paymentTypeClassifier) { // <<< Spring injects PaymentTypeClassifier directly here

        ClassifierCompositeItemProcessor<FunPaymentDto, Cheque> processor =
                new ClassifierCompositeItemProcessor<>();
        processor.setClassifier(paymentTypeClassifier); // Uses the injected parameter
        return processor;
    }


    // --- Define the Batch Job ---
    @Bean
    public Job importPaymentsJob(Step paymentProcessingStep) {
        return new JobBuilder("importPaymentsJob", jobRepository)
                .start(paymentProcessingStep)
                .build();
    }


    // --- Define the Batch Step ---
    @Bean
    public Step paymentProcessingStep(
            ItemProcessor<FunPaymentDto, Cheque> compositePaymentProcessor) { // Uses injected parameter
        return new StepBuilder("paymentProcessingStep", jobRepository)
                .<FunPaymentDto, Cheque>chunk(10, transactionManager)
                .reader(paymentItemReader) // Accesses 'this.paymentItemReader' field
                .processor(compositePaymentProcessor) // Uses injected parameter
                .writer(paymentItemWriter) // Accesses 'this.paymentItemWriter' field
                .faultTolerant()
                // Define which exceptions to skip
                .skip(InvalidChequeDataException.class)
                .skip(ResourceNotFoundException.class)
                // Define the maximum number of skips before the job fails
                .skipLimit(1000)
                // Register the listener to capture skipped items
                .listener(chequeProcessingSkipListener) // Accesses 'this.chequeProcessingSkipListener' field
                .build();
    }
}