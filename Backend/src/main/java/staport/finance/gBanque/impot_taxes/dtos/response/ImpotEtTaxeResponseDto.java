package staport.finance.gBanque.impot_taxes.dtos.response;

import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour un impôt/taxe.
 */
public record ImpotEtTaxeResponseDto(

        Long id,
        String libelle,
        String observation,
        BigDecimal montant,
        Long banqueId,
        String banqueNom,
        Motif motif,
        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperation,
        Status status

) {}
