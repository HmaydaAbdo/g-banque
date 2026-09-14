package staport.finance.gBanque.cmt.echeances.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * DTO de réponse pour l'affichage des encours d'échéances de crédit à moyen terme.
 */
public record EncoursCMTResponseDTO(
        Long banqueId,
        String nomBanque,
        BigDecimal totalEncours,
        Page<EcheanceCMTResponseDto> echeances
) {
}
