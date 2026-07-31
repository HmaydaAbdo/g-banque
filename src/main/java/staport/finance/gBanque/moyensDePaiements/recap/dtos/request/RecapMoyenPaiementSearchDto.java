package staport.finance.gBanque.moyensDePaiements.recap.dtos.request;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Critères de recherche pour les endpoints Recap Encaissement / Recap Décaissement des moyens de
 * paiement. Tous les champs sont optionnels. Les plages de dates portent sur la date d'opération
 * et la date de valeur (pas la date d'émission — choix validé avec HMAYDA le 31/07/2026).
 */
public record RecapMoyenPaiementSearchDto(

        Long banqueId,

        List<TransactionSource> sources,

        List<StatutMoyenPaiement> statuts,

        String libelle,

        String numero,

        BigDecimal montantFrom,

        BigDecimal montantTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOperationFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOperationTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurTo

) {
}
