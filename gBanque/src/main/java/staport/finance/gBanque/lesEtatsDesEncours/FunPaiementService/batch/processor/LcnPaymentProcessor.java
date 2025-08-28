package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Lcn;

import java.math.BigDecimal;
import java.time.ZoneId;

/**
 * ItemProcessor for 'LCN' payments (type = 2).
 * Contains specific business logic for this payment type.
 * This processor transforms FunPaymentDto into a LCN entity.
 * Enhanced to throw exceptions for all invalid scenarios, allowing Spring Batch to track skips.
 */
@Component
public class LcnPaymentProcessor implements ItemProcessor<FunPaymentDto, Object> {

    private static final Logger log = LoggerFactory.getLogger(LcnPaymentProcessor.class);
    ZoneId systemZone = ZoneId.systemDefault();
    private final BanqueRepository banqueRepository;

    public LcnPaymentProcessor(BanqueRepository banqueRepository) {
        this.banqueRepository = banqueRepository;
    }

    @Override
    public Lcn process(FunPaymentDto item) throws Exception {
        // Log the attempt to process for tracing
        log.debug("Attempting to process FunPaymentDto with numero: {}", item.getNumero());

        // --- Step 1: Validate critical fields from FunPaymentDto ---
        // If any required field is invalid, throw InvalidChequeDataException.
        // Spring Batch's fault-tolerant mechanism will catch this and skip the item.
        if (item.getNumero() == null || item.getNumero().trim().isEmpty()) {
            throw new InvalidChequeDataException(" numero is null or empty");
        }

        if (item.getDateCheque() == null) {
            throw new InvalidChequeDataException(" date d'émission (date_chaque) is null");
        }

        if (item.getBeneficiaire() == null || item.getBeneficiaire().trim().isEmpty()) {
            throw new InvalidChequeDataException(" beneficiaire is null or empty");
        }

        if (item.getDateEcheance() == null) {
            throw new InvalidChequeDataException(" date d'échéance (date_echeance) is null");
        }


        if (item.getMontant() == null || item.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidChequeDataException("montant is null or non-positive ");
        }


        if (item.getBanque() == null || item.getBanque().trim().isEmpty()) {
            throw new InvalidChequeDataException("banque (bank name) is null or empty");
        }

        // 2. Create and map to  entity
        Lcn lcn = new Lcn();
        lcn.setNumero(item.getNumero());
        lcn.setDateEmission(item.getDateCheque().atStartOfDay(systemZone).toOffsetDateTime());
        lcn.setBeneficiaire(item.getBeneficiaire());
        lcn.setDateEcheance(item.getDateEcheance().atStartOfDay(systemZone).toOffsetDateTime());
        lcn.setMontant(item.getMontant());
        // 3. Handle Banque Association - Throwing error if not found
        String bankNameFromDto = item.getBanque();
        Banque banque = banqueRepository.findByNom(bankNameFromDto).orElseThrow(() -> new ResourceNotFoundException(String.format("Bank with name '%s' not found ", bankNameFromDto)));
        lcn.setBanque(banque);

        log.info("Successfully processed and transformed lcn Payment {}. Returning lcn entity.", item.getNumero());
        return lcn; // Return the transformed lcn entity
    }
}
