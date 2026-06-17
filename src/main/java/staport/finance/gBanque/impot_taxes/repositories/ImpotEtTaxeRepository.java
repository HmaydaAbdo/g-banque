package staport.finance.gBanque.impot_taxes.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImpotEtTaxeRepository extends JpaRepository<ImpotEtTaxe, Long>, JpaSpecificationExecutor<ImpotEtTaxe> {

    List<ImpotEtTaxe> findByLibelleLikeAndDateEmission(String libellePattern, LocalDate dateEmission);

    /**
     * Retrouve la TVA liée à une commission donnée.
     * Utilisé pour mettre à jour le montant TVA quand le montant de la commission change.
     */
    Optional<ImpotEtTaxe> findByCommission_Id(Long commissionId);

    /**
     * Vérifie si une caution possède au moins une TVA non réglée (status = PREVU)
     * pour un motif donné.
     * Utilisé pour bloquer la désactivation d'une caution tant que sa TVA est en cours.
     */
    boolean existsByCaution_IdAndMotifAndStatus(Long cautionId, Motif motif, Status status);

    /**
     * Supprime en une seule requête toutes les TVA liées aux commissions dont les IDs
     * sont fournis. Utilisé lors de la fusion de commissions pour purger les TVA sources.
     */
    @Modifying
    @Query("DELETE FROM ImpotEtTaxe i WHERE i.commission.id IN :commissionIds")
    void deleteByCommissionIdIn(@Param("commissionIds") Collection<Long> commissionIds);

    /**
     * Vérifie si un impôt/taxe avec le même libellé a déjà été généré pour
     * cette banque dans la fenêtre de dates donnée. Utilisé pour l'idempotence
     * du scheduler des charges fixes.
     */
    boolean existsByBanqueIdAndLibelleAndDateEmissionBetween(
            Long banqueId, String libelle, LocalDate start, LocalDate end);
}