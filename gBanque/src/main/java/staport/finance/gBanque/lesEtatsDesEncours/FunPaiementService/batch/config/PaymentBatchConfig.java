package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.config;

import org.springframework.beans.factory.annotation.Qualifier;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.listeners.ProcessingSkipListener;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.reader.PaymentItemReader;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor.PaymentTypeClassifier;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ClassifierCompositeItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;

@Configuration
public class PaymentBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PaymentItemReader paymentItemReader;
    private final ProcessingSkipListener processingSkipListener;

    public PaymentBatchConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PaymentItemReader paymentItemReader,
            ProcessingSkipListener processingSkipListener
    ) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.paymentItemReader = paymentItemReader;
        this.processingSkipListener = processingSkipListener;
    }


    // --- Batch Job ---
    @Bean
    public Job importPaymentsJob(Step paymentProcessingStep) {
        return new JobBuilder("importPaymentsJob", jobRepository)
                .start(paymentProcessingStep)
                .build();
    }

    // --- Composite Processor ---
    @Bean
    public ItemProcessor<FunPaymentDto, Object> compositePaymentProcessor(PaymentTypeClassifier paymentTypeClassifier) {
        ClassifierCompositeItemProcessor<FunPaymentDto, Object> processor = new ClassifierCompositeItemProcessor<>();
        processor.setClassifier(paymentTypeClassifier);
        return processor;
    }

    // --- Batch Step ---
    @Bean
    public Step paymentProcessingStep(
            ItemProcessor<FunPaymentDto, Object> compositePaymentProcessor,
            @Qualifier("paymentWriter") ItemWriter<Object> paymentWriter
    ) {
        return new StepBuilder("paymentProcessingStep", jobRepository)
                .<FunPaymentDto, Object>chunk(10, transactionManager)
                .reader(paymentItemReader)
                .processor(compositePaymentProcessor)
                .writer(paymentWriter) // Use the new, unified writer bean directly
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(1000)
                .listener(processingSkipListener)
                .build();
    }
}