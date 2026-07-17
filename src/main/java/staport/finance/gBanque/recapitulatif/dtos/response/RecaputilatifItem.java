package staport.finance.gBanque.recapitulatif.dtos.response;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ligne unifiée du récapitulatif (décaissement ou encaissement), projetée directement depuis
 * {@code RecapitulatifEncoursView} (vue SQL {@code v_recapitulatif_encours}) via
 * {@code RecapitulatifEncoursMapper}.
 *
 * <p>Champs et noms JSON inchangés depuis la Phase 1 (classe mutable) pour ne pas casser le
 * contrat consommé par {@code RecaputilatifItem.ts} côté Angular — seule la source de la
 * donnée change (vue SQL au lieu de 17 repositories agrégés en mémoire).</p>
 */
public record RecaputilatifItem(
        Long id,
        LocalDate dateEmission,
        LocalDate dateEcheance,
        String libelle,
        BigDecimal debit,
        BigDecimal credit,
        String observations,
        TransactionSource source
) {
}
