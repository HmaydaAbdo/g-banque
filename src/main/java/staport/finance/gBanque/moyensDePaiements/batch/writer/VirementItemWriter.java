package staport.finance.gBanque.moyensDePaiements.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;

/**
 * A specific ItemWriter for Virement entities.
 * Its only responsibility is to save a chunk of Virement entities.
 */
public class VirementItemWriter implements ItemWriter<Virement> {

    private static final Logger log = LoggerFactory.getLogger(VirementItemWriter.class);
    private final VirementRepository virementRepository;

    public VirementItemWriter(VirementRepository virementRepository) {
        this.virementRepository = virementRepository;
    }

    @Override
    public void write(Chunk<? extends Virement> chunk) throws Exception {
        log.info("Saving {} Virement entities.", chunk.size());
        virementRepository.saveAll(chunk.getItems());
    }
}
