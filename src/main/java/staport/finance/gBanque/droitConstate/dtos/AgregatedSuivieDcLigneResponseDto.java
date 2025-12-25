package staport.finance.gBanque.droitConstate.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public class AgregatedSuivieDcLigneResponseDto {


    private String BanqueName;
    private String marcheName;
    private String marcheNumero;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal reste;
    private Page<SuivieDcLigneResponseDto> page;

    public AgregatedSuivieDcLigneResponseDto() {
    }

    public String getBanqueName() {
        return BanqueName;
    }

    public void setBanqueName(String banqueName) {
        BanqueName = banqueName;
    }

    public String getMarcheName() {
        return marcheName;
    }

    public void setMarcheName(String marcheName) {
        this.marcheName = marcheName;
    }

    public String getMarcheNumero() {
        return marcheNumero;
    }

    public void setMarcheNumero(String marcheNumero) {
        this.marcheNumero = marcheNumero;
    }

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

    public BigDecimal getReste() {
        return reste;
    }

    public void setReste(BigDecimal reste) {
        this.reste = reste;
    }

    public Page<SuivieDcLigneResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<SuivieDcLigneResponseDto> page) {
        this.page = page;
    }
}
