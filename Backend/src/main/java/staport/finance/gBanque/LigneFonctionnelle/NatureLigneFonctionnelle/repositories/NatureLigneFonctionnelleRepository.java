package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.enums.PorteeNature;
import java.util.List;
import java.util.Optional;

@Repository
public interface NatureLigneFonctionnelleRepository extends JpaRepository<NatureLigneFonctionnelle, Long>, JpaSpecificationExecutor<NatureLigneFonctionnelle> {

    Optional<NatureLigneFonctionnelle> findByLabel(String label);

    Optional<NatureLigneFonctionnelle> findByCode(String code);

    boolean existsByLabel(String label);

    boolean existsByCode(String code);

    /**
     * Requête directe sur l'index idx_nlf_portee — utilisée pour les dropdowns
     * des formulaires lignes. Prend une liste de portées compatibles
     * (ex: [GENERALE, GENERALE_ET_SPECIFIQUE]) pour filtrer en une seule query.
     */
    List<NatureLigneFonctionnelle> findByPorteeIn(List<PorteeNature> portees);
}
