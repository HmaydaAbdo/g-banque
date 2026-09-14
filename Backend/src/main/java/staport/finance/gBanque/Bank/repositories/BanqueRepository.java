package staport.finance.gBanque.Bank.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import staport.finance.gBanque.Bank.entities.Banque;

import java.util.List;
import java.util.Optional;

public interface BanqueRepository extends JpaRepository<Banque, Long>, JpaSpecificationExecutor<Banque> {

    /** Correspondance exacte sur le numéro de compte — utilisé pour le contrôle de doublon à la création. */
    Optional<Banque> findByNumeroDeCompte(String numeroDeCompte);

    /**
     * Correspondance insensible à la casse sur le nom.
     * Utilisé par les batch processors (ChargesFixesScheduler, AbstractPaymentProcessor, VirementPaymentProcessor).
     * Note : ces appelants injectent BanqueRepository directement depuis d'autres domaines,
     * en violation de la convention d'encapsulation (CLAUDE.md). À corriger lors d'un refactoring batch.
     */
    Optional<Banque> findByNomIgnoreCase(String nom);

    @Query("SELECT b.id FROM Banque b")
    List<Long> findAllIds();
}
