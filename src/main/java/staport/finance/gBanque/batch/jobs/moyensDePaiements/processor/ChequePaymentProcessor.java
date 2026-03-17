package staport.finance.gBanque.batch.jobs.moyensDePaiements.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
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
        cheque.setDateEmission(item.getDateCreation());
        cheque.setDateEcheance(item.getDateCheque());
        cheque.setBeneficiaire(item.getBeneficiaire());
        cheque.setMontant(item.getMontant());
        cheque.setBanque(banque);
        cheque.setActive(true);
        if(type==Type.ENCAISSEMENT){
            cheque.setEmetteur("STAPORT");
        }

        return cheque;
    }

}
