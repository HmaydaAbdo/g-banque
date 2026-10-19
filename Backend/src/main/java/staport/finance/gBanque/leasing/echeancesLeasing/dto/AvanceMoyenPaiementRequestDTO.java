package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.moyensDePaiements.lecture.TypeMoyenPaiement;

/** Chèque, LCN ou virement à rattacher à l'avance d'un contrat. */
public record AvanceMoyenPaiementRequestDTO(
        @NotNull(message = "Le type de moyen de paiement est obligatoire")
        TypeMoyenPaiement type,
        @NotNull(message = "Le moyen de paiement est obligatoire")
        Long moyenPaiementId
) {}
