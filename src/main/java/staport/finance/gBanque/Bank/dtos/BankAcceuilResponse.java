package staport.finance.gBanque.Bank.dtos;

import staport.finance.gBanque.Bank.enums.Devise;

import java.math.BigDecimal;

public class BankAcceuilResponse {
    private  Long bankId;
    private String logoBase64;
    private String name;
    private BigDecimal soldeActuelle;
    private BigDecimal soldeDepart;
    private BigDecimal plafondFC;
    private Devise devise;

    public Long getBankId() {
        return bankId;
    }

    public void setBankId(Long bankId) {
        this.bankId = bankId;
    }

    public BankAcceuilResponse() {
    }

    // Getters and Setters
    public String getLogoBase64() {
        return logoBase64;
    }
    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public BigDecimal getSoldeActuelle() {
        return soldeActuelle;
    }
    public void setSoldeActuelle(BigDecimal soldeActuelle) {
        this.soldeActuelle = soldeActuelle;
    }
    public BigDecimal getPlafondFC() {
        return plafondFC;
    }
    public void setPlafondFC(BigDecimal plafondFC) {
        this.plafondFC = plafondFC;
    }
    public BigDecimal getSoldeDepart() {
        return soldeDepart;
    }
    public void setSoldeDepart(BigDecimal soldeDepart) {
        this.soldeDepart = soldeDepart;
    }
    public Devise getDevise() {
        return devise;
    }
    public void setDevise(Devise devise) {
        this.devise = devise;
    }

    @Override
    public String toString() {
        return "BankAcceuilResponse{" +
                "bankId=" + bankId +
                ", logoBase64='" + logoBase64 + '\'' +
                ", name='" + name + '\'' +
                ", soldeActuelle=" + soldeActuelle +
                ", soldeDepart=" + soldeDepart +
                ", plafondFC=" + plafondFC +
                ", devise=" + devise +
                '}';
    }
}