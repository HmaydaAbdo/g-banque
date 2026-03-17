package staport.finance.gBanque.batch.jobs.moyensDePaiements.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import java.time.LocalDate;
import java.util.Optional;

/**
 * ItemProcessor for 'Mise à disposition' payments (type = 4).
 * This class now extends AbstractPaymentProcessor to inherit common logic.
 * It focuses solely on the specific business logic for Mise à disposition payments.
 */
@Component
public class MiseADispositionPaymentProcessor extends AbstractPaymentProcessor {

    private final MiseADispositionRepository miseADispositionRepository;

    public MiseADispositionPaymentProcessor(BanqueRepository banqueRepository, MiseADispositionRepository miseADispositionRepository) {
        super(banqueRepository);
        this.miseADispositionRepository = miseADispositionRepository;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) throws InvalidChequeDataException {

        Optional<MiseADisposition> existingMiseADisposition = miseADispositionRepository.findByNumero(item.getNumero());
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

        LocalDate dateEmission = item.getDateCreation();

        miseADisposition.setDateEmission(dateEmission);
        miseADisposition.setBeneficiaire(item.getBeneficiaire());
        miseADisposition.setMontant(item.getMontant());
        miseADisposition.setBanque(banque);
        miseADisposition.setType(Type.DECAISSEMENT);
        miseADisposition.setCinBeneficiaire(item.getCinBeneficiaire());
        miseADisposition.setActive(true);


        log.info("Successfully processed and transformed Mise à disposition Payment {}. Returning MiseADisposition entity.", item.getNumero());
        return miseADisposition;
    }
}
