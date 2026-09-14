package staport.finance.gBanque.cmt.echeances.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * DTO de réponse pour le tableau d'amortissement d'un crédit à moyen terme.
 * Il regroupe les informations agrégées du crédit et la liste paginée de ses échéances.
 */
public record TableauAmortissementCMTResponseDto(
        Long creditId,
        String designationCredit,
        BigDecimal montantTotalAPayer,
        BigDecimal montantTotalDejaPaye,
        BigDecimal montantRestantAPayer,
        Page<EcheanceCMTResponseDto> echeances
) {
}
