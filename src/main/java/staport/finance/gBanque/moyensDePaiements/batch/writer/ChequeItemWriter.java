package staport.finance.gBanque.moyensDePaiements.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.services.ChequeBatchGateway;

/**
 * A specific ItemWriter for Cheque entities.
 * Its only responsibility is to save a chunk of Cheque entities.
 */
public class ChequeItemWriter implements ItemWriter<Cheque> {

    private static final Logger log = LoggerFactory.getLogger(ChequeItemWriter.class);
    private final ChequeBatchGateway chequeBatchGateway;

    public ChequeItemWriter(ChequeBatchGateway chequeBatchGateway) {
        this.chequeBatchGateway = chequeBatchGateway;
    }

    @Override
    public void write(Chunk<? extends Cheque> chunk) throws Exception {
        log.info("Saving {} Cheque entities.", chunk.size());
        chequeBatchGateway.saveAll(new java.util.ArrayList<>(chunk.getItems()));
    }
}
