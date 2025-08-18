package staport.finance.gBanque.leasing.contratLeasing.dto;

import staport.finance.gBanque.leasing.contratLeasing.enums.TypeContratLeasing;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class ContratLeasingResponseDTO {

    private Long id;
    private String numeroContrat;
    private Long banqueDePrelevementId;
    private String nomBanqueDePrelevement;
    private Long organismeLeasingId;
    private String nomOrganismeLeasing;
    private String immobilisation;
    private BigDecimal montantAchatTTC;
    private OffsetDateTime dateDebut;
    private Integer periodeEnMois;
    private TypeContratLeasing typeContrat;
    private BigDecimal valeurResiduelle;
    private List<ContratLeasingPhaseResponseDTO> phases;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateModification;
    private boolean completed; // ⭐ Nouveau champ de réponse

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroContrat() { return numeroContrat; }
    public void setNumeroContrat(String numeroContrat) { this.numeroContrat = numeroContrat; }
    public Long getBanqueDePrelevementId() { return banqueDePrelevementId; }
    public void setBanqueDePrelevementId(Long banqueDePrelevementId) { this.banqueDePrelevementId = banqueDePrelevementId; }
    public String getNomBanqueDePrelevement() { return nomBanqueDePrelevement; }
    public void setNomBanqueDePrelevement(String nomBanqueDePrelevement) { this.nomBanqueDePrelevement = nomBanqueDePrelevement; }
    public Long getOrganismeLeasingId() { return organismeLeasingId; }
    public void setOrganismeLeasingId(Long organismeLeasingId) { this.organismeLeasingId = organismeLeasingId; }
    public String getNomOrganismeLeasing() { return nomOrganismeLeasing; }
    public void setNomOrganismeLeasing(String nomOrganismeLeasing) { this.nomOrganismeLeasing = nomOrganismeLeasing; }
    public String getImmobilisation() { return immobilisation; }
    public void setImmobilisation(String immobilisation) { this.immobilisation = immobilisation; }
    public BigDecimal getMontantAchatTTC() { return montantAchatTTC; }
    public void setMontantAchatTTC(BigDecimal montantAchatTTC) { this.montantAchatTTC = montantAchatTTC; }
    public OffsetDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(OffsetDateTime dateDebut) { this.dateDebut = dateDebut; }
    public Integer getPeriodeEnMois() { return periodeEnMois; }
    public void setPeriodeEnMois(Integer periodeEnMois) { this.periodeEnMois = periodeEnMois; }
    public TypeContratLeasing getTypeContrat() { return typeContrat; }
    public void setTypeContrat(TypeContratLeasing typeContrat) { this.typeContrat = typeContrat; }
    public BigDecimal getValeurResiduelle() { return valeurResiduelle; }
    public void setValeurResiduelle(BigDecimal valeurResiduelle) { this.valeurResiduelle = valeurResiduelle; }
    public List<ContratLeasingPhaseResponseDTO> getPhases() { return phases; }
    public void setPhases(List<ContratLeasingPhaseResponseDTO> phases) { this.phases = phases; }
    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }
    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }
    public boolean isCompleted() { return completed; } // ⭐ Getter du nouveau champ
    public void setCompleted(boolean completed) { this.completed = completed; } // ⭐ Setter du nouveau champ
}