package staport.finance.gBanque.FunPaiementService.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.FunPaiementService.dtos.FunPaymentDto;
import staport.finance.gBanque.virements.entities.Virement;
import staport.finance.gBanque.virements.repositories.VirementRepository;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Optional;

/**
 * ItemProcessor for 'Virement' payments (type = 3).
 * This class now extends AbstractPaymentProcessor to inherit common logic.
 * It focuses solely on the specific business logic for Virement payments.
 */
@Component
public class VirementPaymentProcessor extends AbstractPaymentProcessor {

    private final VirementRepository virementRepository;
    private final OffsetDateTime curentDateTime = OffsetDateTime.now();

    public VirementPaymentProcessor(BanqueRepository banqueRepository, VirementRepository virementRepository) {
        super(banqueRepository);
        this.virementRepository = virementRepository;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) throws InvalidChequeDataException {
        Optional<Virement> existingVirement = virementRepository.findById(item.getNumero());

        if (existingVirement.isPresent()) {
            log.info("Virement with numero {} already exists. Skipping.", item.getNumero());
            return null;
        }

        // Vérifier si le bénéficiaire est "MR LE RECEVEUR DES DOUANES"
        boolean isReceveurDouanes = "MR LE RECEVEUR DES DOUANES".equalsIgnoreCase(item.getBeneficiaire());

        // Virement-specific validation (sauf pour MR LE RECEVEUR DES DOUANES)
        if (!isReceveurDouanes) {
            if (item.getBanqueBeneficiaire() == null || item.getBanqueBeneficiaire().trim().isEmpty()) {
                throw new InvalidChequeDataException("Beneficiary Bank is null or empty for Virement payment numero " + item.getNumero());
            }
            if (item.getRibBeneficiaire() == null || item.getRibBeneficiaire().trim().isEmpty()) {
                throw new InvalidChequeDataException("Beneficiary RIB is null or empty for Virement payment numero " + item.getNumero());
            }
        }


        Virement virement = new Virement();
        virement.setNumero(item.getNumero());

        OffsetDateTime dateEmission=item.getDateCreation().atStartOfDay(systemZone).toOffsetDateTime();

        virement.setDateEmission(dateEmission);
        virement.setBeneficiaire(item.getBeneficiaire());
        virement.setMontant(item.getMontant());
        virement.setBanque(banque);
        virement.setBanqueBeneficiaire(item.getBanqueBeneficiaire());
        virement.setRibBeneficiaire(item.getRibBeneficiaire());

        YearMonth currentMonth = YearMonth.from(curentDateTime);
        YearMonth emissionMonth = YearMonth.from(dateEmission);

        if (emissionMonth.isBefore(currentMonth)) {
            virement.setActive(false);
            log.info("Virement {} marked as INACTIVE (emission month {} < current month {}).",
                    item.getNumero(), emissionMonth, currentMonth);
        } else {
            virement.setActive(true);
            log.info("Virement {} marked as ACTIVE (emission month {} >= current month {}).",
                    item.getNumero(), emissionMonth, currentMonth);
        }

        log.info("Successfully processed and transformed Virement Payment {}. Returning Virement entity.", item.getNumero());
        return virement;
    }
}
