package staport.finance.gBanque.decomptes.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursDecompteDto {

    private BigDecimal totalDesEncours;
    private Page<DecompteResponseDto> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<DecompteResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<DecompteResponseDto> page) {
        this.page = page;
    }
}
