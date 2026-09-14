package staport.finance.gBanque.moyensDePaiements.lcn.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;

import java.util.List;

/**
 * Façade de lecture cross-domaine pour Lcn. Mêmes raisons que
 * {@link staport.finance.gBanque.prefinancement.services.PrefinancementReader}.
 */
@Component
public class LcnReader {

    private final LcnRepository lcnRepository;

    public LcnReader(LcnRepository lcnRepository) {
        this.lcnRepository = lcnRepository;
    }

    @Transactional(readOnly = true)
    public List<Lcn> findAll(Specification<Lcn> spec) {
        return lcnRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public Page<Lcn> findAll(Specification<Lcn> spec, Pageable pageable) {
        return lcnRepository.findAll(spec, pageable);
    }
}
