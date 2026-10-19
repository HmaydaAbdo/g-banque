package staport.finance.gBanque.moyensDePaiements.lcn.repositories;

import java.math.BigDecimal;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LcnRepository extends JpaRepository<Lcn, Long>, JpaSpecificationExecutor<Lcn> {

    /** Moyens de paiement du sens donné, d'un montant exact, dont le statut n'est pas {@code statut} — règlement d'une avance de leasing. */
    List<Lcn> findByTypeAndMontantAndStatutNot(Type type, BigDecimal montant, StatutMoyenPaiement statut);

    Optional<Lcn> findByNumero(String numero);

    /** Clé de matching exclusive de la synchro FunPaiement (cf. AbstractPaymentProcessor). */
    Optional<Lcn> findByIdPaiement(String idPaiement);

    /** {@code banque} est LAZY — évite le N+1 sur les listes paginées (jpa.md). */
    @Override
    @EntityGraph(attributePaths = "banque")
    Page<Lcn> findAll(Specification<Lcn> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "banque")
    List<Lcn> findAll(Specification<Lcn> spec);

    /**
     * Retourne (banqueId, count) des LCN échus (ENCOURS et dateEcheance < today),
     * groupés par banque.
     */
    @Query("""
        SELECT l.banque.id, COUNT(l)
        FROM Lcn l
        WHERE l.statut = 'ENCOURS'
          AND l.dateEcheance < :today
        GROUP BY l.banque.id
    """)
    List<Object[]> countEchusGroupedByBanque(@Param("today") LocalDate today);

    /** Nombre de LCN échus pour une banque donnée — utilisé par l'agrégateur accueil banque unique. */
    @Query("""
        SELECT COUNT(l)
        FROM Lcn l
        WHERE l.banque.id = :banqueId
          AND l.statut = 'ENCOURS'
          AND l.dateEcheance < :today
    """)
    long countEchusByBanqueId(@Param("banqueId") Long banqueId, @Param("today") LocalDate today);
}
