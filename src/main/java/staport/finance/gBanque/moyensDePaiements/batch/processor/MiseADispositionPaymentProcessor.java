package staport.finance.gBanque.moyensDePaiements.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.services.MiseADispositionBatchGateway;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.util.Optional;

/**
 * ItemProcessor for 'Mise à disposition' payments (type = 4).
 * Logique upsert :
 *  - statut=1 (ENCOURS) : insert si nouveau, skip si déjà présent
 *  - statut=3 (ANNULE)  : update → ANNULE si ENCOURS, skip si REGLE/ANNULE,
 *                         insert comme ANNULE si absent
 * Note : le CIN bénéficiaire n'est pas requis pour les annulations.
 */
@Component
public class MiseADispositionPaymentProcessor extends AbstractPaymentProcessor {

    private final MiseADispositionBatchGateway miseADispositionBatchGateway;

    public MiseADispositionPaymentProcessor(IBanqueService banqueService, MiseADispositionBatchGateway miseADispositionBatchGateway) {
        super(banqueService);
        this.miseADispositionBatchGateway = miseADispositionBatchGateway;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) throws InvalidChequeDataException {
        StatutMoyenPaiement statutFun = item.getStatutMoyenPaiement();
        Optional<MiseADisposition> existing = miseADispositionBatchGateway.findByIdPaiement(item.getIdPaiement());

        if (statutFun == StatutMoyenPaiement.ANNULE) {
            if (existing.isPresent()) {
                MiseADisposition mad = existing.get();
                if (mad.getStatut() == StatutMoyenPaiement.ENCOURS) {
                    mad.setStatut(StatutMoyenPaiement.ANNULE);
                    log.info("MiseADisposition {} (id_paiement={}) marquée ANNULE (was ENCOURS)", item.getNumero(), item.getIdPaiement());
                    return mad;
                } else {
                    log.info("MiseADisposition {} (id_paiement={}) déjà {} — annulation ignorée", item.getNumero(), item.getIdPaiement(), mad.getStatut());
                    return null;
                }
            } else {
                log.info("MiseADisposition {} (id_paiement={}) absente localement, création en ANNULE", item.getNumero(), item.getIdPaiement());
                return buildMad(item, banque, StatutMoyenPaiement.ANNULE);
            }
        } else {
            // ENCOURS
            if (existing.isPresent()) {
                log.info("MiseADisposition {} (id_paiement={}) déjà présente, skip", item.getNumero(), item.getIdPaiement());
                return null;
            }

            // Validation spécifique MAD : CIN obligatoire pour un nouvel ENCOURS
            if (item.getCinBeneficiaire() == null || item.getCinBeneficiaire().trim().isEmpty()) {
                throw new InvalidChequeDataException("Beneficiary CIN is null or empty for Mise à disposition payment numero " + item.getNumero());
            }

            log.info("MiseADisposition {} traitée avec succès, création en ENCOURS", item.getNumero());
            return buildMad(item, banque, StatutMoyenPaiement.ENCOURS);
        }
    }

    private MiseADisposition buildMad(FunPaymentDto item, Banque banque, StatutMoyenPaiement statut) {
        MiseADisposition mad = new MiseADisposition();
        mad.setIdPaiement(item.getIdPaiement());
        mad.setNumero(item.getNumero());
        mad.setDateEmission(item.getDateCreation());
        mad.setBeneficiaire(item.getBeneficiaire());
        mad.setMontant(item.getMontant());
        mad.setBanque(banque);
        mad.setType(Type.DECAISSEMENT);
        mad.setCinBeneficiaire(item.getCinBeneficiaire());
        mad.setStatut(statut);
        mad.setObservation(" MAD " + item.getNumero());
        mad.setLibelle(item.getBeneficiaire() != null ? item.getBeneficiaire() : "N/A");
        return mad;
    }
}
