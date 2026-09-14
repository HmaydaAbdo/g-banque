package staport.finance.gBanque.moyensDePaiements.recap.dtos.response;

import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ligne unifiée du Recap Encaissement/Décaissement des moyens de paiement, projetée directement
 * depuis {@code RecapMoyenPaiementView} (vue SQL {@code v_recap_moyens_paiement}).
 *
 * <p>{@code emetteur} et {@code beneficiaire} sont tous les deux exposés : le frontend affiche
 * l'un ou l'autre selon l'écran (émetteur = qui nous paie, sur Recap Encaissement ; bénéficiaire =
 * qui nous devons payer, sur Recap Décaissement), sans que le backend n'ait à décider lequel est
 * "le tiers" — cette décision reste dans la vue, pas dans une transformation de données.</p>
 */
public record RecapMoyenPaiementItem(
        Long id,
        TransactionSource source,
        Long banqueId,
        String banqueNom,
        Type type,
        StatutMoyenPaiement statut,
        String numero,
        LocalDate dateEmission,
        LocalDate dateOperation,
        LocalDate dateValeur,
        LocalDate dateEcheance,
        BigDecimal montant,
        String libelle,
        String observation,
        String beneficiaire,
        String emetteur
) {
}
