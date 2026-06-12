package staport.finance.gBanque.moyensDePaiements.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;

/**
 * A specific ItemWriter for Lcn entities.
 * Its only responsibility is to save a chunk of Lcn entities.
 */
public class LcnItemWriter implements ItemWriter<Lcn> {

    private static final Logger log = LoggerFactory.getLogger(LcnItemWriter.class);
    private final LcnRepository lcnRepository;

    public LcnItemWriter(LcnRepository lcnRepository) {
        this.lcnRepository = lcnRepository;
    }

    @Override
    public void write(Chunk<? extends Lcn> chunk) throws Exception {
        log.info("Saving {} Lcn entities.", chunk.size());
        lcnRepository.saveAll(chunk.getItems());
    }
}
