package staport.finance.gBanque.cautions.dtos.response;

import staport.finance.gBanque.commision.dtos.CommissionResponseDto;

/**
 * Réponse de clôture d'une caution.
 *
 * <p>Retourne la caution mise à jour ({@code actif = false}, {@code dateDeMainLevee} renseignée)
 * et la commission de clôture générée selon la stratégie de la banque.
 */
public record CloturerCautionResponse(
    CautionResponseDto    caution,
    CommissionResponseDto commission
) {}
