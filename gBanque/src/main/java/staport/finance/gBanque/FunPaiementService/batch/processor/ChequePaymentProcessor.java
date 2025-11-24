package staport.finance.gBanque.FunPaiementService.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.FunPaiementService.dtos.FunPaymentDto;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.cheques.repositories.ChequeRepository;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Optional;

/**
 * ItemProcessor for 'Cheque' payments (type = 1).
 * This class now extends AbstractPaymentProcessor to inherit common logic.
 * It focuses solely on the specific business logic for Cheque payments.
 */
@Component
public class ChequePaymentProcessor extends AbstractPaymentProcessor {

    private final ChequeRepository chequeRepository;
    private final OffsetDateTime currentDateTime = OffsetDateTime.now();

    public ChequePaymentProcessor(BanqueRepository banqueRepository, ChequeRepository chequeRepository) {
        super(banqueRepository);
        this.chequeRepository = chequeRepository;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) {

        // Check if cheque already exists by numero
        Optional<Cheque> existingCheque = chequeRepository.findById(item.getNumero());

        if (existingCheque.isPresent()) {
            log.info("Cheque with numero {} already exists. Skipping.", item.getNumero());
            return null; // Spring Batch will skip this item
        }

        // If not found, create a new cheque
        Cheque cheque = new Cheque();
        cheque.setNumero(item.getNumero());
        OffsetDateTime dateEmission=item.getDateCreation().atStartOfDay(systemZone).toOffsetDateTime();
        cheque.setDateEmission(dateEmission);
        cheque.setBeneficiaire(item.getBeneficiaire());
        cheque.setMontant(item.getMontant());
        cheque.setBanque(banque);

        cheque.setDateEcheance(
                item.getDateCheque() != null ?
                        item.getDateCheque().atStartOfDay(systemZone).toOffsetDateTime() :
                        null
        );

        YearMonth currentMonth = YearMonth.from(currentDateTime);
        YearMonth emissionMonth = YearMonth.from(dateEmission);

        YearMonth twoMonthThreshold = currentMonth.minusMonths(1);


        // Business rule: inactive if cheque emission month is before current month
        if (emissionMonth.isBefore(twoMonthThreshold)) {
            cheque.setActive(false);
            log.info("Cheque {} marked as INACTIVE (emission month {} is before current month {}).",
                    item.getNumero(), emissionMonth, currentMonth);
        } else {
            cheque.setActive(true);
            log.info("Cheque {} marked as ACTIVE (emission month {} is current or future month {}).",
                    item.getNumero(), emissionMonth, currentMonth);
        }

        log.info("Successfully processed and transformed Cheque Payment {}. Returning Cheque entity.", item.getNumero());
        return cheque;
    }
}
