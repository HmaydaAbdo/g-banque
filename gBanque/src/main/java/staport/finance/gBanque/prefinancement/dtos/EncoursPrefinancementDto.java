package staport.finance.gBanque.prefinancement.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursPrefinancementDto {

    private BigDecimal totalDesEncours;
    private Page<PrefinancementResponseDTO> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<PrefinancementResponseDTO> getPage() {
        return page;
    }

    public void setPage(Page<PrefinancementResponseDTO> page) {
        this.page = page;
    }
}
