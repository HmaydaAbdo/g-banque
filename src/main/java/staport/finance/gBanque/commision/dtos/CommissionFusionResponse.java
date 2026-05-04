package staport.finance.gBanque.commision.dtos;

import staport.finance.gBanque.impot_taxes.dtos.ImpotEtTaxeResponseDto;

/**
 * Réponse après fusion de commissions.
 *
 * <p>{@code tva} est {@code null} si {@code genererTva = false} dans la requête.
 */
public class CommissionFusionResponse {

    private CommissionResponseDto commission;
    private ImpotEtTaxeResponseDto tva;

    public CommissionFusionResponse(CommissionResponseDto commission, ImpotEtTaxeResponseDto tva) {
        this.commission = commission;
        this.tva        = tva;
    }

    public CommissionResponseDto getCommission() { return commission; }
    public ImpotEtTaxeResponseDto getTva()       { return tva; }
}
