package staport.finance.gBanque.marches.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.marches.entities.Marche;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarcheRepository  extends JpaRepository <Marche, Long>, JpaSpecificationExecutor<Marche> {

    boolean existsByNumero(String numero);

    /** Tous les marchés d'une banque — utilisé par les calculateurs d'agios multi-marché. */
    List<Marche> findByBanqueId(Long banqueId);
}
