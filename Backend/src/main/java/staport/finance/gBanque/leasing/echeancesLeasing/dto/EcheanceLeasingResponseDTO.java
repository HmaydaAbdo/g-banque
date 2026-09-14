package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record EcheanceLeasingResponseDTO(
        Long id,
        Long contratLeasingId,
        String numeroContrat,
        Integer numeroEcheance,
        LocalDate dateEcheance,
        LocalDate dateOperation,
        LocalDate dateValeur,
        BigDecimal montantTTC,
        BigDecimal montantHT,
        BigDecimal montantTVA,
        StatutEcheanceLeasing statutPaiement,
        OffsetDateTime dateCreation,
        OffsetDateTime dateModification,
        String libelle,
        String observation
) {}
