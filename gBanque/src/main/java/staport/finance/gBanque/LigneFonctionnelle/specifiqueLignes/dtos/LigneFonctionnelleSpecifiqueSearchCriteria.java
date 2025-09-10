package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos;

public class LigneFonctionnelleSpecifiqueSearchCriteria {


    private Long banqueId;
    private Long natureLigneFonctionnelleId;
    private Integer yearDepart;
    private Boolean actif;
    private Boolean isExpired;
    private Long marcheId;

    // --- Constructors ---
    public LigneFonctionnelleSpecifiqueSearchCriteria() {
    }

    public LigneFonctionnelleSpecifiqueSearchCriteria(Long banqueId, Long natureLigneFonctionnelleId, Integer yearDepart, Boolean actif, Boolean isExpired, Long marcheId) {
        this.banqueId = banqueId;
        this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
        this.yearDepart = yearDepart;
        this.actif = actif;
        this.isExpired = isExpired;
        this.marcheId = marcheId;
    }

    // --- Getters and Setters ---
    public Long getBanqueId() { return banqueId; }
    public void setBanqueId(Long banqueId) { this.banqueId = banqueId; }

    public Long getNatureLigneFonctionnelleId() { return natureLigneFonctionnelleId; }
    public void setNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId) { this.natureLigneFonctionnelleId = natureLigneFonctionnelleId; }

    public Integer getYearDepart() { return yearDepart; }
    public void setYearDepart(Integer yearDepart) { this.yearDepart = yearDepart; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Boolean getIsExpired() { return isExpired; }
    public void setIsExpired(Boolean isExpired) { this.isExpired = isExpired; }

    public Long getMarcheId() { return marcheId; }
    public void setMarcheId(Long marcheId) { this.marcheId = marcheId; }

    @Override
    public String toString() {
        return "LigneFonctionnelleSpecifiqueSearchCriteria{" +
                "banqueId=" + banqueId +
                ", natureLigneFonctionnelleId=" + natureLigneFonctionnelleId +
                ", yearDepart=" + yearDepart +
                ", actif=" + actif +
                ", isExpired=" + isExpired +
                ", marcheId=" + marcheId +
                '}';
    }
}