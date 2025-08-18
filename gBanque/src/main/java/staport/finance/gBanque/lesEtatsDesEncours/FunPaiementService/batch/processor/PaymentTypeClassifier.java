// File: staport/finance/gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor/PaymentTypeClassifier.java

package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor;

import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.classify.Classifier;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Cheque; // Import Cheque entity

import java.util.HashMap;
import java.util.Map;

/**
 * Classifier that maps a FunPaymentDto to a specific ItemProcessor based on its 'type' attribute.
 * It routes type '1' to ChequePaymentProcessor. Items with other types are filtered out using NoOpPaymentProcessor.
 * Generic types are specific to Cheque, as NoOpPaymentProcessor returns null (which is compatible with Cheque).
 */
@Component
// Classifier outputs ItemProcessor that returns Cheque
public class PaymentTypeClassifier implements Classifier<FunPaymentDto, ItemProcessor<?, ? extends Cheque>> {

    private static final Logger log = LoggerFactory.getLogger(PaymentTypeClassifier.class);

    // The map for specific type processors
    private final Map<Integer, ItemProcessor<FunPaymentDto, ? extends Cheque>> processorMap = new HashMap<>();
    private final ItemProcessor<FunPaymentDto, ? extends Cheque> noOpProcessor; // The processor that filters items

    // Inject both ChequePaymentProcessor and NoOpPaymentProcessor
    public PaymentTypeClassifier(ChequePaymentProcessor chequePaymentProcessor,
                                 NoOpPaymentProcessor noOpProcessor) { // <<< NoOpProcessor is injected here
        this.noOpProcessor = noOpProcessor;
        processorMap.put(1, chequePaymentProcessor); // Only map type 1

        log.info("PaymentTypeClassifier initialized. Only type 1 is explicitly mapped to ChequePaymentProcessor. Other types will be filtered out.");
    }

    /**
     * Classifies a FunPaymentDto item to determine which ItemProcessor should handle it.
     *
     * @param item The FunPaymentDto to classify.
     * @return The ChequePaymentProcessor for type 1, or the NoOpPaymentProcessor which filters out the item for all other types.
     */
    @Override
    public ItemProcessor<?, ? extends Cheque> classify(FunPaymentDto item) {
        int paymentType = item.getType();

        ItemProcessor<FunPaymentDto, ? extends Cheque> processor = processorMap.get(paymentType);

        if (processor == null) {
            // For items not of type 1, return the NoOpProcessor.
            // NoOpProcessor's 'process' method will then return null, filtering the item.
            log.warn("Item with numero {} has unhandled payment type '{}'. It will be filtered out (skipped) from processing.", item.getNumero(), paymentType);
            return noOpProcessor; // <<< Correct: returns the NoOpProcessor instance
        }

        log.debug("Classifying payment ID {} with type '{}' to processor: {}", item.getNumero(), paymentType, processor.getClass().getSimpleName());
        return processor;
    }
}