package staport.finance.gBanque.impot_taxes.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ImpotEtTaxeRepository extends JpaRepository<ImpotEtTaxe, Long>, JpaSpecificationExecutor<ImpotEtTaxe> {
    List<ImpotEtTaxe> findByLibelleLikeAndDateEmission(String libellePattern, LocalDate dateEmission);
}