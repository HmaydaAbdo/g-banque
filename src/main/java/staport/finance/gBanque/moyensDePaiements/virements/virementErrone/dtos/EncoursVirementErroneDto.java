package staport.finance.gBanque.moyensDePaiements.virements.virementErrone.dtos;

import org.springframework.data.domain.Page;
import java.math.BigDecimal;

public class EncoursVirementErroneDto {

    private BigDecimal totalDesEncours;
    private Page<VirementErroneResponseDto> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<VirementErroneResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<VirementErroneResponseDto> page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return "EncoursVirementErroneDto{" +
                "totalDesEncours=" + totalDesEncours +
                ", page=" + page +
                '}';
    }
}