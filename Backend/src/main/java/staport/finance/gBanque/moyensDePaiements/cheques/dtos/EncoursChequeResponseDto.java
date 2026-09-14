package staport.finance.gBanque.moyensDePaiements.cheques.dtos;


import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursChequeResponseDto {

    private BigDecimal totalDesEncours;
    private Page<ChequeResponseDto> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public EncoursChequeResponseDto() {
    }

    public EncoursChequeResponseDto(BigDecimal totalDesEncours, Page<ChequeResponseDto> page) {
        this.totalDesEncours = totalDesEncours;
        this.page = page;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<ChequeResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<ChequeResponseDto> page) {
        this.page = page;
    }
}
