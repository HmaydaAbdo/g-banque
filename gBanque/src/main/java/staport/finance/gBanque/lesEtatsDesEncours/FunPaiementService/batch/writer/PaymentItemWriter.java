// File: staport/finance/gBanque/lesEtatsDesEncours/FunPaiementService/batch/writer/PaymentItemWriter.java

package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Cheque;
import staport.finance.gBanque.lesEtatsDesEncours.core.repositories.ChequeRepository;


@Component
public class PaymentItemWriter implements ItemWriter<Cheque> {

    private static final Logger log = LoggerFactory.getLogger(PaymentItemWriter.class);

    private final ChequeRepository chequeRepository; // Inject the repository

    // Constructor injection for ChequeRepository
    public PaymentItemWriter(ChequeRepository chequeRepository) {
        this.chequeRepository = chequeRepository;
        log.info("PaymentItemWriter initialized with ChequeRepository.");
    }

    @Override
    public void write(Chunk<? extends Cheque> chunk) throws Exception {
        if (chunk == null || chunk.isEmpty()) {
            log.debug("No items in chunk to write.");
            return;
        }

        log.info("Writing {} Cheque entities to the database.", chunk.size());
        try {
            chequeRepository.saveAll(chunk.getItems()); // Save all items in the chunk
            log.debug("Successfully wrote {} Cheque entities. First item numero: {}", chunk.size(), chunk.getItems().get(0).getNumero());
        } catch (Exception e) {
            log.error("Failed to write chunk of Cheque entities to database. First item numero: {}. Error: {}",
                    chunk.getItems().get(0).getNumero(), e.getMessage(), e);
            // Re-throw the exception to allow Spring Batch to handle it (e.g., skip or retry)
            throw e;
        }
    }
}