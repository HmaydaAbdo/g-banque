package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Cheque;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Lcn;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.MiseADisposition;
import staport.finance.gBanque.lesEtatsDesEncours.core.repositories.ChequeRepository;
import staport.finance.gBanque.lesEtatsDesEncours.core.repositories.LcnRepository;
import staport.finance.gBanque.lesEtatsDesEncours.core.repositories.MiseADispositionRepository;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class PaymentItemWriter {

    private static final Logger log = LoggerFactory.getLogger(PaymentItemWriter.class);

    private final ChequeRepository chequeRepository;
    private final LcnRepository lcnRepository;
    private final MiseADispositionRepository miseADispositionRepository;

    public PaymentItemWriter(ChequeRepository chequeRepository, LcnRepository lcnRepository, MiseADispositionRepository miseADispositionRepository) {
        this.chequeRepository = chequeRepository;
        this.lcnRepository = lcnRepository;
        this.miseADispositionRepository = miseADispositionRepository;
    }

    /**
     * Creates a single ItemWriter that handles both Cheque and Lcn entities.
     * It segregates the items from the chunk and saves them to the
     * appropriate repository.
     *
     * @return an ItemWriter for both Cheque and Lcn.
     */
    @Bean
    public ItemWriter<Object> paymentWriter() {
        return chunk -> { // Change 'items' to 'chunk' to match the parameter type
            List<? extends Object> items = chunk.getItems(); // Get the list from the chunk

            log.info("Processing chunk of {} items for writing", items.size());

            // Segregate Cheque items
            List<Cheque> cheques = items.stream()
                    .filter(Cheque.class::isInstance)
                    .map(Cheque.class::cast)
                    .collect(Collectors.toList());

            if (!cheques.isEmpty()) {
                log.info("Saving {} Cheque entities", cheques.size());
                chequeRepository.saveAll(cheques);
            }

            // Segregate Lcn items
            List<Lcn> lcns = items.stream()
                    .filter(Lcn.class::isInstance)
                    .map(Lcn.class::cast)
                    .collect(Collectors.toList());

            if (!lcns.isEmpty()) {
                log.info("Saving {} LCN entities", lcns.size());
                lcnRepository.saveAll(lcns);
            }

            List<MiseADisposition> miseADispositionList=items.stream()
                    .filter(MiseADisposition.class::isInstance)
                    .map(MiseADisposition.class::cast)
                    .collect(Collectors.toList());

            if (!miseADispositionList.isEmpty()){
                log.info("saving {} MiseADisposition entities", miseADispositionList.size());
                miseADispositionRepository.saveAll(miseADispositionList);
            }
        };
    }
}