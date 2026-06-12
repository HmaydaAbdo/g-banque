package staport.finance.gBanque.moyensDePaiements.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * ItemProcessor for 'Cheque' payments (type = 1).
 * Logique upsert :
 *  - statut=1 (ENCOURS) : insert si nouveau, skip si déjà présent
 *  - statut=3 (ANNULE)  : update → ANNULE si ENCOURS, skip si déjà REGLE ou ANNULE,
 *                         insert comme ANNULE si absent
 *  - statut=2/4         : déjà filtré par AbstractPaymentProcessor (retourne null)
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
        StatutMoyenPaiement statutFun = item.getStatutMoyenPaiement();

        boolean isStaport =
                item.getBeneficiaire() != null &&
                        item.getBeneficiaire().trim().equalsIgnoreCase("STAPORT");

        if (!isStaport) {
            Cheque cheque = upsertCheque(item, banque, Type.DECAISSEMENT, item.getNumero(), statutFun);
            return cheque == null ? List.of() : List.of(cheque);
        }

        // STAPORT → deux entrées avec numéros uniques
        String numeroEncaissement = item.getNumero() + "-EN";
        String numeroDecaissement = item.getNumero() + "-DE";

        Cheque encaissement = upsertCheque(item, banque, Type.ENCAISSEMENT, numeroEncaissement, statutFun);
        Cheque decaissement = upsertCheque(item, banque, Type.DECAISSEMENT, numeroDecaissement, statutFun);

        return Stream.of(encaissement, decaissement)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Logique upsert pour un chèque individuel.
     * Retourne l'entité à sauvegarder (insert ou update), ou null si skip.
     */
    private Cheque upsertCheque(FunPaymentDto item, Banque banque, Type type, String numero, StatutMoyenPaiement statutFun) {
        Optional<Cheque> existing = chequeRepository.findByNumero(numero);

        if (statutFun == StatutMoyenPaiement.ANNULE) {
            if (existing.isPresent()) {
                Cheque cheque = existing.get();
                if (cheque.getStatut() == StatutMoyenPaiement.ENCOURS) {
                    cheque.setStatut(StatutMoyenPaiement.ANNULE);
                    log.info("Cheque {} marqué ANNULE (was ENCOURS)", numero);
                    return cheque;
                } else {
                    log.info("Cheque {} déjà {} — annulation ignorée", numero, cheque.getStatut());
                    return null;
                }
            } else {
                // Absent localement → créer directement comme ANNULE
                log.info("Cheque {} absent localement, création en ANNULE", numero);
                return buildCheque(item, banque, type, numero, StatutMoyenPaiement.ANNULE);
            }
        } else {
            // ENCOURS
            if (existing.isPresent()) {
                log.warn("Cheque {} déjà présent, skip", numero);
                return null;
            }
            return buildCheque(item, banque, type, numero, StatutMoyenPaiement.ENCOURS);
        }
    }

    private Cheque buildCheque(FunPaymentDto item, Banque banque, Type type, String numero, StatutMoyenPaiement statut) {
        Cheque cheque = new Cheque();
        cheque.setNumero(numero);
        cheque.setType(type);
        cheque.setDateEmission(item.getDateCreation());
        cheque.setDateEcheance(item.getDateCheque());
        cheque.setBeneficiaire(item.getBeneficiaire());
        cheque.setMontant(item.getMontant());
        cheque.setBanque(banque);
        cheque.setStatut(statut);
        if (type == Type.ENCAISSEMENT) {
            cheque.setEmetteur("STAPORT");
        }
        return cheque;
    }
}
