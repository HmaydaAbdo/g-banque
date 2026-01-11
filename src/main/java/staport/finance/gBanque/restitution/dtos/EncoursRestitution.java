package staport.finance.gBanque.restitution.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursRestitution {

    private BigDecimal totalDesEncours;
    private Page<RestitutionResponseDto> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<RestitutionResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<RestitutionResponseDto> page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return "EncoursRestitution{" +
                "totalDesEncours=" + totalDesEncours +
                ", page=" + page +
                '}';
    }
}