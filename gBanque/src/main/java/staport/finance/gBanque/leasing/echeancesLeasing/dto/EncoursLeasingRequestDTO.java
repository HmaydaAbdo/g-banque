package staport.finance.gBanque.leasing.echeancesLeasing.dto;
import jakarta.validation.constraints.NotNull;
;import java.math.BigDecimal;

/**
 * DTO utilisé pour requêter les encours d'échéances de leasing.
 * Contient les critères de recherche pour identifier la banque et la période de calcul.
 */
public class EncoursLeasingRequestDTO {

    @NotNull(message = "L'identifiant de la banque est obligatoire.")
    private Long banqueId;

    private Long organismeLeasingId;

    private String numeroContrat;

    private BigDecimal montant;

    // --- Getters et Setters ---

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }


    public Long getOrganismeLeasingId() {
        return organismeLeasingId;
    }

    public void setOrganismeLeasingId(Long organismeLeasingId) {
        this.organismeLeasingId = organismeLeasingId;
    }

    public String getNumeroContrat() {
        return numeroContrat;
    }

    public void setNumeroContrat(String numeroContrat) {
        this.numeroContrat = numeroContrat;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    @Override
    public String toString() {
        return "EncoursLeasingRequestDTO{" +
                "banqueId=" + banqueId +
                ", organismeLeasingId=" + organismeLeasingId +
                ", numeroContrat='" + numeroContrat + '\'' +
                ", montant=" + montant +
                '}';
    }
}