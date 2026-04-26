package staport.finance.gBanque.impot_taxes.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.time.LocalDate;
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
}