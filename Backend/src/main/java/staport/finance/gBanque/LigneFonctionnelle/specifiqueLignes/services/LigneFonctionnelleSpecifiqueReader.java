package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.marches.entities.Marche;

import java.util.Optional;

/**
 * Façade de lecture cross-domaine pour LigneFonctionnelleSpecifique.
 *
 * <p>Mêmes raisons que {@code LigneFonctionnelleReader} (générale) : Préfinancement, Factoring
 * et DroitConstate ont besoin de résoudre la ligne spécifique active pour un marché/nature donné,
 * sans jamais injecter {@link LigneFonctionnelleSpecifiqueRepository} directement.
 */
@Component
public class LigneFonctionnelleSpecifiqueReader {

    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;

    public LigneFonctionnelleSpecifiqueReader(LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository) {
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
    }

    /**
     * Ligne spécifique active pour un marché et une nature donnés. Verrou PESSIMISTIC_WRITE porté
     * par la requête sous-jacente — participe à la transaction de l'appelant.
     */
    @Transactional(readOnly = true)
    public Optional<LigneFonctionnelleSpecifique> findByMarcheAndNatureLigneFonctionnelleAndActifTrue(
            Marche marche, NatureLigneFonctionnelle nature) {
        return ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, nature);
    }

    /**
     * Ligne spécifique active pour un marché et une nature, <b>sans verrou</b> : lecture pure (calcul des agios), utilisable
     * dans une transaction en lecture seule ou sans transaction englobante.
     */
    @Transactional(readOnly = true)
    public Optional<LigneFonctionnelleSpecifique> findActiveSansVerrou(Marche marche, NatureLigneFonctionnelle nature) {
        return ligneFonctionnelleSpecifiqueRepository.findActiveSansVerrou(marche, nature);
    }
}
