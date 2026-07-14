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

    /**
     * Accesseur dédié à l'audit ({@code AuditableAction#idAccessor}) — la commission fusionnée
     * n'est pas directement accessible comme {@code Long} depuis la racine de la réponse.
     */
    public Long getCommissionId() { return commission != null ? commission.getId() : null; }
}
