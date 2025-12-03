package staport.finance.gBanque.FunPaiementService.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.FunPaiementService.dtos.FunPaymentDto;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.virements.enums.Type;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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


        boolean isStaport =
                item.getBeneficiaire() != null &&
                        item.getBeneficiaire().trim().equalsIgnoreCase("STAPORT");

        if (!isStaport) {
            Cheque cheque = createCheque(item, banque, Type.DECAISSEMENT, item.getNumero());
            return cheque == null ? List.of() : List.of(cheque); // TOUJOURS une liste
        }

        // STAPORT → create two virements with unique ids
        String numeroEncaissement = item.getNumero() + "-EN";
        String numeroDecaissement = item.getNumero() + "-DE";

        Cheque encaissement = createCheque(item, banque, Type.ENCAISSEMENT, numeroEncaissement);
        Cheque decaissement = createCheque(item, banque, Type.DECAISSEMENT, numeroDecaissement);

        return Stream.of(encaissement, decaissement)
                .filter(Objects::nonNull)
                .toList();


    }

    private Cheque createCheque(FunPaymentDto item, Banque banque, Type type, String numero) {

        if (chequeRepository.findByNumero(numero).isPresent()) {
            log.warn("Cheque {} already exists, skipping", numero);
            return null;
        }

        Cheque cheque= new Cheque();
        cheque.setNumero(numero);
        cheque.setType(type);
        cheque.setDateEmission(item.getDateCreation().atStartOfDay(systemZone).toOffsetDateTime());
        cheque.setDateEcheance(item.getDateCheque() !=null ? item.getDateCheque().atStartOfDay(systemZone).toOffsetDateTime() : null);
        cheque.setBeneficiaire(item.getBeneficiaire());
        cheque.setMontant(item.getMontant());
        cheque.setBanque(banque);

        YearMonth currentMonth = YearMonth.from(currentDateTime);
        YearMonth emissionMonth = YearMonth.from(item.getDateCreation());

        YearMonth twoMonthThreshold = currentMonth.minusMonths(2);
        // Activation logic
        if (emissionMonth.isBefore(twoMonthThreshold)) {
            cheque.setActive(false);
            log.info("Cheque {} marked as INACTIVE (emission month {} is before current month {}).",
                    item.getNumero(), emissionMonth, currentMonth);
        } else {
            cheque.setActive(true);
            log.info("Cheque {} marked as ACTIVE (emission month {} is current or future month {}).",
                    item.getNumero(), emissionMonth, currentMonth);
        }

        if(type==Type.ENCAISSEMENT){
            cheque.setEmetteur("STAPORT");
        }

        return cheque;
    }

}
