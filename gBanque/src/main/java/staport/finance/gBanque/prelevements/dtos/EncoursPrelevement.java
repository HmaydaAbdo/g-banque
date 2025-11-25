package staport.finance.gBanque.prelevements.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursPrelevement {

    private BigDecimal totalDesEncours;
    private Page<PrelevementResponseDto> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<PrelevementResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<PrelevementResponseDto> page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return "EncoursPrelevement{" +
                "totalDesEncours=" + totalDesEncours +
                ", page=" + page +
                '}';
    }
}
