package staport.finance.gBanque.moyensDePaiements.virementInternational.dtos;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursVirementInternationalResponseDto {

    private BigDecimal totalDesEncours;
    private Page<VirementInternationalResponseDto> page;

    public EncoursVirementInternationalResponseDto() {
    }

    public EncoursVirementInternationalResponseDto(BigDecimal totalDesEncours, Page<VirementInternationalResponseDto> page) {
        this.totalDesEncours = totalDesEncours;
        this.page = page;
    }

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<VirementInternationalResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<VirementInternationalResponseDto> page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return "EncoursVirementInternationalResponseDto{" +
                "totalDesEncours=" + totalDesEncours +
                ", page=" + page +
                '}';
    }
}