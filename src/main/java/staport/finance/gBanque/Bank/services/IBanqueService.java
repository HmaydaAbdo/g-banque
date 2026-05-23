package staport.finance.gBanque.Bank.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import staport.finance.gBanque.Bank.dtos.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.dtos.BanqueSearchCriteria;
import staport.finance.gBanque.Bank.entities.Banque;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service contract for the Banque module.
 * Controllers must inject this interface (DIP), never the concrete class.
 * All operations that read the live balance read it from the Transaction table
 * (soldeDepart + SUM confirmed credits - SUM confirmed debits); the removed
 * soldeActuel column on the entity is no longer involved.
 */
public interface IBanqueService {

    /**
     * Creates a new banque. The logo file is mandatory.
     */
    BanqueResponse createBanque(BanqueRequest request, MultipartFile logoFile);

    /**
     * Returns a single banque by ID, with its live computed balance.
     */
    BanqueResponse getBanqueById(Long id);

    /**
     * Returns a paginated, filtered list of banques.
     */
    Page<BanqueResponse> getAllBanques(BanqueSearchCriteria criteria, Pageable pageable);

    /**
     * Updates name, devise, address, and agence. Logo is optional on update.
     */
    BanqueResponse updateBanque(Long id, BanqueRequest request, MultipartFile logoFile);

    /**
     * Deletes a banque only if all three guards pass:
     * <ol>
     *   <li>No active lignes fonctionnelles</li>
     *   <li>No pending (unconfirmed) transactions</li>
     *   <li>Computed balance == 0</li>
     * </ol>
     *
     * @throws staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException if not found
     * @throws staport.finance.gBanque.Bank.exceptions.BanqueDeletionException if a guard fails
     */
    void deleteBanque(Long id);

    /**
     * Returns accueil summary cards for ALL banques (live balance + FC plafond).
     */
    List<BankAcceuilResponse> getAccueilBankInfo();

    /**
     * Returns the accueil summary card for a single banque.
     */
    BankAcceuilResponse getAccueilBankInfoById(Long banqueId);

    /**
     * Returns the plafond of the active Facilité de Caisse ligne for the given banque,
     * or {@link BigDecimal#ZERO} if none exists.
     */
    BigDecimal getPlafondFC(Long banqueId);

    List<Long> getAllBanqueIds();

    Banque findOrThrow(Long id);
}
