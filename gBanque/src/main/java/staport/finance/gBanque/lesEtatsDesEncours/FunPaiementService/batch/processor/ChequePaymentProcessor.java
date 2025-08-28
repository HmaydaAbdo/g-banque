package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor;

import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Cheque;
import java.math.BigDecimal;
import java.time.ZoneId;


/**
 * ItemProcessor for 'Cheque' payments (type = 1).
 * Contains specific business logic for this payment type.
 * This processor transforms FunPaymentDto into a Cheque entity.
 * Enhanced to throw exceptions for all invalid scenarios, allowing Spring Batch to track skips.
 */
@Component
public class ChequePaymentProcessor implements ItemProcessor<FunPaymentDto, Object> {

    private static final Logger log = LoggerFactory.getLogger(ChequePaymentProcessor.class);
    ZoneId systemZone = ZoneId.systemDefault();
    private final BanqueRepository banqueRepository;

    public ChequePaymentProcessor(BanqueRepository banqueRepository) {
        this.banqueRepository = banqueRepository;
    }

    @Override
    public Cheque process(FunPaymentDto item) throws Exception {
        // Log the attempt to process for tracing
        log.debug("Attempting to process FunPaymentDto with numero: {}", item.getNumero());

        // --- Step 1: Validate critical fields from FunPaymentDto ---
        // If any required field is invalid, throw InvalidChequeDataException.
        // Spring Batch's fault-tolerant mechanism will catch this and skip the item.
        if (item.getNumero() == null || item.getNumero().trim().isEmpty()) {
            throw new InvalidChequeDataException(" numero is null or empty");
        }

        if (item.getDateCheque() == null) {
            throw new InvalidChequeDataException(" date emission (date_chaque) is null");
        }

        if (item.getBeneficiaire() == null || item.getBeneficiaire().trim().isEmpty()) {
            throw new InvalidChequeDataException(" beneficiaire is null or empty");
        }

        if (item.getMontant() == null || item.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidChequeDataException("montant is null or non-positive ");
        }


        if (item.getBanque() == null || item.getBanque().trim().isEmpty()) {
            throw new InvalidChequeDataException("banque (bank name) is null or empty");
        }

        // 2. Create and map to Cheque entity
        Cheque cheque = new Cheque();
        cheque.setNumero(item.getNumero());
        cheque.setDateEmission(item.getDateCheque().atStartOfDay(systemZone).toOffsetDateTime());
        cheque.setBeneficiaire(item.getBeneficiaire());

        cheque.setDateEcheance(
                item.getDateEcheance()!=null ?
                        item.getDateEcheance().atStartOfDay(systemZone).toOffsetDateTime()
                        : null
        );

        cheque.setMontant(item.getMontant());

        // 3. Handle Banque Association - Throwing error if not found
        String bankNameFromDto = item.getBanque();
        Banque banque = banqueRepository.findByNom(bankNameFromDto).orElseThrow(() -> new ResourceNotFoundException(String.format("Bank with name '%s' not found ", bankNameFromDto)));
        cheque.setBanque(banque);

        log.info("Successfully processed and transformed Cheque Payment {}. Returning Cheque entity.", item.getNumero());
        return cheque;
    }
}