package staport.finance.gBanque.impot_taxes.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursImpotEtTaxeDto {

    private BigDecimal totalDesEncours;
    private Page<ImpotEtTaxeResponseDto> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<ImpotEtTaxeResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<ImpotEtTaxeResponseDto> page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return "EncoursImpotEtTaxeDto{" +
                "totalDesEncours=" + totalDesEncours +
                ", page=" + page +
                '}';
    }
}