package staport.finance.gBanque.transactions.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class AgreratedTransactionResponseDto {

    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal totalTvaDeductible;
    private BigDecimal totalTvaCollectee;
    private Page<TransactionResponseDto> page;

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

    public BigDecimal getTotalTvaDeductible() {
        return totalTvaDeductible;
    }

    public void setTotalTvaDeductible(BigDecimal totalTvaDeductible) {
        this.totalTvaDeductible = totalTvaDeductible;
    }

    public BigDecimal getTotalTvaCollectee() {
        return totalTvaCollectee;
    }

    public void setTotalTvaCollectee(BigDecimal totalTvaCollectee) {
        this.totalTvaCollectee = totalTvaCollectee;
    }

    public Page<TransactionResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<TransactionResponseDto> page) {
        this.page = page;
    }
}
