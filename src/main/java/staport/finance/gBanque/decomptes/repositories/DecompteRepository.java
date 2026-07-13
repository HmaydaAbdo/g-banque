package staport.finance.gBanque.decomptes.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.util.List;

@Repository
public interface DecompteRepository extends JpaRepository<Decompte, Long>, JpaSpecificationExecutor<Decompte> {

    /**
     * Recherche paginée avec chargement du marché, de sa banque et de la banque d'encaissement
     * en une seule requête JOIN — évite le N+1 lors du mapping vers {@code DecompteResponseDto}.
     */
    @EntityGraph(value = "Decompte.avecAssociations")
    Page<Decompte> findAll(Specification<Decompte> spec, Pageable pageable);

    /**
     * Recherche non paginée avec critères dynamiques — utilisée exclusivement par l'export PDF
     * (toutes les lignes filtrées, pas seulement la page affichée à l'écran).
     */
    @EntityGraph(value = "Decompte.avecAssociations")
    List<Decompte> findAll(Specification<Decompte> spec, Sort sort);

    /**
     * Compte les décomptes par statut — utilisé par {@code DecompteMetrics}
     * ({@code gbanque.decompte.prevu.count} / {@code gbanque.decompte.paye.count}).
     */
    long countByStatus(Status status);
}
