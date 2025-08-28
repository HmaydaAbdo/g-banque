package staport.finance.gBanque.LigneFonctionnelle.dtos;

public class LigneFonctionnelleSearchCriteria {
    private Long banqueId;
    private Long natureLigneFonctionnelleId;
    private Integer yearDepart;
    private Boolean actif;
    private Boolean isExpired;

    // --- Constructors ---
    public LigneFonctionnelleSearchCriteria() {
    }

    public LigneFonctionnelleSearchCriteria(Long banqueId, Long natureLigneFonctionnelleId, Integer yearDepart, Boolean actif, Boolean isExpired) {
        this.banqueId = banqueId;
        this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
        this.yearDepart = yearDepart;
        this.actif = actif;
        this.isExpired = isExpired;
    }

    // --- Getters and Setters ---
    public Long getBanqueId() { return banqueId; }
    public void setBanqueId(Long banqueId) { this.banqueId = banqueId; }

    public Long getNatureLigneFonctionnelleId() {
        return natureLigneFonctionnelleId;
    }
    public void setNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId) {
        this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
    }

    public Integer getYearDepart() {
        return yearDepart;
    }
    public void setYearDepart(Integer yearDepart) {
        this.yearDepart = yearDepart;
    }

    public Boolean getActif() {
        return actif;
    }
    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public Boolean getIsExpired() {
        return isExpired;
    }
    public void setIsExpired(Boolean expired) {
        this.isExpired = expired;
    }

    @Override
    public String toString() {
        return "LigneFonctionnelleSearchCriteria{" +
                "banqueId=" + banqueId +
                ", natureLigneFonctionnelleId=" + natureLigneFonctionnelleId +
                ", yearDepart=" + yearDepart +
                ", actif=" + actif +
                ", isExpired=" + isExpired +
                '}';
    }
}