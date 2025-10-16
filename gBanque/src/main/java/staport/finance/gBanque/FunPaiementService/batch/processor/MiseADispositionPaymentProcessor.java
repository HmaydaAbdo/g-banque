package staport.finance.gBanque.FunPaiementService.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.FunPaiementService.dtos.FunPaymentDto;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.miseADisposition.repositories.MiseADispositionRepository;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Optional;

/**
 * ItemProcessor for 'Mise à disposition' payments (type = 4).
 * This class now extends AbstractPaymentProcessor to inherit common logic.
 * It focuses solely on the specific business logic for Mise à disposition payments.
 */
@Component
public class MiseADispositionPaymentProcessor extends AbstractPaymentProcessor {

    private final MiseADispositionRepository miseADispositionRepository;
    private final OffsetDateTime curentDateTime = OffsetDateTime.now();

    public MiseADispositionPaymentProcessor(BanqueRepository banqueRepository, MiseADispositionRepository miseADispositionRepository) {
        super(banqueRepository);
        this.miseADispositionRepository = miseADispositionRepository;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) throws InvalidChequeDataException {

        Optional<MiseADisposition> existingMiseADisposition = miseADispositionRepository.findById(item.getNumero());
        if (existingMiseADisposition.isPresent()) {
            log.info("Mise à disposition with numero {} already exists. Skipping.", item.getNumero());
            return null; // Spring Batch will skip this item
        }

        // MiseADisposition-specific validation
        if (item.getCinBeneficiaire() == null || item.getCinBeneficiaire().trim().isEmpty()) {
            throw new InvalidChequeDataException("Beneficiary CIN is null or empty for Mise à disposition payment numero " + item.getNumero());
        }

        MiseADisposition miseADisposition = new MiseADisposition();
        miseADisposition.setNumero(item.getNumero());

        OffsetDateTime dateEmission=item.getDateCreation().atStartOfDay(systemZone).toOffsetDateTime();

        miseADisposition.setDateEmission(dateEmission);
        miseADisposition.setBeneficiaire(item.getBeneficiaire());
        miseADisposition.setMontant(item.getMontant());
        miseADisposition.setBanque(banque);
        miseADisposition.setCinBeneficiaire(item.getCinBeneficiaire());



        if( dateEmission.isBefore(curentDateTime) ) {
            miseADisposition.setActive(false);
            log.info("Mise à disposition {} marked as INACTIVE (emission date {} < current date {}).",
                    item.getNumero(), dateEmission, curentDateTime);
        }else {
            miseADisposition.setActive(true);
            log.info("Mise à disposition {} marked as ACTIVE (emission date {} >= current date {}).",
                    item.getNumero(), dateEmission, curentDateTime);
        }
        log.info("Successfully processed and transformed Mise à disposition Payment {}. Returning MiseADisposition entity.", item.getNumero());
        return miseADisposition;
    }
}
