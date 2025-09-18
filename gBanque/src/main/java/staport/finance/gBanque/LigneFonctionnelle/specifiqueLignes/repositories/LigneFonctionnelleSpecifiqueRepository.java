package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.marches.entities.Marche;

import java.util.List;
import java.util.Optional;

@Repository
public interface LigneFonctionnelleSpecifiqueRepository extends JpaRepository<LigneFonctionnelleSpecifique, Long> {
    /**
     * Finds an active specific functional line by its associated Marche and Nature.
     * This is the correct method to prevent the creation of active duplicates based on the new business rule.
     *
     * @param marche The Marche entity.
     * @param nature The NatureLigneFonctionnelle entity.
     * @return An Optional containing the active LigneFonctionnelleSpecifique if found.
     */
    Optional<LigneFonctionnelleSpecifique> findByMarcheAndNatureLigneFonctionnelleAndActifTrue(Marche marche, NatureLigneFonctionnelle nature);

    /**
     * Finds all specific functional lines for a given Marche, ordered by date.
     *
     * @param marche The Marche entity.
     * @return A list of LigneFonctionnelleSpecifique entities.
     */
    List<LigneFonctionnelleSpecifique> findByMarcheOrderByDateDepartAsc(Marche marche);

    Page<LigneFonctionnelleSpecifique> findAll(Specification<LigneFonctionnelleSpecifique> spec, Pageable pageable);

    /**
     * Finds a page of LigneFonctionnelleSpecifique by its NatureLigneFonctionnelle, with support for pagination and sorting.
     * Spring Data JPA will automatically implement this method.
     * @param natureLigneFonctionnelle The nature of the functional line.
     * @param pageable The pagination and sorting information.
     * @return A Page of LigneFonctionnelleSpecifique entities.
     */
    Page<LigneFonctionnelleSpecifique> findByNatureLigneFonctionnelle(NatureLigneFonctionnelle natureLigneFonctionnelle, Pageable pageable);
}