package staport.finance.gBanque.LigneFonctionnelle.dtos;



public class LigneFonctionnelleSearchCriteria {
    private Long banqueId;
    private Long natureLigneFonctionnelleId;

    // --- Constructors ---
    public LigneFonctionnelleSearchCriteria() {
    }

    public LigneFonctionnelleSearchCriteria(Long banqueId, Long natureLigneFonctionnelleId, String natureLigneFonctionnelleLabel ) {
        this.banqueId = banqueId;
        this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
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



}
