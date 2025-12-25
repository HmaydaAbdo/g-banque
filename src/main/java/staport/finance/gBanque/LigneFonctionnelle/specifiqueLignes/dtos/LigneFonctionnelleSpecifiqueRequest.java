package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class LigneFonctionnelleSpecifiqueRequest {


    @NotNull(message = "L'ID de la nature de ligne fonctionnelle est obligatoire.")
    private Long natureLigneFonctionnelleId;

    @NotNull(message = "Le plafond est obligatoire.")
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

    private Boolean actif;

    @NotNull(message = "L'ID du marché est obligatoire.")
    private Long marcheId;

    // --- Constructors ---
    public LigneFonctionnelleSpecifiqueRequest() {
    }

    // You will need a new constructor with all fields
    public LigneFonctionnelleSpecifiqueRequest( Long natureLigneFonctionnelleId, BigDecimal plafond, BigDecimal taux, OffsetDateTime dateDepart, OffsetDateTime dateFin, Boolean actif, Long marcheId) {
        this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
        this.plafond = plafond;
        this.taux = taux;
        this.dateDepart = dateDepart;
        this.dateFin = dateFin;
        this.actif = actif;
        this.marcheId = marcheId;
    }

    // --- Getters and Setters ---
    public Long getNatureLigneFonctionnelleId() { return natureLigneFonctionnelleId; }
    public void setNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId) { this.natureLigneFonctionnelleId = natureLigneFonctionnelleId; }
    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }
    public BigDecimal getTaux() { return taux; }
    public void setTaux(BigDecimal taux) { this.taux = taux; }
    public OffsetDateTime getDateDepart() { return dateDepart; }
    public void setDateDepart(OffsetDateTime dateDepart) { this.dateDepart = dateDepart; }
    public OffsetDateTime getDateFin() { return dateFin; }
    public void setDateFin(OffsetDateTime dateFin) { this.dateFin = dateFin; }
    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
    public Long getMarcheId() { return marcheId; }
    public void setMarcheId(Long marcheId) { this.marcheId = marcheId; }

    @Override
    public String toString() {
        return "LigneFonctionnelleSpecifiqueRequest{" +
                ", natureLigneFonctionnelleId=" + natureLigneFonctionnelleId +
                ", plafond=" + plafond +
                ", taux=" + taux +
                ", dateDepart=" + dateDepart +
                ", dateFin=" + dateFin +
                ", actif=" + actif +
                ", marcheId=" + marcheId +
                '}';
    }
}