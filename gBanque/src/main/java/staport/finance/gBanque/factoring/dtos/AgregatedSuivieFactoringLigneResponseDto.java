package staport.finance.gBanque.factoring.dtos;

import org.springframework.data.domain.Page;
import java.math.BigDecimal;

public class AgregatedSuivieFactoringLigneResponseDto {

    private String BanqueName;
    private String marcheName;
    private String marcheNumero;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal reste;
    private Page<SuivieFactoringLineResponseDto> page;

    public AgregatedSuivieFactoringLigneResponseDto() {
    }

    public String getBanqueName() {
        return BanqueName;
    }

    public void setBanqueName(String banqueName) {
        this.BanqueName = banqueName;
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

    public Page<SuivieFactoringLineResponseDto> getPage() {
        return page;
    }

    public void setPage(Page<SuivieFactoringLineResponseDto> page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return "AgregatedSuivieFactoringLigneResponseDto{" +
                "BanqueName='" + BanqueName + '\'' +
                ", marcheName='" + marcheName + '\'' +
                ", marcheNumero='" + marcheNumero + '\'' +
                ", totalDebit=" + totalDebit +
                ", totalCredit=" + totalCredit +
                ", reste=" + reste +
                ", page=" + page +
                '}';
    }
}
