package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;
import staport.finance.gBanque.lcn.entities.Lcn;
import staport.finance.gBanque.lcn.repositories.LcnRepository;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Optional;

/**
 * ItemProcessor for 'LCN' payments (type = 2).
 * This class now extends AbstractPaymentProcessor to inherit common logic.
 * It focuses solely on the specific business logic for LCN payments.
 */
@Component
public class LcnPaymentProcessor extends AbstractPaymentProcessor {

    private final LcnRepository lcnRepository;
    private final OffsetDateTime curentDateTime = OffsetDateTime.now();

    public LcnPaymentProcessor(BanqueRepository banqueRepository, LcnRepository lcnRepository) {
        super(banqueRepository);
        this.lcnRepository = lcnRepository;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) throws InvalidChequeDataException {
        // Check if LCN already exists by numero
        Optional<Lcn> existingLcn = lcnRepository.findById(item.getNumero());

        if (existingLcn.isPresent()) {
            log.info("Lcn with numero {} already exists. Skipping.", item.getNumero());
            return null; // Spring Batch will skip this item
        }


        // LCN-specific validation
        if (item.getDateEcheance() == null) {
            throw new InvalidChequeDataException("Date d'échéance (dateEcheance) is null for LCN payment numero " + item.getNumero());
        }

        Lcn lcn = new Lcn();
        lcn.setNumero(item.getNumero());
        lcn.setDateEmission(item.getDateCheque().atStartOfDay(systemZone).toOffsetDateTime());
        lcn.setBeneficiaire(item.getBeneficiaire());
        OffsetDateTime dateEcheance = item.getDateEcheance().atStartOfDay(systemZone).toOffsetDateTime();
        lcn.setDateEcheance(dateEcheance);
        lcn.setMontant(item.getMontant());
        lcn.setBanque(banque);

        // Compare dateEcheance against current month
        YearMonth currentMonth = YearMonth.from(curentDateTime);
        YearMonth echeanceMonth = YearMonth.from(dateEcheance);

        if (echeanceMonth.isBefore(currentMonth)) {
            lcn.setActive(false);
            log.info("LCN {} marked as INACTIVE (echeance month {} < current month {}).",
                    item.getNumero(), echeanceMonth, currentMonth);
        } else {
            lcn.setActive(true);
            log.info("LCN {} marked as ACTIVE (echeance month {} >= current month {}).",
                    item.getNumero(), echeanceMonth, currentMonth);
        }
        log.info("Successfully processed and transformed Lcn Payment {}. Returning Lcn entity.", item.getNumero());
        return lcn;
    }
}
