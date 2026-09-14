package staport.finance.gBanque.moyensDePaiements.batch.writer;

import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import staport.finance.gBanque.commision.service.CommissionService;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.cheques.services.ChequeBatchGateway;
import staport.finance.gBanque.moyensDePaiements.lcn.services.LcnBatchGateway;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.services.MiseADispositionBatchGateway;
import staport.finance.gBanque.moyensDePaiements.virements.service.VirementBatchGateway;
import java.util.Map;

/**
 * Configuration class to provide a single, unified ItemWriter bean
 * that dispatches items to the correct, specialized writer.
 */
@Configuration
public class PaymentWriterConfig {

    private final ChequeBatchGateway chequeBatchGateway;
    private final LcnBatchGateway lcnBatchGateway;
    private final MiseADispositionBatchGateway miseADispositionBatchGateway;
    private final VirementBatchGateway virementBatchGateway;
    private final CommissionService commissionService;



    public PaymentWriterConfig(
            ChequeBatchGateway chequeBatchGateway,
            LcnBatchGateway lcnBatchGateway,
            MiseADispositionBatchGateway miseADispositionBatchGateway,
            VirementBatchGateway virementBatchGateway, CommissionService commissionService) {
        this.chequeBatchGateway = chequeBatchGateway;
        this.lcnBatchGateway = lcnBatchGateway;
        this.miseADispositionBatchGateway = miseADispositionBatchGateway;
        this.virementBatchGateway = virementBatchGateway;
        this.commissionService = commissionService;
    }

    @Bean
    public ChequeItemWriter chequeItemWriter() {
        return new ChequeItemWriter(chequeBatchGateway);
    }

    @Bean
    public LcnItemWriter lcnItemWriter() {
        return new LcnItemWriter(lcnBatchGateway, commissionService);
    }

    @Bean
    public VirementItemWriter virementItemWriter() {
        return new VirementItemWriter(virementBatchGateway, commissionService);
    }

    @Bean
    public MiseADispositionItemWriter miseADispositionItemWriter() {
        return new MiseADispositionItemWriter(miseADispositionBatchGateway, commissionService);
    }

    /**
     * Creates a single dispatching ItemWriter.
     * This writer is configured with a map of specific writers for each
     * entity type. It iterates through the chunk once and delegates
     * the writing to the appropriate specialized writer.
     * @return a single ItemWriter that can handle all payment entities.
     */
    @Bean
    public ItemWriter<Object> paymentWriter() {
        Map<Class<?>, ItemWriter<?>> writers = Map.of(
                Cheque.class, chequeItemWriter(),
                Lcn.class, lcnItemWriter(),
                Virement.class, virementItemWriter(),
                MiseADisposition.class, miseADispositionItemWriter()
        );
        return new DispatchingPaymentWriter(writers);
    }
}
