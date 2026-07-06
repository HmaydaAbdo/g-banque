package staport.finance.gBanque.factoring.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.marches.entities.Marche;

import java.util.List;

/**
 * Façade de lecture cross-domaine pour Factoring. Mêmes raisons que
 * {@link staport.finance.gBanque.prefinancement.services.PrefinancementReader}.
 */
@Component
public class FactoringReader {

    private final FactoringRepository factoringRepository;

    public FactoringReader(FactoringRepository factoringRepository) {
        this.factoringRepository = factoringRepository;
    }

    @Transactional(readOnly = true)
    public List<Factoring> findByMarche(Marche marche) {
        return factoringRepository.findByMarche(marche);
    }

    @Transactional(readOnly = true)
    public Page<Factoring> findAll(Specification<Factoring> spec, Pageable pageable) {
        return factoringRepository.findAll(spec, pageable);
    }
}
