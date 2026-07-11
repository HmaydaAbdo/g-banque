package staport.finance.gBanque.moyensDePaiements.batch.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.synchronisation.client.FunPaiementClient;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Spring Batch ItemReader qui consomme le flux FunPaiement via {@link FunPaiementClient}
 * (appel réseau extrait dans ce client — 10/07/2026).
 * The fetch logic is triggered for every job execution within the open() method,
 * ensuring fresh data for each run.
 */
@Component
public class PaymentItemReader implements ItemStreamReader<FunPaymentDto> {

    private static final Logger log = LoggerFactory.getLogger(PaymentItemReader.class);

    private final FunPaiementClient funPaiementClient;
    private Iterator<FunPaymentDto> paymentIterator;

    public PaymentItemReader(FunPaiementClient funPaiementClient) {
        this.funPaiementClient = funPaiementClient;
    }

    /**
     * This method is part of the ItemStreamReader interface and is called
     * at the beginning of each step execution.
     * It's the correct place to fetch all payments to ensure a fresh dataset
     * for every batch run.
     * @param executionContext The current execution context.
     * @throws ItemStreamException if an error occurs during initialization.
     */
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        log.info("PaymentItemReader: Initializing for a new job execution by fetching all payments from external API...");
        try {
            List<FunPaymentDto> allPayments = funPaiementClient.fetchAll();

            if (!allPayments.isEmpty()) {
                this.paymentIterator = allPayments.iterator();
                log.info("PaymentItemReader: Successfully fetched {} payments for processing.", allPayments.size());
            } else {
                this.paymentIterator = Collections.emptyIterator();
                log.warn("PaymentItemReader: No payments found or fetched from the external API.");
            }
        } catch (Exception e) {
            log.error("PaymentItemReader: Failed to fetch payments from external API during open().", e);
            throw new ItemStreamException("Failed to initialize PaymentItemReader during open(): " + e.getMessage(), e);
        }
    }

    /**
     * Reads the next item from the in-memory list of payments.
     * This method is called repeatedly by the Spring Batch framework
     * until it returns null, signaling the end of the input.
     * @return The next FunPaymentDto item, or null if no more items are available.
     */
    @Override
    public FunPaymentDto read() {
        if (paymentIterator.hasNext()) {
            FunPaymentDto item = paymentIterator.next();
            log.debug("PaymentItemReader: Providing item with numero: {}", item.getNumero());
            return item;
        } else {
            log.info("PaymentItemReader: All items have been read.");
            return null;
        }
    }

    /**
     * No-op method as we don't need to persist state.
     */
    @Override
    public void update(ExecutionContext executionContext) {
        // No state to save
    }

    /**
     * This method is part of the ItemStreamReader interface and is called
     * at the end of each step execution. We reset the iterator here.
     * @throws ItemStreamException if an error occurs during closing.
     */
    @Override
    public void close() throws ItemStreamException {
        log.info("PaymentItemReader: Closing the reader and resetting iterator.");
        this.paymentIterator = null;
    }
}
