package staport.finance.gBanque.Agios.dtos;


import java.math.BigDecimal;

public class AgiosDetailAggregateDto {

    private BigDecimal totalAgiosHT;
    private BigDecimal totalTVA;
    private BigDecimal totalAgiosTTC;
    private int nbJoursDecouvert;
    private int nbJoursTotal;

    // constructeur, getters, setters

    public AgiosDetailAggregateDto() {}

    public AgiosDetailAggregateDto(BigDecimal totalAgiosHT, BigDecimal totalTVA,
                                   BigDecimal totalAgiosTTC, int nbJoursDecouvert, int nbJoursTotal) {
        this.totalAgiosHT = totalAgiosHT;
        this.totalTVA = totalTVA;
        this.totalAgiosTTC = totalAgiosTTC;
        this.nbJoursDecouvert = nbJoursDecouvert;
        this.nbJoursTotal = nbJoursTotal;
    }

    public BigDecimal getTotalAgiosHT() { return totalAgiosHT; }
    public void setTotalAgiosHT(BigDecimal totalAgiosHT) { this.totalAgiosHT = totalAgiosHT; }
    public BigDecimal getTotalTVA() { return totalTVA; }
    public void setTotalTVA(BigDecimal totalTVA) { this.totalTVA = totalTVA; }
    public BigDecimal getTotalAgiosTTC() { return totalAgiosTTC; }
    public void setTotalAgiosTTC(BigDecimal totalAgiosTTC) { this.totalAgiosTTC = totalAgiosTTC; }
    public int getNbJoursDecouvert() { return nbJoursDecouvert; }
    public void setNbJoursDecouvert(int nbJoursDecouvert) { this.nbJoursDecouvert = nbJoursDecouvert; }
    public int getNbJoursTotal() { return nbJoursTotal; }
    public void setNbJoursTotal(int nbJoursTotal) { this.nbJoursTotal = nbJoursTotal; }
}