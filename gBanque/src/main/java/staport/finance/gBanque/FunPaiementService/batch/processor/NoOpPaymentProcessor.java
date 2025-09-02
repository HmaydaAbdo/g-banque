package staport.finance.gBanque.FunPaiementService.batch.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.FunPaiementService.dtos.FunPaymentDto;

/**
 * An ItemProcessor that performs "No Operation" and returns null.
 * This is used to filter out items from the batch flow that do not
 * match any specific processing criteria.
 * Returning null from ItemProcessor.process() causes the item to be skipped (filtered).
 */
@Component
public class NoOpPaymentProcessor implements ItemProcessor<FunPaymentDto, Object> {

    private static final Logger log = LoggerFactory.getLogger(NoOpPaymentProcessor.class);

    @Override
    public Object process(FunPaymentDto item) {
        // Log that the item is being filtered out
        log.debug("No-Op processing item with numero {} (type {}). This item will be filtered out (skipped).", item.getNumero(), item.getType());
        return null; // Returning null filters the item from the batch flow
    }
}
