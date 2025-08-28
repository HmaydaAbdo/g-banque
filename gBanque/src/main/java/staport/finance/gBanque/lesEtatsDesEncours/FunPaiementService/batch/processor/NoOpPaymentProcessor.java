// File: staport/finance/gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor/NoOpPaymentProcessor.java

package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor;

import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Cheque; // IMPORTANT: The output type should match your other processors if no common interface is used
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ItemProcessor that performs "No Operation" and returns null.
 * This is used to filter out items from the batch flow that do not
 * match any specific processing criteria.
 * Returning null from ItemProcessor.process() causes the item to be skipped (filtered).
 */
@Component
// The output type (Cheque) matches what ChequePaymentProcessor produces,
// ensuring compatibility with the ClassifierCompositeItemProcessor.
public class NoOpPaymentProcessor implements ItemProcessor<FunPaymentDto, Object> {

    private static final Logger log = LoggerFactory.getLogger(NoOpPaymentProcessor.class);

    @Override
    public Cheque process(FunPaymentDto item) throws Exception {
        // Log that the item is being filtered out
        log.debug("No-Op processing item with numero {} (type {}). This item will be filtered out (skipped).", item.getNumero(), item.getType());
        return null; // Returning null filters the item from the batch flow
    }
}