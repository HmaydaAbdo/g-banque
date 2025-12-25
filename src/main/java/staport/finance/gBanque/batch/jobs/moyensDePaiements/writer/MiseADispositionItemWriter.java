package staport.finance.gBanque.batch.jobs.moyensDePaiements.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;

/**
 * A specific ItemWriter for MiseADisposition entities.
 * Its only responsibility is to save a chunk of MiseADisposition entities.
 */
public class MiseADispositionItemWriter implements ItemWriter<MiseADisposition> {

    private static final Logger log = LoggerFactory.getLogger(MiseADispositionItemWriter.class);
    private final MiseADispositionRepository miseADispositionRepository;

    public MiseADispositionItemWriter(MiseADispositionRepository miseADispositionRepository) {
        this.miseADispositionRepository = miseADispositionRepository;
    }

    @Override
    public void write(Chunk<? extends MiseADisposition> chunk) throws Exception {
        log.info("Saving {} MiseADisposition entities.", chunk.size());
        miseADispositionRepository.saveAll(chunk.getItems());
    }
}
