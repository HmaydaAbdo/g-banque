package staport.finance.gBanque.moyensDePaiements.lecture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Façade cross-domaine d'un type de moyen de paiement. Une implémentation par module
 * ({@code ChequeAvanceReader}, {@code LcnAvanceReader}, {@code VirementAvanceReader}) ; le domaine appelant les
 * rassemble par {@link #type()} sans aucun {@code switch}.
 */
public interface MoyenPaiementReader {

    TypeMoyenPaiement type();

    Optional<MoyenPaiementResume> findById(Long id);

    /**
     * Décaissements non annulés (réglés ou en cours), les plus récents d'abord — candidats au règlement d'une avance.
     *
     * @param recherche texte cherché dans le numéro ou le montant ({@code null} ou vide : pas de filtre)
     * @param idsExclus moyens de paiement à écarter (déjà rattachés à une autre avance)
     */
    Page<MoyenPaiementResume> findDecaissementsNonAnnules(String recherche, Collection<Long> idsExclus, Pageable pageable);

    /**
     * Réécrit le libellé et l'observation du moyen de paiement (ex. ajout ou retrait de la mention d'avance) ;
     * un opérateur qui renvoie le texte tel quel ne change rien.
     */
    void modifierTextes(Long id, UnaryOperator<String> libelle, UnaryOperator<String> observation);
}
