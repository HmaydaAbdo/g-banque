package staport.finance.gBanque.fraisBancaires.dtos;

import org.springframework.data.domain.Page;
import java.math.BigDecimal;

public class EncoursFraisBancaireDto {

    private BigDecimal totalDesEncours;
    private Page<FraisBancaireResposneDto> page;

    public BigDecimal getTotalDesEncours() {
        return totalDesEncours;
    }

    public void setTotalDesEncours(BigDecimal totalDesEncours) {
        this.totalDesEncours = totalDesEncours;
    }

    public Page<FraisBancaireResposneDto> getPage() {
        return page;
    }

    public void setPage(Page<FraisBancaireResposneDto> page) {
        this.page = page;
    }
}
