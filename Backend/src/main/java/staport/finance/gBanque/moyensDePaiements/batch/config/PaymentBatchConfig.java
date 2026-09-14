package staport.finance.gBanque.moyensDePaiements.batch.config;

import org.springframework.beans.factory.annotation.Qualifier;
import staport.finance.gBanque.moyensDePaiements.batch.listeners.EmailNotificationJobListener;
import staport.finance.gBanque.moyensDePaiements.batch.listeners.ProcessingSkipListener;
import staport.finance.gBanque.moyensDePaiements.batch.reader.PaymentItemReader;
import staport.finance.gBanque.moyensDePaiements.batch.processor.PaymentTypeClassifier;
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
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;

@Configuration
public class PaymentBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PaymentItemReader paymentItemReader;
    private final ProcessingSkipListener processingSkipListener;
    private final EmailNotificationJobListener emailNotificationJobListener;

    public PaymentBatchConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PaymentItemReader paymentItemReader,
            ProcessingSkipListener processingSkipListener, EmailNotificationJobListener emailNotificationJobListener
    ) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.paymentItemReader = paymentItemReader;
        this.processingSkipListener = processingSkipListener;
        this.emailNotificationJobListener = emailNotificationJobListener;
    }


    // --- Batch Job ---
    @Bean
    public Job importPaymentsJob(Step paymentProcessingStep) {
        return new JobBuilder("importPaymentsJob", jobRepository)
                .start(paymentProcessingStep)
                .listener(emailNotificationJobListener)
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
                .skipLimit(10)
                .listener(processingSkipListener)
                .build();
    }
}