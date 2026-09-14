package staport.finance.gBanque.transactions.dtos;

import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Critères de filtrage de l'historique des transactions.
 * Lié en paramètres de requête GET ({@code @ModelAttribute} ou binding implicite) — les records
 * sont compatibles avec le data binding Spring sur leur constructeur canonique.
 */
public record TransactionSearchDto(
        String libelle,
        Long banqueId,
        Long rapprochementId,
        List<TransactionSource> transactionSources,
        String observation,
        StatutRapprochement statutRapprochement,
        BigDecimal montant,
        LocalDate dateEmissionDebut,
        LocalDate dateEmissionFin,
        LocalDate dateOperationDebut,
        LocalDate dateOperationFin,
        LocalDate dateValeurDebut,
        LocalDate dateValeurFin
) {
}
