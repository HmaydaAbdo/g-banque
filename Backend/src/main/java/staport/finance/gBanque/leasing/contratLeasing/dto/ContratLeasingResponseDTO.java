package staport.finance.gBanque.leasing.contratLeasing.dto;

import staport.finance.gBanque.leasing.contratLeasing.enums.TypeContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.MoyenPaiementAvanceDTO;

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
        /** Avance (échéance n°1), {@code null} si le contrat n'en a pas. */
        BigDecimal avanceTTC,
        /** Moyen de paiement qui règle l'avance ; renseigné uniquement par la consultation d'un contrat, null sinon. */
        MoyenPaiementAvanceDTO avanceMoyenPaiement,
        List<ContratLeasingPhaseResponseDTO> phases,
        OffsetDateTime dateCreation,
        OffsetDateTime dateModification,
        boolean completed
) {

    /** Copie du DTO avec le plafond en cours de l'organisme (le mapper ne le renseigne pas). */
    public ContratLeasingResponseDTO withPlafond(BigDecimal plafondEnCours) {
        return new ContratLeasingResponseDTO(id, numeroContrat, banqueDePrelevementId, nomBanqueDePrelevement,
                organismeLeasingId, nomOrganismeLeasing, contratCadreId, plafondEnCours, immobilisation,
                montantAchatTTC, dateDebut, periodeEnMois, dateFin, typeContrat, valeurResiduelle, avanceTTC,
                avanceMoyenPaiement, phases, dateCreation, dateModification, completed);
    }

    /** Copie du DTO avec le moyen de paiement de l'avance (le mapper ne le renseigne pas). */
    public ContratLeasingResponseDTO withAvanceMoyenPaiement(MoyenPaiementAvanceDTO moyen) {
        return new ContratLeasingResponseDTO(id, numeroContrat, banqueDePrelevementId, nomBanqueDePrelevement,
                organismeLeasingId, nomOrganismeLeasing, contratCadreId, plafond, immobilisation,
                montantAchatTTC, dateDebut, periodeEnMois, dateFin, typeContrat, valeurResiduelle, avanceTTC,
                moyen, phases, dateCreation, dateModification, completed);
    }
}
