package staport.finance.gBanque.moyensDePaiements.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.service.VirementBatchGateway;
import staport.finance.gBanque.commision.service.CommissionService;
import java.util.ArrayList;
import java.util.List;

public class VirementItemWriter implements ItemWriter<Virement> {

    private static final Logger log = LoggerFactory.getLogger(VirementItemWriter.class);


    private final VirementBatchGateway virementBatchGateway;
    private final CommissionService commissionService;

    public VirementItemWriter(
            VirementBatchGateway virementBatchGateway,
            CommissionService commissionService) {
        this.virementBatchGateway = virementBatchGateway;
        this.commissionService = commissionService;
    }

    @Override
    public void write(Chunk<? extends Virement> chunk) {

        if (chunk.isEmpty()) {
            log.debug("No virements to write.");
            return;
        }

        log.info("Processing {} virement(s)", chunk.size());

        List<Virement> saved =
                virementBatchGateway.saveAll(new ArrayList<>(chunk.getItems()));

        for (Virement virement : saved) {
            commissionService.generateCommissionIfNeeded(virement);
        }

        log.info("{} virement(s) successfully processed.", saved.size());    }






}