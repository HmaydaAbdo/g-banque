package staport.finance.gBanque.moyensDePaiements.virements.dtos;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Critères de recherche pour les listes de virements (décaissement et encaissement). Tous les
 * champs sont optionnels.
 */
public record VirementSearchDto(

        String numero,

        String beneficiaire,

        String banqueBeneficiaire,

        String nomBanqueEmettrice,

        String ribBeneficiaire,

        StatutMoyenPaiement statut,

        String emetteur,

        Type type,

        BigDecimal montant,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateEmissionFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateEmissionTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOperationFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOperationTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurTo

) {}
