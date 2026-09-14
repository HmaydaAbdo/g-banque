package staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Critères de recherche pour les listes de mises à disposition (décaissement et encaissement).
 * Tous les champs sont optionnels.
 */
public record MiseADispositionSearchDto(

        String numero,

        String beneficiaire,

        String cinBeneficiaire,

        String banqueName,

        String emetteur,

        StatutMoyenPaiement statut,

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
