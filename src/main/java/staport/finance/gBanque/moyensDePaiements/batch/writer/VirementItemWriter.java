package staport.finance.gBanque.moyensDePaiements.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.commision.service.CommissionService;
import java.util.List;

public class VirementItemWriter implements ItemWriter<Virement> {

    private static final Logger log = LoggerFactory.getLogger(VirementItemWriter.class);


    private final VirementRepository virementRepository;
    private final CommissionService commissionService;

    public VirementItemWriter(
            VirementRepository virementRepository,
            CommissionService commissionService) {
        this.virementRepository = virementRepository;
        this.commissionService = commissionService;
    }

    @Override
    public void write(Chunk<? extends Virement> chunk) {

        if (chunk.isEmpty()) {
            log.debug("No virements to write.");
            return;
        }

        log.info("Processing {} virement(s)", chunk.size());

        List<? extends Virement> saved =
                virementRepository.saveAll(chunk.getItems());

        for (Virement virement : saved) {
            commissionService.generateCommissionIfNeeded(virement);
        }

        log.info("{} virement(s) successfully processed.", saved.size());    }






}