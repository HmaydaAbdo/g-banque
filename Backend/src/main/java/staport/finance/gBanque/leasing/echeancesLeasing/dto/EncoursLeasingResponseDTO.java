package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * DTO de réponse pour l'affichage des encours d'échéances de leasing.
 * Fournit à la fois un résumé financier et la liste détaillée des échéances concernées,
 * avec support de la pagination et du tri.
 */
public record EncoursLeasingResponseDTO(
        Long banqueId,
        String nomBanque,
        BigDecimal totalEncours,
        Page<EcheanceLeasingResponseDTO> echeances
) {}
