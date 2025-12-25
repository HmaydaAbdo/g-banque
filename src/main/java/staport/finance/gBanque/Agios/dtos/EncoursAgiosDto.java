package staport.finance.gBanque.Agios.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursAgiosDto {

    private BigDecimal totalDesEncours;
    private Page<AgiosResponseDto> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<AgiosResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<AgiosResponseDto> page) {
        this.page = page;
    }
}
