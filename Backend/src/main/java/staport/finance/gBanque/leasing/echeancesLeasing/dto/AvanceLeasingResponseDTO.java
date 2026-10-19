package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Avance d'un contrat de leasing (échéance n°1) et moyen de paiement qui la règle.
 *
 * @param echeanceId    identifiant de l'échéance n°1
 * @param statut        statut effectif : celui du moyen de paiement rattaché (PAYÉ s'il est RÉGLÉ), PRÉVU sinon
 * @param moyenPaiement moyen de paiement rattaché, {@code null} tant que l'avance n'est pas rattachée
 */
public record AvanceLeasingResponseDTO(
        Long echeanceId,
        Long contratLeasingId,
        BigDecimal montantTTC,
        LocalDate dateEcheance,
        StatutEcheanceLeasing statut,
        MoyenPaiementAvanceDTO moyenPaiement
) {}
