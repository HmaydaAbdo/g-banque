package staport.finance.gBanque.prefinancement.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrefinancementRepository extends JpaRepository<Prefinancement, Long>, JpaSpecificationExecutor<Prefinancement> {

    List<Prefinancement> findByMarche(Marche marche);

    /**
     * Somme des montants PAYE d'un type donné (DEBLOCAGE ou REMBOURSEMENT)
     * pour un marché, dont la dateValeur est antérieure ou égale au jour J.
     *
     * Utilisé pour reconstituer l'encours préfinancement jour par jour :
     *   Encours_j = Σ(DEBLOCAGE) − Σ(REMBOURSEMENT)
     *
     * @return la somme, ou null si aucun enregistrement ne correspond
     */
    @Query("""
        SELECT COALESCE(SUM(p.montant), 0)
        FROM Prefinancement p
        WHERE p.marche = :marche
          AND p.type = :type
          AND p.status = staport.finance.gBanque.prefinancement.enums.Status.PAYE
          AND p.dateValeur <= :jour
    """)
    BigDecimal sumMontantPaye(
            @Param("marche") Marche marche,
            @Param("type") Type type,
            @Param("jour") LocalDate jour);
}
