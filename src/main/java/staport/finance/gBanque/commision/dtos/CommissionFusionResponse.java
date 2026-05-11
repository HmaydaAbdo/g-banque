package staport.finance.gBanque.commision.dtos;

/**
 * Réponse après fusion de commissions.
 * Aucune TVA n'est générée lors d'une fusion.
 */
public class CommissionFusionResponse {

    private CommissionResponseDto commission;

    public CommissionFusionResponse(CommissionResponseDto commission) {
        this.commission = commission;
    }

    public CommissionResponseDto getCommission() { return commission; }
}
