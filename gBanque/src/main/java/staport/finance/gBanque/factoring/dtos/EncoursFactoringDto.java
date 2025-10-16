package staport.finance.gBanque.factoring.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursFactoringDto {

    private BigDecimal totalDesEncours;
    private Page<FactoringResponseDto> page;

    public EncoursFactoringDto() {
    }

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<FactoringResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<FactoringResponseDto> page) {
        this.page = page;
    }
}
