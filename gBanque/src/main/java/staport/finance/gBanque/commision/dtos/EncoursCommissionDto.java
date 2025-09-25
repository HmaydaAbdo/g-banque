package staport.finance.gBanque.commision.dtos;

import org.springframework.data.domain.Page;
import java.math.BigDecimal;

public class EncoursCommissionDto {
    private BigDecimal totalDesEncours;
    private Page<CommissionResponseDto> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<CommissionResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<CommissionResponseDto> page) {
        this.page = page;
    }
}