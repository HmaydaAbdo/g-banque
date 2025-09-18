package staport.finance.gBanque.droitConstate.dtos;

import org.springframework.data.domain.Page;


import java.math.BigDecimal;

public class EncoursDroitConstateDto {
    private BigDecimal totalDesEncours;
    private Page<DroitConstateResponseDto> page;

    public Page<DroitConstateResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<DroitConstateResponseDto> page) {
        this.page = page;
    }

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }
}
