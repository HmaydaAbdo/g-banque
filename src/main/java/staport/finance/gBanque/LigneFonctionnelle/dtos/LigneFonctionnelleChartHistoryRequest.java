package staport.finance.gBanque.LigneFonctionnelle.dtos;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class LigneFonctionnelleChartHistoryRequest {

    @NotNull(message = "Banque ID cannot be null")
    @Positive(message = "Banque ID must be a positive number")
    private Long banqueId;

    @NotNull(message = "Nature Ligne Fonctionnelle ID cannot be null")
    @Positive(message = "Nature Ligne Fonctionnelle ID must be a positive number")
    private Long natureLigneFonctionnelleId;



    // --- Constructors ---
    public LigneFonctionnelleChartHistoryRequest() {
    }

    public LigneFonctionnelleChartHistoryRequest(Long banqueId, Long natureLigneFonctionnelleId) {
        this.banqueId = banqueId;
        this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
    }

    // --- Getters and Setters ---
    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public Long getNatureLigneFonctionnelleId() {
        return natureLigneFonctionnelleId;
    }

    public void setNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId) {
        this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
    }

    // --- toString (optional) ---
    @Override
    public String toString() {
        return "LigneFonctionnelleChartHistoryRequest{" +
                "banqueId=" + banqueId +
                ", natureLigneFonctionnelleId=" + natureLigneFonctionnelleId +
                '}';
    }
}