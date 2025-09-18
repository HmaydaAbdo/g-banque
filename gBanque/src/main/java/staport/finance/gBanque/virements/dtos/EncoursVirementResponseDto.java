package staport.finance.gBanque.virements.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursVirementResponseDto {

    private BigDecimal totalDesEncours;
    private Page<VirementResponseDto> page;

    public EncoursVirementResponseDto() {
    }

    public EncoursVirementResponseDto(BigDecimal totalDesEncours, Page<VirementResponseDto> page) {
        this.totalDesEncours = totalDesEncours;
        this.page = page;
    }

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<VirementResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<VirementResponseDto> page) {
        this.page = page;
    }
}
