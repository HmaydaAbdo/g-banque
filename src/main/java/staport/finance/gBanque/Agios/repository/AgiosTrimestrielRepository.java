package staport.finance.gBanque.Agios.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.Agios.entities.AgiosTrimestriel;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Agios.enums.StatutTrimestre;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgiosTrimestrielRepository extends JpaRepository<AgiosTrimestriel, Long>, JpaSpecificationExecutor<AgiosTrimestriel> {

    Optional<AgiosTrimestriel> findByTrimestreAndAnneeAndBanqueIdAndTypeAgios(
            int trimestre, int annee, Long banqueId, TypeAgios typeAgios);

    List<AgiosTrimestriel> findByBanqueIdAndTypeAgiosOrderByAnneeDescTrimestreDesc(
            Long banqueId, TypeAgios typeAgios);

    List<AgiosTrimestriel> findByBanqueIdAndTrimestreAndAnnee(
            Long banqueId, int trimestre, int annee);

    List<AgiosTrimestriel> findByStatut(StatutTrimestre statut);

    List<AgiosTrimestriel> findAllByOrderByAnneeDescTrimestreDesc();
}