package staport.finance.gBanque.interets.dtos;

import org.springframework.data.domain.Page;
import java.math.BigDecimal;

public class EncoursInteretDto {


    private BigDecimal totalDesEncours;
    private Page<InteretResponseDto> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<InteretResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<InteretResponseDto> page) {
        this.page = page;
    }
}
