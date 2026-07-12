package staport.finance.gBanque.moyensDePaiements.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.services.LcnBatchGateway;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.util.Optional;

/**
 * ItemProcessor for 'LCN' payments (type = 2).
 * Logique upsert :
 *  - statut=1 (ENCOURS) : insert si nouveau, skip si déjà présent
 *  - statut=3 (ANNULE)  : update → ANNULE si ENCOURS, skip si REGLE/ANNULE,
 *                         insert comme ANNULE si absent
 */
@Component
public class LcnPaymentProcessor extends AbstractPaymentProcessor {

    private final LcnBatchGateway lcnBatchGateway;

    public LcnPaymentProcessor(IBanqueService banqueService, LcnBatchGateway lcnBatchGateway) {
        super(banqueService);
        this.lcnBatchGateway = lcnBatchGateway;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) throws InvalidChequeDataException {
        StatutMoyenPaiement statutFun = item.getStatutMoyenPaiement();
        Optional<Lcn> existing = lcnBatchGateway.findByIdPaiement(item.getIdPaiement());

        if (statutFun == StatutMoyenPaiement.ANNULE) {
            if (existing.isPresent()) {
                Lcn lcn = existing.get();
                if (lcn.getStatut() == StatutMoyenPaiement.ENCOURS) {
                    lcn.setStatut(StatutMoyenPaiement.ANNULE);
                    log.info("Lcn {} (id_paiement={}) marqué ANNULE (was ENCOURS)", item.getNumero(), item.getIdPaiement());
                    return lcn;
                } else {
                    log.info("Lcn {} (id_paiement={}) déjà {} — annulation ignorée", item.getNumero(), item.getIdPaiement(), lcn.getStatut());
                    return null;
                }
            } else {
                // Absent localement → créer directement comme ANNULE
                log.info("Lcn {} (id_paiement={}) absent localement, création en ANNULE", item.getNumero(), item.getIdPaiement());
                return buildLcn(item, banque, StatutMoyenPaiement.ANNULE);
            }
        } else {
            // ENCOURS
            if (existing.isPresent()) {
                log.info("Lcn {} (id_paiement={}) déjà présent, skip", item.getNumero(), item.getIdPaiement());
                return null;
            }

            // Validation spécifique LCN : date d'échéance obligatoire pour un nouvel ENCOURS
            if (item.getDateEcheance() == null) {
                throw new InvalidChequeDataException("Date d'échéance (dateEcheance) is null for LCN payment numero " + item.getNumero());
            }

            log.info("Lcn {} traité avec succès, création en ENCOURS", item.getNumero());
            return buildLcn(item, banque, StatutMoyenPaiement.ENCOURS);
        }
    }

    private Lcn buildLcn(FunPaymentDto item, Banque banque, StatutMoyenPaiement statut) {
        Lcn lcn = new Lcn();
        lcn.setIdPaiement(item.getIdPaiement());
        lcn.setNumero(item.getNumero());
        lcn.setDateEmission(item.getDateCreation());
        lcn.setBeneficiaire(item.getBeneficiaire());
        lcn.setDateEcheance(item.getDateEcheance());
        lcn.setMontant(item.getMontant());
        lcn.setBanque(banque);
        lcn.setType(Type.DECAISSEMENT);
        lcn.setStatut(statut);
        return lcn;
    }
}
