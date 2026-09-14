package staport.finance.gBanque.LigneFonctionnelle.config.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import staport.finance.gBanque.LigneFonctionnelle.config.entities.LigneFonctionnelleConfig;

public interface LigneFonctionnelleConfigRepository extends JpaRepository<LigneFonctionnelleConfig, Long> {
}
