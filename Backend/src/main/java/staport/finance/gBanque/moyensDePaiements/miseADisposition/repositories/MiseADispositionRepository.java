package staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;

import java.util.List;
import java.util.Optional;

@Repository
public interface MiseADispositionRepository extends JpaRepository<MiseADisposition, Long>, JpaSpecificationExecutor<MiseADisposition> {

    @Override
    @EntityGraph(attributePaths = {"banque"})
    Page<MiseADisposition> findAll(Specification<MiseADisposition> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"banque"})
    List<MiseADisposition> findAll(Specification<MiseADisposition> spec);

    Optional<MiseADisposition> findByNumero(String numero);

    /** Clé de matching exclusive de la synchro FunPaiement (cf. AbstractPaymentProcessor). */
    Optional<MiseADisposition> findByIdPaiement(String idPaiement);
}
