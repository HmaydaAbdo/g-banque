package staport.finance.gBanque.droitConstate.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DroitConstateRepository extends JpaRepository<DroitConstate, Long>, JpaSpecificationExecutor<DroitConstate> {

    List<DroitConstate> findByMarche(Marche marche);

    long countByStatus(Status status);

    /**
     * Somme des montants PAYE d'un type donné (DEBLOCAGE ou REMBOURSEMENT)
     * pour un marché, dont la dateValeur est antérieure ou égale au jour J.
     *
     * Utilisé pour reconstituer l'encours droits constatés jour par jour :
     *   Encours_j = Σ(DEBLOCAGE) − Σ(REMBOURSEMENT)
     */
    @Query("""
        SELECT COALESCE(SUM(d.montant), 0)
        FROM DroitConstate d
        WHERE d.marche = :marche
          AND d.type = :type
          AND d.status = staport.finance.gBanque.prefinancement.enums.Status.PAYE
          AND d.dateValeur <= :jour
    """)
    BigDecimal sumMontantPaye(
            @Param("marche") Marche marche,
            @Param("type") Type type,
            @Param("jour") LocalDate jour);

    @Override
    @NonNull
    @EntityGraph(value = "DroitConstate.avecAssociations")
    Page<DroitConstate> findAll(org.springframework.data.jpa.domain.Specification<DroitConstate> spec, @NonNull Pageable pageable);
}
