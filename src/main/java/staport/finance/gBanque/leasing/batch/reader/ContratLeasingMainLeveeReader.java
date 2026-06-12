package staport.finance.gBanque.leasing.batch.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;

import java.util.Map;

@Component
public class ContratLeasingMainLeveeReader extends RepositoryItemReader<ContratLeasing> {

    private static final Logger log = LoggerFactory.getLogger(ContratLeasingMainLeveeReader.class);
    private static final int PAGE_SIZE = 50;
    private static final String READER_NAME = "contratLeasingMainLeveeReader";

    public ContratLeasingMainLeveeReader(ContratLeasingRepository contratLeasingRepository) {
        log.info("ContratLeasingMainLeveeReader initialized with repository.");

        this.setRepository(contratLeasingRepository);
        this.setMethodName("findByCompleted");
        this.setArguments(java.util.List.of(false));
        this.setPageSize(PAGE_SIZE);
        this.setSort(Map.of("id", Sort.Direction.ASC));
        this.setSaveState(true);
        this.setName(READER_NAME);

        log.info("{} configured successfully with page size: {}", READER_NAME, PAGE_SIZE);
    }
}