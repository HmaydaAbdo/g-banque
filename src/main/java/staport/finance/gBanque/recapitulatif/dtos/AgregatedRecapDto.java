package staport.finance.gBanque.recapitulatif.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class AgregatedRecapDto {

    BigDecimal totalDebit;
    BigDecimal totalCredit;
    Page<RecaputilatifItem> page;

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }

    public Page<RecaputilatifItem> getPage() {
        return page;
    }

    public void setPage(Page<RecaputilatifItem> page) {
        this.page = page;
    }
}
