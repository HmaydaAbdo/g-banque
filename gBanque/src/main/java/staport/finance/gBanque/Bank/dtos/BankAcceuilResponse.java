package staport.finance.gBanque.Bank.dtos;

import java.math.BigDecimal;

public class BankAcceuilResponse {
    private  Long bankId;
    private String logoBase64;
    private String name;
    private BigDecimal soldeActuelle;
    private BigDecimal plafondFC;

    public Long getBankId() {
        return bankId;
    }

    public void setBankId(Long bankId) {
        this.bankId = bankId;
    }

    public BankAcceuilResponse() {
    }

    public BankAcceuilResponse(String logoBase64, String name, BigDecimal soldeActuelle, BigDecimal plafondFC) {
        this.logoBase64 = logoBase64;
        this.name = name;
        this.soldeActuelle = soldeActuelle;
        this.plafondFC = plafondFC;
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
}