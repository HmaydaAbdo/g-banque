package staport.finance.gBanque.leasing.config.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LeasingConfigResponseDTO(
        Long id,
        BigDecimal valeurResiduelle,
        BigDecimal tauxTva,
        OffsetDateTime dateModification
) {}
