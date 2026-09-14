package staport.finance.gBanque.moyensDePaiements.lcn.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursLcnResponse {

    private BigDecimal totalEncours;
    private Page<LcnResponseDTO> page;


    public EncoursLcnResponse(BigDecimal totalEncours, Page<LcnResponseDTO> page) {
        this.totalEncours = totalEncours;
        this.page = page;
    }

    public BigDecimal getTotalEncours() {
        return totalEncours;
    }

    public void setTotalEncours(BigDecimal totalEncours) {
        this.totalEncours = totalEncours;
    }

    public Page<LcnResponseDTO> getPage() {
        return page;
    }

    public void setPage(Page<LcnResponseDTO> page) {
        this.page = page;
    }
}
