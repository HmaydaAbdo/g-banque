package staport.finance.gBanque.moyensDePaiements.lecture;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Façade de lecture cross-domaine d'un type de moyen de paiement. Une implémentation par module
 * ({@code ChequeReader}, {@code LcnReader}, {@code VirementReader}) ; le domaine appelant les rassemble
 * par {@link #type()} sans aucun {@code switch}.
 */
public interface MoyenPaiementReader {

    TypeMoyenPaiement type();

    Optional<MoyenPaiementResume> findById(Long id);

    /** Décaissements non annulés d'un montant exact — candidats au règlement d'une avance. */
    List<MoyenPaiementResume> findDecaissementsNonAnnulesDeMontant(BigDecimal montant);
}
