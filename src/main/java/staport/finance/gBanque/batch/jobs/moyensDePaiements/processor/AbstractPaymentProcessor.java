package staport.finance.gBanque.batch.jobs.moyensDePaiements.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Abstract base class for all payment processors.
 * This class handles common logic such as basic validation and bank lookup,
 * reducing code duplication in the specific processor implementations.
 */
public abstract class AbstractPaymentProcessor implements ItemProcessor<FunPaymentDto, Object> {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final ZoneId systemZone = ZoneId.of("Africa/Casablanca");
    protected final BanqueRepository banqueRepository;

    public AbstractPaymentProcessor(BanqueRepository banqueRepository) {
        this.banqueRepository = banqueRepository;
    }

    @Override
    public final Object process(FunPaymentDto item) throws Exception {
        log.debug("Starting common validation for item with numero: {}", item.getNumero());
        validateCommonFields(item);
        log.debug("Common validation passed for item: {}", item.getNumero());

        Optional<Banque> optionalBanque = banqueRepository.findByNomIgnoreCase(item.getBanque());
        Banque banque = optionalBanque.orElseThrow(() -> new ResourceNotFoundException(
                String.format("Bank with name '%s' not found for item with numero '%s'", item.getBanque(), item.getNumero())
        ));

        log.debug("Bank '{}' found. Now delegating to specific processor logic.", banque.getNom());
        return processSpecific(item, banque);
    }

    /**
     * Validates fields common to all payment types.
     * @param item The payment DTO to validate.
     * @throws InvalidChequeDataException if any common field is invalid.
     */
    protected void validateCommonFields(FunPaymentDto item) {
        if (item.getNumero() == null || item.getNumero().trim().isEmpty()) {
            throw new InvalidChequeDataException("Payment numero is null or empty.");
        }
        if (item.getDateCreation() == null) {
            throw new InvalidChequeDataException("Date d'émission (date_creation) is null for payment numero " + item.getNumero());
        }
//        if (item.getBeneficiaire() == null || item.getBeneficiaire().trim().isEmpty()) {
//            throw new InvalidChequeDataException("Beneficiaire is null or empty for payment numero " + item.getNumero());
//        }
        if (item.getMontant() == null || item.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidChequeDataException("Montant is null or non-positive for payment numero " + item.getNumero());
        }
        if (item.getBanque() == null || item.getBanque().trim().isEmpty()) {
            throw new InvalidChequeDataException("Banque (bank name) is null or empty for payment numero " + item.getNumero());
        }
    }

    /**
     * Abstract method for specific payment type processing.
     * Subclasses must implement this to perform their unique business logic.
     * @param item The validated FunPaymentDto.
     * @param banque The Banque entity found.
     * @return The specific entity type (e.g., Cheque, Virement).
     * @throws Exception For any specific processing errors.
     */
    protected abstract Object processSpecific(FunPaymentDto item, Banque banque) throws Exception;
}
