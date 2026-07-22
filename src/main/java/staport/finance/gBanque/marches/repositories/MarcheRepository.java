package staport.finance.gBanque.marches.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.marches.entities.Marche;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarcheRepository  extends JpaRepository <Marche, Long>, JpaSpecificationExecutor<Marche> {

    boolean existsByNumero(String numero);

    /**
     * Recherche par ID avec chargement de la banque en une seule requête — utilisée pour le
     * détail ({@code getMarcheById}), évite une requête LAZY supplémentaire lors du mapping.
     */
    @EntityGraph(value = "Marche.avecBanque")
    @Query("SELECT m FROM Marche m WHERE m.id = :id")
    Optional<Marche> findByIdAvecBanque(@Param("id") Long id);

    /**
     * Vérifie l'unicité du numéro en excluant le marché courant — utilisé lors de la
     * modification (un marché ne doit pas se voir refuser sa propre valeur inchangée).
     */
    boolean existsByNumeroAndIdNot(String numero, Long id);

    /**
     * Marchés actifs d'une banque — utilisé par les calculateurs d'agios multi-marché et le
     * calcul d'encours des lignes fonctionnelles. Un marché archivé ({@code actif = false})
     * est exclu définitivement de ces calculs (cf. {@code MarcheService.findByBanqueId}).
     */
    List<Marche> findByBanqueIdAndActifTrue(Long banqueId);

    /**
     * Recherche paginée avec critères dynamiques et chargement de la banque en une seule
     * requête JOIN — l'{@code @EntityGraph} évite une requête N+1 par marché lors du mapping
     * vers {@code MarcheResponseDTO} (accès à {@code banque.nom}/{@code banque.id}).
     */
    @EntityGraph(value = "Marche.avecBanque")
    Page<Marche> findAll(Specification<Marche> spec, Pageable pageable);

    /**
     * Recherche non paginée avec critères dynamiques et chargement de la banque en une seule
     * requête JOIN — utilisée par l'export PDF, qui a besoin de l'intégralité des résultats
     * filtrés (pas seulement la page affichée à l'écran) sans provoquer de N+1 sur
     * {@code banque.nom} pendant la construction du tableau.
     */
    @EntityGraph(value = "Marche.avecBanque")
    List<Marche> findAll(Specification<Marche> spec, Sort sort);

    /**
     * Compte les marchés par statut d'archivage — utilisé par {@code MarcheMetrics}
     * ({@code gbanque.marche.actif.count} / {@code gbanque.marche.archive.count}).
     */
    long countByActif(boolean actif);
}
