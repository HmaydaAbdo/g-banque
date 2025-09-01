package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.classify.Classifier;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;

import java.util.HashMap;
import java.util.Map;

/**
 * Classifier that maps a FunPaymentDto to a specific ItemProcessor based on its 'type' attribute.
 * The generic signature is now correctly aligned with the ItemProcessor,
 * ensuring the returned ItemProcessor is compatible.
 */
@Component
public class PaymentTypeClassifier implements Classifier<FunPaymentDto, ItemProcessor<?, ?>> {

    private static final Logger log = LoggerFactory.getLogger(PaymentTypeClassifier.class);

    private final Map<Integer, ItemProcessor<? super FunPaymentDto, Object>> processorMap = new HashMap<>();
    private final NoOpPaymentProcessor noOpProcessor;

    public PaymentTypeClassifier(
            ChequePaymentProcessor chequePaymentProcessor,
            LcnPaymentProcessor lcnPaymentProcessor,
            VirementPaymentProcessor virementPaymentProcessor,
            MiseADispositionPaymentProcessor miseADispositionPaymentProcessor,
            NoOpPaymentProcessor noOpProcessor
    ) {
        this.noOpProcessor = noOpProcessor;

        processorMap.put(1, chequePaymentProcessor);
        processorMap.put(2, lcnPaymentProcessor);
        processorMap.put(3, virementPaymentProcessor);
        processorMap.put(4, miseADispositionPaymentProcessor);

        log.info("PaymentTypeClassifier initialized. Types mapped: 1 -> Cheque, 2 -> LCN, 3 -> Virement, 4 -> Mise à disposition. Others will be skipped.");
    }

    /**
     * Classifies a FunPaymentDto to the appropriate processor.
     * Returns the processor for the given type, or the No-Op processor if the type is not found.
     *
     * @param item The payment DTO to classify.
     * @return The specific ItemProcessor or the NoOpPaymentProcessor.
     */
    @Override
    public ItemProcessor<? super FunPaymentDto, Object> classify(FunPaymentDto item) {
        int paymentType = item.getType();
        ItemProcessor<? super FunPaymentDto, Object> processor = processorMap.getOrDefault(paymentType, noOpProcessor);

        if (processor == noOpProcessor) {
            log.warn("Item with numero {} has unhandled payment type '{}'. This item will be skipped.", item.getNumero(), paymentType);
        } else {
            log.debug("Classifying payment {} with type '{}' to processor: {}", item.getNumero(), paymentType, processor.getClass().getSimpleName());
        }

        return processor;
    }
}
