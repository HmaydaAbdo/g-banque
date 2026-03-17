package staport.finance.gBanque.leasing.contratLeasing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import staport.finance.gBanque.leasing.contratLeasing.enums.TypeContratLeasing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ContratLeasingRequestDTO {
    private Long id;
    @NotBlank(message = "Le numéro de contrat est obligatoire")
    @Size(max = 50, message = "Le numéro de contrat ne doit pas dépasser 50 caractères")
    private String numeroContrat;

    @NotNull(message = "L'ID de la banque de prélèvement est obligatoire")
    private Long banqueDePrelevementId; // Reference by ID for related entities

    @NotNull(message = "L'ID de l'organisme de leasing est obligatoire")
    private Long organismeLeasingId; // Reference by ID for related entities

    @NotBlank(message = "L'immobilisation est obligatoire")
    @Size(max = 255, message = "L'immobilisation ne doit pas dépasser 255 caractères")
    private String immobilisation;

    @NotNull(message = "Le montant d'achat est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant d'achat doit être supérieur à zéro")
    @Positive(message = "Le montant d'achat TTC est obligatoire et doit être positif.")
    private BigDecimal montantAchatTTC;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut ;

    @NotNull(message = "La période en mois est obligatoire")
    @Min(value = 1, message = "La période en mois doit être d'au moins 1")
    @Positive(message = "La période en mois doit être un entier positif")
    private Integer periodeEnMois;

    @NotNull(message = "Le type de contrat de leasing est obligatoire")
    private TypeContratLeasing typeContrat;
    @NotNull(message = "la valeur résiduelle est obligatoire")

    @Positive(message = "la valeur résiduelle doit être positif ")
    private BigDecimal valeurResiduelle;

    @Valid
    @NotEmpty(message = "Phases list must not be empty")
    private List<ContratLeasingPhaseRequestDTO> phases;

    // Getters and Setters
    public String getNumeroContrat() { return numeroContrat; }
    public void setNumeroContrat(String numeroContrat) { this.numeroContrat = numeroContrat; }
    public Long getBanqueDePrelevementId() { return banqueDePrelevementId; }
    public void setBanqueDePrelevementId(Long banqueDePrelevementId) { this.banqueDePrelevementId = banqueDePrelevementId; }
    public Long getOrganismeLeasingId() { return organismeLeasingId; }
    public void setOrganismeLeasingId(Long organismeLeasingId) { this.organismeLeasingId = organismeLeasingId; }
    public String getImmobilisation() { return immobilisation; }
    public void setImmobilisation(String immobilisation) { this.immobilisation = immobilisation; }
    public BigDecimal getMontantAchatTTC() { return montantAchatTTC; }
    public void setMontantAchatTTC(BigDecimal montantAchatTTC) { this.montantAchatTTC = montantAchatTTC; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public Integer getPeriodeEnMois() { return periodeEnMois; }
    public void setPeriodeEnMois(Integer periodeEnMois) { this.periodeEnMois = periodeEnMois; }
    public TypeContratLeasing getTypeContrat() { return typeContrat; }
    public void setTypeContrat(TypeContratLeasing typeContrat) { this.typeContrat = typeContrat; }
    public List<ContratLeasingPhaseRequestDTO> getPhases() { return phases; }
    public void setPhases(List<ContratLeasingPhaseRequestDTO> phases) { this.phases = phases; }

    public BigDecimal getValeurResiduelle() {
        return valeurResiduelle;
    }

    public void setValeurResiduelle(BigDecimal valeurResiduelle) {
        this.valeurResiduelle = valeurResiduelle;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ContratLeasingRequestDTO{" +
                "id=" + id +
                ", numeroContrat='" + numeroContrat + '\'' +
                ", banqueDePrelevementId=" + banqueDePrelevementId +
                ", organismeLeasingId=" + organismeLeasingId +
                ", immobilisation='" + immobilisation + '\'' +
                ", montantAchatTTC=" + montantAchatTTC +
                ", dateDebut=" + dateDebut +
                ", periodeEnMois=" + periodeEnMois +
                ", typeContrat=" + typeContrat +
                ", valeurResiduelle=" + valeurResiduelle +
                ", phases=" + phases +
                '}';
    }
}