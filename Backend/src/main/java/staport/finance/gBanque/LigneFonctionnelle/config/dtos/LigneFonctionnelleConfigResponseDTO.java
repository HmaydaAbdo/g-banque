package staport.finance.gBanque.LigneFonctionnelle.config.dtos;

import java.time.OffsetDateTime;

public record LigneFonctionnelleConfigResponseDTO(
        Long id,
        Integer alertThresholdMonths,
        Integer warningThresholdMonths,
        OffsetDateTime dateModification
) {}
