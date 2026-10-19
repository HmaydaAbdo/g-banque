package staport.finance.gBanque.moyensDePaiements.virements.repositories;

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
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VirementRepository extends JpaRepository<Virement, Long>, JpaSpecificationExecutor<Virement> {

    /** Moyens de paiement du sens donné, d'un montant exact, dont le statut n'est pas {@code statut} — règlement d'une avance de leasing. */
    List<Virement> findByTypeAndMontantAndStatutNot(Type type, BigDecimal montant, StatutMoyenPaiement statut);

    boolean existsByNumero(String numero);

    Optional<Virement> findByNumero(String numero);

    /** Clé de matching exclusive de la synchro FunPaiement (cf. AbstractPaymentProcessor). */
    Optional<Virement> findByIdPaiement(String idPaiement);

    /** {@code banqueEmettrice}/{@code banqueBeneficiare} sont LAZY — évite le N+1 sur les listes
     *  paginées (jpa.md). */
    @Override
    @EntityGraph(attributePaths = {"banqueEmettrice", "banqueBeneficiare"})
    Page<Virement> findAll(Specification<Virement> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"banqueEmettrice", "banqueBeneficiare"})
    List<Virement> findAll(Specification<Virement> spec);

    /**
     * Retourne (banqueId, count) des virements échus (ENCOURS et dateEmission < today),
     * groupés par la banque concernée selon le type :
     *  - DECAISSEMENT → banqueEmettrice (notre banque émet)
     *  - ENCAISSEMENT → banqueBeneficiare (notre banque reçoit)
     */
    @Query("""
        SELECT
            CASE v.type
                WHEN 'DECAISSEMENT' THEN v.banqueEmettrice.id
                WHEN 'ENCAISSEMENT' THEN v.banqueBeneficiare.id
            END,
            COUNT(v)
        FROM Virement v
        WHERE v.statut = 'ENCOURS'
          AND v.dateEmission < :today
          AND (
              (v.type = 'DECAISSEMENT' AND v.banqueEmettrice IS NOT NULL)
              OR
              (v.type = 'ENCAISSEMENT' AND v.banqueBeneficiare IS NOT NULL)
          )
        GROUP BY
            CASE v.type
                WHEN 'DECAISSEMENT' THEN v.banqueEmettrice.id
                WHEN 'ENCAISSEMENT' THEN v.banqueBeneficiare.id
            END
    """)
    List<Object[]> countEchusGroupedByBanque(@Param("today") LocalDate today);

    /** Nombre de virements échus pour une banque donnée — utilisé par l'agrégateur accueil banque unique. */
    @Query("""
        SELECT COUNT(v)
        FROM Virement v
        WHERE v.statut = 'ENCOURS'
          AND v.dateEmission < :today
          AND (
              (v.type = 'DECAISSEMENT' AND v.banqueEmettrice.id = :banqueId)
              OR
              (v.type = 'ENCAISSEMENT' AND v.banqueBeneficiare.id = :banqueId)
          )
    """)
    long countEchusByBanqueId(@Param("banqueId") Long banqueId, @Param("today") LocalDate today);
}
