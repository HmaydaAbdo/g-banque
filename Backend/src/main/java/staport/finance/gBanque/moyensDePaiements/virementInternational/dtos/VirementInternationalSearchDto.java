package staport.finance.gBanque.moyensDePaiements.virementInternational.dtos;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Critères de recherche pour les listes de virements internationaux (décaissement et
 * encaissement). Tous les champs sont optionnels.
 */
public record VirementInternationalSearchDto(

        Long banqueId,

        String beneficiaire,

        String emetteur,

        BigDecimal montant,

        String libelle,

        String observation,

        StatutMoyenPaiement statut,

        Type type,

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
