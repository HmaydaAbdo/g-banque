package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories;


import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.marches.entities.Marche;
import java.util.List;
import java.util.Optional;

@Repository
public interface LigneFonctionnelleSpecifiqueRepository extends JpaRepository<LigneFonctionnelleSpecifique, Long> {
    /**
     * Recherche une ligne spécifique active pour un marché et une nature donnés.
     * PESSIMISTIC_WRITE : SELECT ... FOR UPDATE — garantit qu'aucune autre transaction concurrente
     * ne peut créer une ligne dupliquée entre le moment de la vérification et le commit.
     *
     * @param marche Le Marche associé.
     * @param nature La NatureLigneFonctionnelle associée.
     * @return Un Optional contenant la ligne active si elle existe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LigneFonctionnelleSpecifique> findByMarcheAndNatureLigneFonctionnelleAndActifTrue(Marche marche, NatureLigneFonctionnelle nature);


    Page<LigneFonctionnelleSpecifique> findAll(Specification<LigneFonctionnelleSpecifique> spec, Pageable pageable);

    /**
     * Variante non paginée réservée à l'export PDF — avec fetch anticipé des relations affichées
     * (marché, banque du marché, nature) pour éviter le N+1/LazyInitializationException hors
     * transaction, mirroring {@code LigneFonctionnelleRepository.findAll(Specification, Sort)}.
     */
    @EntityGraph(attributePaths = {"marche", "marche.banque", "natureLigneFonctionnelle"})
    List<LigneFonctionnelleSpecifique> findAll(Specification<LigneFonctionnelleSpecifique> spec, Sort sort);

    /**
     * Vérifie l'existence d'au moins une ligne fonctionnelle spécifique référençant cette nature.
     * Utilisé comme guard de suppression par {@code NatureLigneFonctionnelleService}.
     */
    boolean existsByNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId);
}