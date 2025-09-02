package staport.finance.gBanque.FunPaiementService.batch.writer;

import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.lcn.entities.Lcn;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.virements.entities.Virement;
import staport.finance.gBanque.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.lcn.repositories.LcnRepository;
import staport.finance.gBanque.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.virements.repositories.VirementRepository;

import java.util.Map;

/**
 * Configuration class to provide a single, unified ItemWriter bean
 * that dispatches items to the correct, specialized writer.
 */
@Configuration
public class PaymentWriterConfig {

    private final ChequeRepository chequeRepository;
    private final LcnRepository lcnRepository;
    private final MiseADispositionRepository miseADispositionRepository;
    private final VirementRepository virementRepository;

    public PaymentWriterConfig(
            ChequeRepository chequeRepository,
            LcnRepository lcnRepository,
            MiseADispositionRepository miseADispositionRepository,
            VirementRepository virementRepository) {
        this.chequeRepository = chequeRepository;
        this.lcnRepository = lcnRepository;
        this.miseADispositionRepository = miseADispositionRepository;
        this.virementRepository = virementRepository;
    }

    @Bean
    public ChequeItemWriter chequeItemWriter() {
        return new ChequeItemWriter(chequeRepository);
    }

    @Bean
    public LcnItemWriter lcnItemWriter() {
        return new LcnItemWriter(lcnRepository);
    }

    @Bean
    public VirementItemWriter virementItemWriter() {
        return new VirementItemWriter(virementRepository);
    }

    @Bean
    public MiseADispositionItemWriter miseADispositionItemWriter() {
        return new MiseADispositionItemWriter(miseADispositionRepository);
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
