package staport.finance.gBanque.commision.service;

import staport.finance.gBanque.commision.dtos.CommissionConfigRequestDTO;
import staport.finance.gBanque.commision.dtos.CommissionConfigResponseDTO;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Interface publique du service de configuration des commissions.
 * Permet à d'autres modules (ex : leasing) de résoudre les montants de commission
 * sans dépendre de la classe concrète ni des valeurs YAML.
 */
public interface ICommissionConfigService {

    /**
     * Résout le montant de commission applicable pour une banque, un organisme et un type.
     *
     * <p>Algorithme de résolution :
     * <ol>
     *   <li>Chercher une règle {@code SI_MATCH} : banqueNom + type + organismeNom correspondent → retourner son montant.</li>
     *   <li>Si une règle {@code SI_MATCH} existe pour cette banque mais l'organisme ne correspond pas
     *       → chercher une règle {@code SI_NON_MATCH} pour la même banque.</li>
     *   <li>Sinon → chercher une règle {@code DEFAULT} pour cette banque.</li>
     *   <li>Aucune règle → {@code Optional.empty()}.</li>
     * </ol>
     *
     * @param banqueNom    Nom de la banque (insensible à la casse)
     * @param organismeNom Nom de l'organisme — peut être null pour les types sans organisme
     * @param type         Type de transaction ({@code LEASING}, {@code Virement}, etc.)
     * @return Le montant de commission, ou {@code Optional.empty()} si aucune règle ne s'applique
     */
    Optional<BigDecimal> resolveCommission(String banqueNom, String organismeNom, TransactionSource type);

    /**
     * Vérifie qu'au moins une règle de commission existe pour cette banque et ce type.
     */
    boolean hasCommission(String banqueNom, TransactionSource type);

    // ── CRUD admin ────────────────────────────────────────────────────────────

    List<CommissionConfigResponseDTO> findAll();

    CommissionConfigResponseDTO create(CommissionConfigRequestDTO dto);

    CommissionConfigResponseDTO update(Long id, CommissionConfigRequestDTO dto);

    void delete(Long id);
}
