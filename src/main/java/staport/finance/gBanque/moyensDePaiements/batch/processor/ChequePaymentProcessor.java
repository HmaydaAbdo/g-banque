package staport.finance.gBanque.moyensDePaiements.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.services.ChequeBatchGateway;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.util.Optional;

/**
 * ItemProcessor for 'Cheque' payments (type = 1).
 * Seul le Virement supporte l'encaissement ET le décaissement (opération interne
 * entre banques Staport). Tous les autres moyens de paiement synchronisés via FunPaiement
 * (Cheque, LCN, Mise à disposition) sont toujours des DECAISSEMENT.
 * Logique upsert :
 *  - statut=1 (ENCOURS) : insert si nouveau, skip si déjà présent
 *  - statut=3 (ANNULE)  : update → ANNULE si ENCOURS, skip si déjà REGLE ou ANNULE,
 *                         insert comme ANNULE si absent
 *  - statut=2/4         : déjà filtré par AbstractPaymentProcessor (retourne null)
 */
@Component
public class ChequePaymentProcessor extends AbstractPaymentProcessor {

    private final ChequeBatchGateway chequeBatchGateway;

    public ChequePaymentProcessor(IBanqueService banqueService, ChequeBatchGateway chequeBatchGateway) {
        super(banqueService);
        this.chequeBatchGateway = chequeBatchGateway;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) {
        StatutMoyenPaiement statutFun = item.getStatutMoyenPaiement();
        Optional<Cheque> existing = chequeBatchGateway.findByIdPaiement(item.getIdPaiement());

        if (statutFun == StatutMoyenPaiement.ANNULE) {
            if (existing.isPresent()) {
                Cheque cheque = existing.get();
                if (cheque.getStatut() == StatutMoyenPaiement.ENCOURS) {
                    cheque.setStatut(StatutMoyenPaiement.ANNULE);
                    log.info("Cheque {} (id_paiement={}) marqué ANNULE (was ENCOURS)", item.getNumero(), item.getIdPaiement());
                    return cheque;
                } else {
                    log.info("Cheque {} (id_paiement={}) déjà {} — annulation ignorée", item.getNumero(), item.getIdPaiement(), cheque.getStatut());
                    return null;
                }
            } else {
                // Absent localement → créer directement comme ANNULE
                log.info("Cheque {} (id_paiement={}) absent localement, création en ANNULE", item.getNumero(), item.getIdPaiement());
                return buildCheque(item, banque, StatutMoyenPaiement.ANNULE);
            }
        } else {
            // ENCOURS
            if (existing.isPresent()) {
                log.warn("Cheque {} (id_paiement={}) déjà présent, skip", item.getNumero(), item.getIdPaiement());
                return null;
            }
            return buildCheque(item, banque, StatutMoyenPaiement.ENCOURS);
        }
    }

    private Cheque buildCheque(FunPaymentDto item, Banque banque, StatutMoyenPaiement statut) {
        Cheque cheque = new Cheque();
        cheque.setIdPaiement(item.getIdPaiement());
        cheque.setNumero(item.getNumero());
        cheque.setType(Type.DECAISSEMENT);
        cheque.setDateEmission(item.getDateCreation());
        cheque.setDateEcheance(item.getDateCheque());
        cheque.setBeneficiaire(item.getBeneficiaire());
        cheque.setMontant(item.getMontant());
        cheque.setBanque(banque);
        cheque.setStatut(statut);
        cheque.setObservation("CHQ N°" + item.getNumero());
        cheque.setLibelle((cheque.getType() == Type.DECAISSEMENT ? "Décaissement" : "Encaissement")
                + " - Bénéficiaire: " + (cheque.getBeneficiaire() != null ? cheque.getBeneficiaire() : "N/A")
                + " / Émetteur: " + (cheque.getEmetteur() != null ? cheque.getEmetteur() : "N/A"));
        return cheque;
    }
}
