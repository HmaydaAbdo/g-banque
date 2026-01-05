
package staport.finance.gBanque.Bank.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.Bank.entities.Banque;

import java.util.Optional;

@Repository
public interface BanqueRepository extends JpaRepository<Banque, Long>, JpaSpecificationExecutor<Banque> { // IMPORTANT: Extend JpaSpecificationExecutor


    Optional<Banque> findByNumeroDeCompte(String numeroDeCompte);
    Optional<Banque> findByNom(String nom);
    Optional<Banque> findByNomIgnoreCase(String nom);


}