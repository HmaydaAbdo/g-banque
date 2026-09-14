package staport.finance.gBanque.droitConstate.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Façade de lecture cross-domaine pour DroitConstate. Mêmes raisons que
 * {@link staport.finance.gBanque.prefinancement.services.PrefinancementReader}.
 */
@Component
public class DroitConstateReader {

    private final DroitConstateRepository droitConstateRepository;

    public DroitConstateReader(DroitConstateRepository droitConstateRepository) {
        this.droitConstateRepository = droitConstateRepository;
    }

    @Transactional(readOnly = true)
    public List<DroitConstate> findByMarche(Marche marche) {
        return droitConstateRepository.findByMarche(marche);
    }

    @Transactional(readOnly = true)
    public Page<DroitConstate> findAll(Specification<DroitConstate> spec, Pageable pageable) {
        return droitConstateRepository.findAll(spec, pageable);
    }

    /**
     * Somme des montants payés d'un type donné pour un marché, jusqu'à une date incluse.
     * Utilisé par les calculateurs d'agios pour reconstituer l'encours de droits constatés.
     */
    @Transactional(readOnly = true)
    public BigDecimal sumMontantPaye(Marche marche, Type type, LocalDate jour) {
        BigDecimal total = droitConstateRepository.sumMontantPaye(marche, type, jour);
        return total != null ? total : BigDecimal.ZERO;
    }
}
