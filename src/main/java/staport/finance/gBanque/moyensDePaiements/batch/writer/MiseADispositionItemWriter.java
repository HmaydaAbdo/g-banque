package staport.finance.gBanque.moyensDePaiements.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import staport.finance.gBanque.commision.service.CommissionService;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;

import java.util.List;

/**
 * A specific ItemWriter for MiseADisposition entities.
 * Sauvegarde le chunk puis déclenche, pour chaque mise à disposition, la génération
 * de commission si une règle est configurée pour sa banque (décaissement uniquement).
 */
public class MiseADispositionItemWriter implements ItemWriter<MiseADisposition> {

    private static final Logger log = LoggerFactory.getLogger(MiseADispositionItemWriter.class);
    private final MiseADispositionRepository miseADispositionRepository;
    private final CommissionService commissionService;

    public MiseADispositionItemWriter(MiseADispositionRepository miseADispositionRepository, CommissionService commissionService) {
        this.miseADispositionRepository = miseADispositionRepository;
        this.commissionService = commissionService;
    }

    @Override
    public void write(Chunk<? extends MiseADisposition> chunk) throws Exception {
        log.info("Saving {} MiseADisposition entities.", chunk.size());
        List<? extends MiseADisposition> saved = miseADispositionRepository.saveAll(chunk.getItems());

        for (MiseADisposition mad : saved) {
            commissionService.generateCommissionIfNeeded(mad);
        }
    }
}
