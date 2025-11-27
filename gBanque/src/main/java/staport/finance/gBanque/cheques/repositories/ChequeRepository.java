package staport.finance.gBanque.cheques.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.cheques.entities.Cheque;

import java.util.Optional;

@Repository
public interface ChequeRepository extends JpaRepository<Cheque, Long>, JpaSpecificationExecutor<Cheque> {


    /**
     * Finds a Cheque by its unique numero.
     * @param numero The unique identifier of the cheque.
     * @return An Optional containing the found Cheque, or an empty Optional if none is found.
     */
    Optional<Cheque> findByNumero(String numero);
}
