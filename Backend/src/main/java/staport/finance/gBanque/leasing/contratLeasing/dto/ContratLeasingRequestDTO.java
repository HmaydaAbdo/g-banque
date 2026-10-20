package staport.finance.gBanque.leasing.contratLeasing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import staport.finance.gBanque.leasing.contratLeasing.enums.TypeContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.AvanceMoyenPaiementRequestDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContratLeasingRequestDTO(
        Long id,
        @NotBlank(message = "Le numéro de contrat est obligatoire")
        @Size(max = 50, message = "Le numéro de contrat ne doit pas dépasser 50 caractères")
        String numeroContrat,
        @NotNull(message = "L'ID de la banque de prélèvement est obligatoire")
        Long banqueDePrelevementId,
        @NotNull(message = "L'ID de l'organisme de leasing est obligatoire")
        Long organismeLeasingId,
        @NotBlank(message = "L'immobilisation est obligatoire")
        @Size(max = 255, message = "L'immobilisation ne doit pas dépasser 255 caractères")
        String immobilisation,
        @NotNull(message = "Le montant d'achat est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant d'achat doit être supérieur à zéro")
        @Positive(message = "Le montant d'achat TTC est obligatoire et doit être positif.")
        BigDecimal montantAchatTTC,
        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,
        @NotNull(message = "La période en mois est obligatoire")
        @Min(value = 1, message = "La période en mois doit être d'au moins 1")
        @Positive(message = "La période en mois doit être un entier positif")
        Integer periodeEnMois,
        @NotNull(message = "Le type de contrat de leasing est obligatoire")
        TypeContratLeasing typeContrat,
        @NotNull(message = "La valeur résiduelle est obligatoire")
        @Positive(message = "La valeur résiduelle doit être positive.")
        BigDecimal valeurResiduelle,
        /**
         * Avance optionnelle (échéance n°1) : chèque, LCN ou virement existant qui la règle. Son montant devient celui de
         * l'avance, incluse dans le montant d'achat et dans la période.
         */
        @Valid
        AvanceMoyenPaiementRequestDTO avanceMoyenPaiement,
        @Valid
        @NotEmpty(message = "La liste des phases ne doit pas être vide")
        List<ContratLeasingPhaseRequestDTO> phases
) {}
