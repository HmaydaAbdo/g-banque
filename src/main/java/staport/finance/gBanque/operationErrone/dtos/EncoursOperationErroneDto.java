package staport.finance.gBanque.operationErrone.dtos;

import org.springframework.data.domain.Page;
import java.math.BigDecimal;

public class EncoursOperationErroneDto {

    private BigDecimal totalDesEncours;
    private Page<OperationErroneResponseDto> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<OperationErroneResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<OperationErroneResponseDto> page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return "EncoursOperationErroneDto{" +
                "totalDesEncours=" + totalDesEncours +
                ", page=" + page +
                '}';
    }
}