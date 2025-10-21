package staport.finance.gBanque.LigneFonctionnelle.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class LigneFonctionnelleRequest {

    @NotNull(message = "L'ID de la banque est obligatoire.")
    private Long banqueId;

    @NotNull(message = "L'ID de la nature de ligne fonctionnelle est obligatoire.")
    private Long natureLigneFonctionnelleId;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le plafond doit être supérieur à zéro.")
    private BigDecimal plafond;

    @NotNull(message = "Le taux est obligatoire.")
    @DecimalMin(value = "0.0", message = "Le taux ne peut pas être négatif.")
    @DecimalMax(value = "100.0", message = "Le taux ne peut pas dépasser 100.")
    private BigDecimal taux;

    @NotNull(message = "La date de départ est obligatoire.")
    private OffsetDateTime dateDepart;

    @NotNull(message = "La date de fin est obligatoire.")
    private OffsetDateTime dateFin;

    @Valid
    private List<SousLigneRequestDto> sousLignes;

    // --- Constructors ---
    public LigneFonctionnelleRequest() {
    }


    public LigneFonctionnelleRequest(Long banqueId, Long natureLigneFonctionnelleId, BigDecimal plafond, BigDecimal taux, OffsetDateTime dateDepart, OffsetDateTime dateFin, Boolean actif) {
        this.banqueId = banqueId;
        this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
        this.plafond = plafond;
        this.taux = taux;
        this.dateDepart = dateDepart;
        this.dateFin = dateFin;

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

    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }
    public BigDecimal getTaux() { return taux; }
    public void setTaux(BigDecimal taux) { this.taux = taux; }
    public OffsetDateTime getDateDepart() {return dateDepart;}
    public void setDateDepart(OffsetDateTime dateDepart) {this.dateDepart = dateDepart;}
    public OffsetDateTime getDateFin() {return dateFin;}
    public void setDateFin(OffsetDateTime dateFin) {this.dateFin = dateFin;}

    public List<SousLigneRequestDto> getSousLignes() { return sousLignes; }
    public void setSousLignes(List<SousLigneRequestDto> sousLignes) { this.sousLignes = sousLignes; }
}