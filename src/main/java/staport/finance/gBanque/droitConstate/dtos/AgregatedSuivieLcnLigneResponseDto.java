package staport.finance.gBanque.droitConstate.dtos;

import org.springframework.data.domain.Page;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.LcnResponseDTO;

import java.math.BigDecimal;

public class AgregatedSuivieLcnLigneResponseDto {

    private String banqueName;
    private BigDecimal total;
    private Page<LcnResponseDTO> page;

    public AgregatedSuivieLcnLigneResponseDto() {
    }

    public String getBanqueName() {
        return banqueName;
    }

    public void setBanqueName(String banqueName) {
        this.banqueName = banqueName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Page<LcnResponseDTO> getPage() {
        return page;
    }

    public void setPage(Page<LcnResponseDTO> page) {
        this.page = page;
    }
}
