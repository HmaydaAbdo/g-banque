
package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.classify.Classifier;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;

import java.util.HashMap;
import java.util.Map;

/**
 * Classifier that maps a FunPaymentDto to a specific ItemProcessor based on its 'type' attribute.
 * The generic signature is corrected to align with the ClassifierCompositeItemProcessor,
 * ensuring the returned ItemProcessor produces an Object type.
 */
@Component
public class PaymentTypeClassifier implements Classifier<FunPaymentDto, ItemProcessor<?, ?>>{

    private static final Logger log = LoggerFactory.getLogger(PaymentTypeClassifier.class);

    private final Map<Integer, ItemProcessor<? super FunPaymentDto, Object>> processorMap = new HashMap<>();
    private final ItemProcessor<? super FunPaymentDto, Object> noOpProcessor;

    public PaymentTypeClassifier(
            ChequePaymentProcessor chequePaymentProcessor,
            LcnPaymentProcessor lcnPaymentProcessor,
            MiseADispositionPaymentProcessor miseADispositionPaymentProcessor,
            NoOpPaymentProcessor noOpProcessor
    ) {

        this.noOpProcessor = (ItemProcessor<? super FunPaymentDto, Object>) noOpProcessor;

        processorMap.put(1, (ItemProcessor<? super FunPaymentDto, Object>) chequePaymentProcessor);
        processorMap.put(2, (ItemProcessor<? super FunPaymentDto, Object>) lcnPaymentProcessor);
        processorMap.put(4, (ItemProcessor<? super FunPaymentDto, Object>) miseADispositionPaymentProcessor);

        log.info("PaymentTypeClassifier initialized. Types mapped: 1 -> Cheque, 2 -> LCN, 4-> mise à disposition .Others skipped.");
    }

    @Override
    public ItemProcessor<? super FunPaymentDto, Object> classify(FunPaymentDto item) {
        int paymentType = item.getType();

        ItemProcessor<? super FunPaymentDto, Object> processor = processorMap.getOrDefault(paymentType, noOpProcessor);

        if (processor == noOpProcessor) {
            log.warn("Item with numero {} has unhandled payment type '{}'. Skipped.", item.getNumero(), paymentType);
        } else {
            log.debug("Classifying payment {} with type '{}' to processor: {}", item.getNumero(), paymentType, processor.getClass().getSimpleName());
        }

        return processor;
    }
}
