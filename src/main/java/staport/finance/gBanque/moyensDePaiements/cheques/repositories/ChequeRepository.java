package staport.finance.gBanque.moyensDePaiements.cheques.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChequeRepository extends JpaRepository<Cheque, Long>, JpaSpecificationExecutor<Cheque> {

    /**
     * Finds a Cheque by its unique numero.
     * @param numero The unique identifier of the cheque.
     * @return An Optional containing the found Cheque, or an empty Optional if none is found.
     */
    Optional<Cheque> findByNumero(String numero);

    /** Clé de matching exclusive de la synchro FunPaiement (cf. AbstractPaymentProcessor). */
    Optional<Cheque> findByIdPaiement(String idPaiement);

    /**
     * Recherche paginée avec critères dynamiques — charge {@code banque} en une seule requête
     * (anti-N+1, nécessaire depuis le passage de {@code banque} en {@code FetchType.LAZY}).
     */
    @Override
    @EntityGraph(attributePaths = "banque")
    Page<Cheque> findAll(Specification<Cheque> spec, Pageable pageable);

    /**
     * Recherche non paginée avec critères dynamiques — utilisée pour les totaux d'encours.
     */
    @Override
    @EntityGraph(attributePaths = "banque")
    List<Cheque> findAll(Specification<Cheque> spec);
}
