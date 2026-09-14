package staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class EncoursMiseAdisposition {
    private BigDecimal totalDesEncours ;
    private Page<MiseADispositionResponseDto> page;

    public EncoursMiseAdisposition() {
    }

    public EncoursMiseAdisposition(BigDecimal totalDesEncours, Page<MiseADispositionResponseDto> page) {
        this.totalDesEncours = totalDesEncours;
        this.page = page;
    }

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<MiseADispositionResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<MiseADispositionResponseDto> page) {
        this.page = page;
    }


}
