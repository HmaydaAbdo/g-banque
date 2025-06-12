package staport.finance.gBanque.LigneFonctionnelle.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;

import java.util.Optional;
import java.util.List;

@Repository
public interface LigneFonctionnelleRepository extends JpaRepository<LigneFonctionnelle, Long>, JpaSpecificationExecutor<LigneFonctionnelle> {

    //  Find by banqueId and natureLigneFonctionnelleId
    Optional<LigneFonctionnelle> findByBanqueIdAndNatureLigneFonctionnelleId(Long banqueId, Long natureLigneFonctionnelleId);

    // Find all functional lines for a specific bank
    List<LigneFonctionnelle> findByBanqueId(Long banqueId);

    // Count functional lines associated with a specific NatureLigneFonctionnelle ID (for deletion check in NatureLigneFonctionnelleService)
    long countByNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId);
}
