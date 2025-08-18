package staport.finance.gBanque.leasing.echeancesLeasing.dto;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.PeriodeEncours;

/**
 * DTO utilisé pour requêter les encours d'échéances de leasing.
 * Contient les critères de recherche pour identifier la banque et la période de calcul.
 */
public class EncoursLeasingRequestDTO {

    @NotNull(message = "L'identifiant de la banque est obligatoire.")
    private Long banqueId;

    @NotNull(message = "La période de calcul des encours est obligatoire (JOUR ou MOIS).")
    private PeriodeEncours periode;

    // Nouveau champ optionnel pour le filtre par organisme de leasing
    private Long organismeLeasingId;

    // --- Getters et Setters ---

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public PeriodeEncours getPeriode() {
        return periode;
    }

    public void setPeriode(PeriodeEncours periode) {
        this.periode = periode;
    }

    public Long getOrganismeLeasingId() {
        return organismeLeasingId;
    }

    public void setOrganismeLeasingId(Long organismeLeasingId) {
        this.organismeLeasingId = organismeLeasingId;
    }
}