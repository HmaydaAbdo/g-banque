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
        /**
         * Plafond en cours de l'organisme : celui de son contrat cadre ACTIF et non échu, quel que soit
         * le contrat cadre signé à la création du contrat. {@code null} si l'organisme n'a aucun contrat
         * cadre en cours.
         */
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
) {

    /** Copie du DTO avec le plafond en cours de l'organisme (le mapper ne le renseigne pas). */
    public ContratLeasingResponseDTO withPlafond(BigDecimal plafondEnCours) {
        return new ContratLeasingResponseDTO(id, numeroContrat, banqueDePrelevementId, nomBanqueDePrelevement,
                organismeLeasingId, nomOrganismeLeasing, contratCadreId, plafondEnCours, immobilisation,
                montantAchatTTC, dateDebut, periodeEnMois, dateFin, typeContrat, valeurResiduelle, phases,
                dateCreation, dateModification, completed);
    }
}
