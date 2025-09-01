package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;
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
    private final OffsetDateTime curentDateTime = OffsetDateTime.now();

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
        OffsetDateTime dateEmission=item.getDateCheque().atStartOfDay(systemZone).toOffsetDateTime();
        cheque.setDateEmission(dateEmission);
        cheque.setBeneficiaire(item.getBeneficiaire());
        cheque.setMontant(item.getMontant());
        cheque.setBanque(banque);

        cheque.setDateEcheance(
                item.getDateEcheance() != null ?
                        item.getDateEcheance().atStartOfDay(systemZone).toOffsetDateTime() :
                        null
        );

        // Get current month (YearMonth is handy for comparisons)
        YearMonth currentMonth = YearMonth.from(curentDateTime);
        YearMonth emissionMonth = YearMonth.from(dateEmission);

        // Business rule: inactive if cheque emission month is before current month
        if (emissionMonth.isBefore(currentMonth)) {
            cheque.setActive(false);
            log.info("Cheque {} marked as INACTIVE (emission month {} < current month {}).",
                    item.getNumero(), emissionMonth, currentMonth);
        } else {
            cheque.setActive(true);
            log.info("Cheque {} marked as ACTIVE (emission month {} >= current month {}).",
                    item.getNumero(), emissionMonth, currentMonth);
        }

        log.info("Successfully processed and transformed Cheque Payment {}. Returning Cheque entity.", item.getNumero());
        return cheque;
    }
}
