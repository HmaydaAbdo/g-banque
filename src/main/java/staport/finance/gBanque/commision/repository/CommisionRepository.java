package staport.finance.gBanque.commision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CommisionRepository extends JpaRepository<Commission, Long>, JpaSpecificationExecutor<Commission> {

    List<Commission> findByLibelleLikeAndDateEmission(String libellePattern, LocalDate dateEmission);

    List<Commission> findByLibelleLike(String libellePattern);

    /**
     * Vérifie si une caution possède au moins une commission non réglée (status = PREVU).
     * Utilisé pour bloquer la désactivation d'une caution tant qu'une commission est en cours.
     */
    boolean existsByCaution_IdAndStatus(Long cautionId, Status status);

    /**
     * Garde d'idempotence : vérifie si une commission auto-générée existe déjà
     * pour cette caution à cette date d'émission exacte.
     * Empêche les doublons si le batch ou l'API est appelé plusieurs fois pour le même mois.
     */
    boolean existsByCaution_IdAndDateEmission(Long cautionId, LocalDate dateEmission);

    /**
     * Vérifie si une commission existe pour cette caution dans une plage de dates.
     * Utilisé par le batch pour détecter qu'un prorata d'entrée a déjà été créé
     * lors de la création de la caution, afin d'éviter une double facturation du
     * mois d'entrée pour les banques à facturation mois courant (AWB, CDM).
     *
     * <p>Exemple : caution AWB créée le 19/05 → prorata dateEmission=19/05.
     * Batch fin mai cherche une commission entre 01/05 et 31/05 → la trouve → skip.
     */
    boolean existsByCaution_IdAndDateEmissionBetween(Long cautionId,
                                                      LocalDate debutPeriode,
                                                      LocalDate finPeriode);

    /**
     * Récupère le prorata d'entrée PREVU d'une caution à une date d'émission exacte.
     * Utilisé à la clôture pour supprimer le prorata non encore validé si la caution
     * est clôturée dans le même mois que sa date de prise.
     */
    java.util.Optional<Commission> findByCaution_IdAndDateEmissionAndStatus(
            Long cautionId, LocalDate dateEmission, Status status);
}
