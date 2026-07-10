package staport.finance.gBanque.moyensDePaiements.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import staport.finance.gBanque.commision.service.CommissionService;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;

import java.util.List;

/**
 * A specific ItemWriter for Lcn entities.
 * Sauvegarde le chunk puis déclenche, pour chaque LCN, la génération de commission
 * si une règle est configurée pour sa banque (décaissement uniquement).
 */
public class LcnItemWriter implements ItemWriter<Lcn> {

    private static final Logger log = LoggerFactory.getLogger(LcnItemWriter.class);
    private final LcnRepository lcnRepository;
    private final CommissionService commissionService;

    public LcnItemWriter(LcnRepository lcnRepository, CommissionService commissionService) {
        this.lcnRepository = lcnRepository;
        this.commissionService = commissionService;
    }

    @Override
    public void write(Chunk<? extends Lcn> chunk) throws Exception {
        log.info("Saving {} Lcn entities.", chunk.size());
        List<? extends Lcn> saved = lcnRepository.saveAll(chunk.getItems());

        for (Lcn lcn : saved) {
            commissionService.generateCommissionIfNeeded(lcn);
        }
    }
}
