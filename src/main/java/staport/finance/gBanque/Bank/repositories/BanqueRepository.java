package staport.finance.gBanque.Bank.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import staport.finance.gBanque.Bank.entities.Banque;

import java.util.List;
import java.util.Optional;

public interface BanqueRepository  extends JpaRepository<Banque, Long>, JpaSpecificationExecutor<Banque> {



    /** Exact match on account number — used for duplicate-check on create. */
    Optional<Banque> findByNumeroDeCompte(String numeroDeCompte);

    /** Case-insensitive match on name — used for duplicate-check on create/update. */
    Optional<Banque> findByNomIgnoreCase(String nom);

    @Query("SELECT b.id FROM Banque b")
    List<Long> findAllIds();
}
