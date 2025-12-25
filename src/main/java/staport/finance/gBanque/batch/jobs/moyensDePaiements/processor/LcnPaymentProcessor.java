package staport.finance.gBanque.batch.jobs.moyensDePaiements.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * ItemProcessor for 'LCN' payments (type = 2).
 * This class now extends AbstractPaymentProcessor to inherit common logic.
 * It focuses solely on the specific business logic for LCN payments.
 */
@Component
public class LcnPaymentProcessor extends AbstractPaymentProcessor {

    private final LcnRepository lcnRepository;

    public LcnPaymentProcessor(BanqueRepository banqueRepository, LcnRepository lcnRepository) {
        super(banqueRepository);
        this.lcnRepository = lcnRepository;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) throws InvalidChequeDataException {
        // Check if LCN already exists by numero
        Optional<Lcn> existingLcn = lcnRepository.findByNumero(item.getNumero());

        if (existingLcn.isPresent()) {
            log.info("Lcn with numero {} already exists. Skipping.", item.getNumero());
            return null; // Spring Batch will skip this item
        }

        // LCN-specific validation
        if (item.getDateEcheance() == null) {
            throw new InvalidChequeDataException("Date d'échéance (dateEcheance) is null for LCN payment numero " + item.getNumero());
        }

        // Création de l'entité et mapping des champs
        Lcn lcn = new Lcn();
        lcn.setNumero(item.getNumero());
        lcn.setDateEmission(item.getDateCreation().atStartOfDay(systemZone).toOffsetDateTime());
        lcn.setBeneficiaire(item.getBeneficiaire());
        OffsetDateTime dateEcheance = item.getDateEcheance().atStartOfDay(systemZone).toOffsetDateTime();
        lcn.setDateEcheance(dateEcheance);
        lcn.setMontant(item.getMontant());
        lcn.setBanque(banque);
        lcn.setType(Type.DECAISSEMENT);
        lcn.setActive(true);

        log.info("Successfully processed and transformed Lcn Payment {}. Returning Lcn entity.", item.getNumero());
        return lcn;
    }
}
