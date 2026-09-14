package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * DTO représentant le tableau d'amortissement complet d'un contrat de leasing.
 * Inclut les échéances détaillées et les agrégats financiers.
 */
public record TableauAmortissementDTO(
        Long contratLeasingId,
        String numeroContrat,
        BigDecimal totalIndemniteAPayer,
        BigDecimal montantTotalDejaPaye,
        BigDecimal montantRestantAPayer,
        Page<EcheanceLeasingResponseDTO> echeances
) {}
