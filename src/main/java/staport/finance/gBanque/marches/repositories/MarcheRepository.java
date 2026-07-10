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

    /**
     * Marchés actifs d'une banque — utilisé par les calculateurs d'agios multi-marché et le
     * calcul d'encours des lignes fonctionnelles. Un marché archivé ({@code actif = false})
     * est exclu définitivement de ces calculs (cf. {@code MarcheService.findByBanqueId}).
     */
    List<Marche> findByBanqueIdAndActifTrue(Long banqueId);
}
