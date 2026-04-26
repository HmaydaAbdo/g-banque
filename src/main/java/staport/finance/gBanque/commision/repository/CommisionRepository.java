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
}
