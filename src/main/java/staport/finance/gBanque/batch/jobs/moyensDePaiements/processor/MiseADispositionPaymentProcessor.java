package staport.finance.gBanque.batch.jobs.moyensDePaiements.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
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

    private final MiseADispositionRepository miseADispositionRepository;

    public MiseADispositionPaymentProcessor(BanqueRepository banqueRepository, MiseADispositionRepository miseADispositionRepository) {
        super(banqueRepository);
        this.miseADispositionRepository = miseADispositionRepository;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) throws InvalidChequeDataException {
        StatutMoyenPaiement statutFun = item.getStatutMoyenPaiement();
        Optional<MiseADisposition> existing = miseADispositionRepository.findByNumero(item.getNumero());

        if (statutFun == StatutMoyenPaiement.ANNULE) {
            if (existing.isPresent()) {
                MiseADisposition mad = existing.get();
                if (mad.getStatut() == StatutMoyenPaiement.ENCOURS) {
                    mad.setStatut(StatutMoyenPaiement.ANNULE);
                    log.info("MiseADisposition {} marquée ANNULE (was ENCOURS)", item.getNumero());
                    return mad;
                } else {
                    log.info("MiseADisposition {} déjà {} — annulation ignorée", item.getNumero(), mad.getStatut());
                    return null;
                }
            } else {
                log.info("MiseADisposition {} absente localement, création en ANNULE", item.getNumero());
                return buildMad(item, banque, StatutMoyenPaiement.ANNULE);
            }
        } else {
            // ENCOURS
            if (existing.isPresent()) {
                log.info("MiseADisposition {} déjà présente, skip", item.getNumero());
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
        mad.setNumero(item.getNumero());
        mad.setDateEmission(item.getDateCreation());
        mad.setBeneficiaire(item.getBeneficiaire());
        mad.setMontant(item.getMontant());
        mad.setBanque(banque);
        mad.setType(Type.DECAISSEMENT);
        mad.setCinBeneficiaire(item.getCinBeneficiaire());
        mad.setStatut(statut);
        return mad;
    }
}
