package staport.finance.gBanque.batch.jobs.moyensDePaiements.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;

/**
 * A specific ItemWriter for Cheque entities.
 * Its only responsibility is to save a chunk of Cheque entities.
 */
public class ChequeItemWriter implements ItemWriter<Cheque> {

    private static final Logger log = LoggerFactory.getLogger(ChequeItemWriter.class);
    private final ChequeRepository chequeRepository;

    public ChequeItemWriter(ChequeRepository chequeRepository) {
        this.chequeRepository = chequeRepository;
    }

    @Override
    public void write(Chunk<? extends Cheque> chunk) throws Exception {
        log.info("Saving {} Cheque entities.", chunk.size());
        chequeRepository.saveAll(chunk.getItems());
    }
}
