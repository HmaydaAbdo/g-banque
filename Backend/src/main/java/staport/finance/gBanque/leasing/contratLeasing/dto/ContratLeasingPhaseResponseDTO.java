package staport.finance.gBanque.leasing.contratLeasing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ContratLeasingPhaseResponseDTO(
        Long id,
        Integer numeroPhase,
        Integer dureeMois,
        BigDecimal mensualiteTTC,
        OffsetDateTime dateCreation,
        OffsetDateTime dateModification
) {}
