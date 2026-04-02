package staport.finance.gBanque.commision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CommisionRepository extends JpaRepository<Commission, Long>, JpaSpecificationExecutor<Commission> {

    List<Commission> findByLibelleLikeAndDateEmission(String libellePattern, LocalDate dateEmission);

    List<Commission> findByLibelleLike(String libellePattern);

    /**
     * Vérifie l'existence d'une commission par son triptyque d'unicité —
     * utilisé pour garantir l'idempotence de la génération mensuelle des commissions cautions.
     *
     * @param operation    type de l'opération (ex: {@code CAUTION})
     * @param dateEmission date d'émission de la commission
     * @param libelle      libellé de la commission (nature + numéro caution)
     * @return {@code true} si une commission identique existe déjà
     */
    boolean existsByOperationAndDateEmissionAndLibelle(
            TransactionSource operation,
            LocalDate dateEmission,
            String libelle
    );
}
