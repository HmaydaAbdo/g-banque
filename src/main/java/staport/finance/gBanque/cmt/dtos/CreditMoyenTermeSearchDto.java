package staport.finance.gBanque.cmt.dtos;

/**
 * DTO utilisé pour la recherche et le filtrage de crédits moyen terme.
 * Tous les champs sont optionnels pour permettre des requêtes flexibles.
 */
public class CreditMoyenTermeSearchDto {

    private Long banqueId;
    private String designation;
    private Boolean completed;

    public CreditMoyenTermeSearchDto() {}

    // Getters et Setters pour tous les champs
    public Long getBanqueId() { return banqueId; }
    public void setBanqueId(Long banqueId) { this.banqueId = banqueId; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    @Override
    public String toString() {
        return "CreditMoyenTermeSearchDto{" +
                "banqueId=" + banqueId +
                ", designation='" + designation + '\'' +
                ", completed=" + completed +
                '}';
    }
}