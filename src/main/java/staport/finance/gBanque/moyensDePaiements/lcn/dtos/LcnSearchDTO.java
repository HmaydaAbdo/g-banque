package staport.finance.gBanque.moyensDePaiements.lcn.dtos;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Critères de recherche pour les listes de LCN (décaissement et encaissement). Tous les champs
 * sont optionnels.
 */
public record LcnSearchDTO(

        String numero,

        String emetteur,

        String beneficiaire,

        String nomBanque,

        StatutMoyenPaiement statut,

        BigDecimal montant,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateEmissionFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateEmissionTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateEcheanceFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateEcheanceTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOperationFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOperationTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurTo

) {}
