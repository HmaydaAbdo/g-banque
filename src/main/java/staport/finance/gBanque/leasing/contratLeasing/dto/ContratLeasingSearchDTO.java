package staport.finance.gBanque.leasing.contratLeasing.dto;

import staport.finance.gBanque.leasing.contratLeasing.enums.TypeContratLeasing;

import java.math.BigDecimal;

public record ContratLeasingSearchDTO(
        String numeroContrat,
        Long banqueDePrelevementId,
        Long organismeLeasingId,
        String immobilisation,
        TypeContratLeasing typeContrat,
        Boolean completed,
        BigDecimal montantEcheance
) {}
