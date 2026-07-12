package staport.finance.gBanque.moyensDePaiements.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import staport.finance.gBanque.commision.service.CommissionService;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.services.MiseADispositionBatchGateway;

import java.util.ArrayList;
import java.util.List;

/**
 * A specific ItemWriter for MiseADisposition entities.
 * Sauvegarde le chunk puis déclenche, pour chaque mise à disposition, la génération
 * de commission si une règle est configurée pour sa banque (décaissement uniquement).
 */
public class MiseADispositionItemWriter implements ItemWriter<MiseADisposition> {

    private static final Logger log = LoggerFactory.getLogger(MiseADispositionItemWriter.class);
    private final MiseADispositionBatchGateway miseADispositionBatchGateway;
    private final CommissionService commissionService;

    public MiseADispositionItemWriter(MiseADispositionBatchGateway miseADispositionBatchGateway, CommissionService commissionService) {
        this.miseADispositionBatchGateway = miseADispositionBatchGateway;
        this.commissionService = commissionService;
    }

    @Override
    public void write(Chunk<? extends MiseADisposition> chunk) throws Exception {
        log.info("Saving {} MiseADisposition entities.", chunk.size());
        List<MiseADisposition> saved = miseADispositionBatchGateway.saveAll(new ArrayList<>(chunk.getItems()));

        for (MiseADisposition mad : saved) {
            commissionService.generateCommissionIfNeeded(mad);
        }
    }
}
