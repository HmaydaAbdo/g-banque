package staport.finance.gBanque.prefinancement.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;

import java.util.List;

/**
 * Façade de lecture cross-domaine pour Préfinancement.
 *
 * <p>{@code LigneFonctionnelleService}/{@code LigneFonctionnelleSpecifiqueService} ont besoin
 * d'agréger les opérations de préfinancement pour les écrans de suivi d'une ligne, sans jamais
 * injecter {@link PrefinancementRepository} directement (encapsulation des domaines,
 * architecture.md). N'injecte aucun Service — aucun risque de cycle avec
 * {@code ILigneFonctionnelleService}/{@code ILigneFonctionnelleSpecifiqueService}.
 */
@Component
public class PrefinancementReader {

    private final PrefinancementRepository prefinancementRepository;

    public PrefinancementReader(PrefinancementRepository prefinancementRepository) {
        this.prefinancementRepository = prefinancementRepository;
    }

    @Transactional(readOnly = true)
    public List<Prefinancement> findByMarche(Marche marche) {
        return prefinancementRepository.findByMarche(marche);
    }

    @Transactional(readOnly = true)
    public Page<Prefinancement> findAll(Specification<Prefinancement> spec, Pageable pageable) {
        return prefinancementRepository.findAll(spec, pageable);
    }
}
