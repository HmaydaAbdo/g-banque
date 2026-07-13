package staport.finance.gBanque.factoring.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FactoringRepository extends JpaRepository<Factoring, Long>, JpaSpecificationExecutor<Factoring> {

    List<Factoring> findByMarche(Marche marche);

    long countByStatus(Status status);

    /**
     * Somme des montants PAYE d'un type donné (DEBLOCAGE ou REMBOURSEMENT)
     * pour un marché, dont la dateValeur est antérieure ou égale au jour J.
     *
     * Utilisé pour reconstituer l'encours factoring jour par jour :
     *   Encours_j = Σ(DEBLOCAGE PAYE ≤ j) − Σ(REMBOURSEMENT PAYE ≤ j)
     */
    @Query("""
        SELECT COALESCE(SUM(f.montant), 0)
        FROM Factoring f
        WHERE f.marche = :marche
          AND f.type   = :type
          AND f.status = staport.finance.gBanque.prefinancement.enums.Status.PAYE
          AND f.dateValeur <= :jour
    """)
    BigDecimal sumMontantPaye(
            @Param("marche") Marche marche,
            @Param("type")   Type   type,
            @Param("jour")   LocalDate jour);

    @Override
    @NonNull
    @EntityGraph(value = "Factoring.avecAssociations")
    Page<Factoring> findAll(org.springframework.data.jpa.domain.Specification<Factoring> spec, @NonNull Pageable pageable);
}
