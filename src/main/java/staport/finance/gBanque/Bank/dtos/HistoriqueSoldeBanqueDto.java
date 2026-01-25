package staport.finance.gBanque.Bank.dtos;

import java.math.BigDecimal;

public class HistoriqueSoldeBanqueDto {

    private Long id;
    private Long banqueId;
    private String nomBanque;
    private Integer mois;
    private Integer annee;
    private BigDecimal soldeDepart;
    private BigDecimal soldeFin;

    // --- Constructors ---

    public HistoriqueSoldeBanqueDto() {}

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public Integer getMois() {
        return mois;
    }

    public void setMois(Integer mois) {
        this.mois = mois;
    }

    public Integer getAnnee() {
        return annee;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }

    public BigDecimal getSoldeDepart() {
        return soldeDepart;
    }

    public void setSoldeDepart(BigDecimal soldeDepart) {
        this.soldeDepart = soldeDepart;
    }

    public BigDecimal getSoldeFin() {
        return soldeFin;
    }

    public void setSoldeFin(BigDecimal soldeFin) {
        this.soldeFin = soldeFin;
    }
}