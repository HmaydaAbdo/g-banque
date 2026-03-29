package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import java.util.Optional;

@Repository
public interface NatureLigneFonctionnelleRepository extends JpaRepository<NatureLigneFonctionnelle, Long>, JpaSpecificationExecutor<NatureLigneFonctionnelle> {


    Optional<NatureLigneFonctionnelle> findByLabel(String label);

    Optional<NatureLigneFonctionnelle> findByCode(String code);

    boolean existsByLabel(String label);

    boolean existsByCode(String code);
}
