package staport.finance.gBanque.impayees.dtos;


import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursImpayeeDto {

    private BigDecimal totalDesEncours;
    private Page<ImpayeeResponseDto> page;

    public EncoursImpayeeDto() {
    }

    public EncoursImpayeeDto(BigDecimal totalDesEncours, Page<ImpayeeResponseDto> page) {
        this.totalDesEncours = totalDesEncours;
        this.page = page;
    }

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<ImpayeeResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<ImpayeeResponseDto> page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return "EncoursImpayeeDto{" +
                "totalDesEncours=" + totalDesEncours +
                ", page=" + page +
                '}';
    }
}