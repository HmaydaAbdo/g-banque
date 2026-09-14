package staport.finance.gBanque.cmt.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditMoyenTermeRepository extends JpaRepository<CreditMoyenTerme,Long>, JpaSpecificationExecutor<CreditMoyenTerme> {

    List<CreditMoyenTerme> findByCompleted(boolean completed);

    /**
     * Recherche non paginée — utilisée exclusivement par l'export PDF afin d'imprimer
     * l'intégralité des crédits correspondant aux filtres (pas seulement la page
     * affichée à l'écran). {@code @EntityGraph} évite le N+1 sur {@code banque.nom}.
     */
    @EntityGraph(attributePaths = {"banque"})
    List<CreditMoyenTerme> findAll(Specification<CreditMoyenTerme> spec, Sort sort);

    /**
     * Trouve tous les crédits CMT selon leur statut d'archivage avec pagination.
     * Utile pour le batch job qui traite les crédits par chunks.
     *
     * @param completed true pour les crédits archivés, false pour les crédits actifs
     * @param pageable Informations de pagination
     * @return Page de crédits CMT correspondants
     */
    Page<CreditMoyenTerme> findByCompleted(boolean completed, Pageable pageable);

    /**
     * Utilisé par les métriques Gauge (metrics.md) — évite de charger les entités juste pour compter.
     */
    long countByCompleted(boolean completed);

}
