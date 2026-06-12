package staport.finance.gBanque.moyensDePaiements.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * ItemProcessor for 'Virement' payments (type = 3).
 * Logique upsert :
 *  - statut=1 (ENCOURS) : insert si nouveau, skip si déjà présent
 *  - statut=3 (ANNULE)  : update → ANNULE si ENCOURS, skip si REGLE/ANNULE,
 *                         insert comme ANNULE si absent
 */
@Component
public class VirementPaymentProcessor extends AbstractPaymentProcessor {

    private final VirementRepository virementRepository;

    public VirementPaymentProcessor(BanqueRepository banqueRepository, VirementRepository virementRepository) {
        super(banqueRepository);
        this.virementRepository = virementRepository;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) throws InvalidChequeDataException {
        StatutMoyenPaiement statutFun = item.getStatutMoyenPaiement();

        boolean isStaport =
                item.getBeneficiaire() != null &&
                        item.getBeneficiaire().trim().equalsIgnoreCase("STAPORT");

        if (!isStaport) {
            Virement v = upsertVirement(item, banque, Type.DECAISSEMENT, item.getNumero(), statutFun);
            return v == null ? List.of() : List.of(v);
        }

        // STAPORT → deux entrées avec numéros uniques
        String numeroEncaissement = item.getNumero() + "-EN";
        String numeroDecaissement = item.getNumero() + "-DE";

        Virement encaissement = upsertVirement(item, banque, Type.ENCAISSEMENT, numeroEncaissement, statutFun);
        Virement decaissement = upsertVirement(item, banque, Type.DECAISSEMENT, numeroDecaissement, statutFun);

        return Stream.of(encaissement, decaissement)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Logique upsert pour un virement individuel.
     * Retourne l'entité à sauvegarder, ou null si skip.
     */
    private Virement upsertVirement(FunPaymentDto item, Banque banque, Type type, String numero, StatutMoyenPaiement statutFun) {
        Optional<Virement> existing = virementRepository.findByNumero(numero);

        if (statutFun == StatutMoyenPaiement.ANNULE) {
            if (existing.isPresent()) {
                Virement v = existing.get();
                if (v.getStatut() == StatutMoyenPaiement.ENCOURS) {
                    v.setStatut(StatutMoyenPaiement.ANNULE);
                    log.info("Virement {} marqué ANNULE (was ENCOURS)", numero);
                    return v;
                } else {
                    log.info("Virement {} déjà {} — annulation ignorée", numero, v.getStatut());
                    return null;
                }
            } else {
                log.info("Virement {} absent localement, création en ANNULE", numero);
                return buildVirement(item, banque, type, numero, StatutMoyenPaiement.ANNULE);
            }
        } else {
            // ENCOURS
            if (existing.isPresent()) {
                log.warn("Virement {} déjà présent, skip", numero);
                return null;
            }
            return buildVirement(item, banque, type, numero, StatutMoyenPaiement.ENCOURS);
        }
    }

    private Virement buildVirement(FunPaymentDto item, Banque banque, Type type, String numero, StatutMoyenPaiement statut) {
        Virement v = new Virement();
        v.setNumero(numero);
        v.setType(type);
        v.setDateEmission(item.getDateCreation());
        v.setBeneficiaire(item.getBeneficiaire());
        v.setMontant(item.getMontant());
        v.setBanqueEmettrice(banque);
        Optional<Banque> banqueBeneficiaire = this.banqueRepository.findByNomIgnoreCase(item.getBanqueBeneficiaire());
        banqueBeneficiaire.ifPresent(v::setBanqueBeneficiare);
        v.setNomBanqueBeneficiaireLibre(item.getBanqueBeneficiaire());
        v.setRibBeneficiaire(item.getRibBeneficiaire());
        v.setStatut(statut);
        if (type == Type.ENCAISSEMENT) {
            v.setEmetteur("STAPORT");
        }
        return v;
    }
}
