package staport.finance.gBanque.moyensDePaiements.lecture;

import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vue en lecture seule d'un chèque, d'une LCN ou d'un virement, exposée aux autres domaines
 * (jamais l'entité ni le repository du module — architecture.md, « Encapsulation des domaines »).
 *
 * @param decaissement {@code true} pour un décaissement, {@code false} pour un encaissement
 */
public record MoyenPaiementResume(
        TypeMoyenPaiement type,
        Long id,
        String numero,
        BigDecimal montant,
        StatutMoyenPaiement statut,
        boolean decaissement,
        LocalDate dateEmission,
        LocalDate dateOperation,
        LocalDate dateValeur,
        String beneficiaire,
        String nomBanque) {
}
