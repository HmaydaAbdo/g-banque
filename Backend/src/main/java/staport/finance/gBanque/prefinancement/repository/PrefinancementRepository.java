package staport.finance.gBanque.prefinancement.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrefinancementRepository extends JpaRepository<Prefinancement, Long>, JpaSpecificationExecutor<Prefinancement> {

    List<Prefinancement> findByMarche(Marche marche);

    long countByStatus(Status status);

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

    @Override
    @NonNull
    @EntityGraph(value = "Prefinancement.avecAssociations")
    Page<Prefinancement> findAll(org.springframework.data.jpa.domain.Specification<Prefinancement> spec, @NonNull Pageable pageable);

    /**
     * Recherche non paginée avec chargement de l'association marché+banque en une seule requête
     * JOIN — utilisée par les exports PDF (liste complète + écrans d'encours), qui ont besoin de
     * l'intégralité des résultats filtrés sans provoquer de N+1 pendant la construction du PDF.
     */
    @NonNull
    @EntityGraph(value = "Prefinancement.avecAssociations")
    List<Prefinancement> findAll(org.springframework.data.jpa.domain.Specification<Prefinancement> spec, @NonNull Sort sort);
}
