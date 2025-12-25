package staport.finance.gBanque.batch.jobs.cmt.reader;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.cmt.repositories.CreditMoyenTermeRepository;

import java.util.Map;

@Component
public class CreditMoyenTermeMainLeveeReader extends RepositoryItemReader<CreditMoyenTerme> {

    private static final Logger log = LoggerFactory.getLogger(CreditMoyenTermeMainLeveeReader.class);
    private static final int PAGE_SIZE = 50;
    private static final String READER_NAME = "creditMoyenTermeMainLeveeReader";

    public CreditMoyenTermeMainLeveeReader(CreditMoyenTermeRepository creditMoyenTermeRepository) {
        log.info("Initialisation de CreditMoyenTermeMainLeveeReader avec le repository");
        this.setRepository(creditMoyenTermeRepository);
        this.setMethodName("findByCompleted");
        this.setArguments(java.util.List.of(false));
        this.setPageSize(PAGE_SIZE);
        this.setSort(Map.of("id", Sort.Direction.ASC));
        this.setSaveState(true);
        this.setName(READER_NAME);

        log.info("{} configuré avec succès - Taille de page: {}", READER_NAME, PAGE_SIZE);
    }
}