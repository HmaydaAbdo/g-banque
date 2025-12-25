package staport.finance.gBanque.leasing.lesOrganismsLeasing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;

import java.util.Optional;

public interface OrganismeLeasingRepository extends JpaRepository<OrganismeLeasing, Long>, JpaSpecificationExecutor<OrganismeLeasing> {
    Optional<OrganismeLeasing> findByNom(String nom);
}