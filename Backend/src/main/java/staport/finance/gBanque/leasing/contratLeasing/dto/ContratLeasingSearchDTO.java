package staport.finance.gBanque.leasing.contratLeasing.dto;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.leasing.contratLeasing.enums.TypeContratLeasing;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratLeasingSearchDTO(
        String numeroContrat,
        Long banqueDePrelevementId,
        Long organismeLeasingId,
        String immobilisation,
        TypeContratLeasing typeContrat,
        Boolean completed,
        BigDecimal montantEcheance,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebutMin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebutMax
) {}
