package staport.finance.gBanque.leasing.contratLeasing.dto;

import staport.finance.gBanque.leasing.contratLeasing.enums.TypeContratLeasing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ContratLeasingResponseDTO(
        Long id,
        String numeroContrat,
        Long banqueDePrelevementId,
        String nomBanqueDePrelevement,
        Long organismeLeasingId,
        String nomOrganismeLeasing,
        /** ID du contrat cadre actif au moment de la signature. */
        Long contratCadreId,
        /** Plafond du contrat cadre au moment de la signature. */
        BigDecimal plafond,
        String immobilisation,
        BigDecimal montantAchatTTC,
        LocalDate dateDebut,
        Integer periodeEnMois,
        LocalDate dateFin,
        TypeContratLeasing typeContrat,
        BigDecimal valeurResiduelle,
        List<ContratLeasingPhaseResponseDTO> phases,
        OffsetDateTime dateCreation,
        OffsetDateTime dateModification,
        boolean completed
) {}
