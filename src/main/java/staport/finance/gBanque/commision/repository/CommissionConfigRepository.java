package staport.finance.gBanque.commision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.commision.entities.CommissionConfig;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.util.List;

@Repository
public interface CommissionConfigRepository extends JpaRepository<CommissionConfig, Long> {

    /**
     * Récupère toutes les règles pour une banque (par nom) et un type de transaction donnés.
     * La comparaison du nom est insensible à la casse (navigation via FK banque.nom).
     */
    List<CommissionConfig> findByBanque_NomIgnoreCaseAndType(String banqueNom, TransactionSource type);

    /**
     * Vérifie qu'au moins une règle existe pour cette banque (par nom) et ce type.
     */
    boolean existsByBanque_NomIgnoreCaseAndType(String banqueNom, TransactionSource type);
}
