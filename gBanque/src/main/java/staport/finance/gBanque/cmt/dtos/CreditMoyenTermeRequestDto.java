package staport.finance.gBanque.cmt.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO utilisé pour la création ou la mise à jour d'un crédit moyen terme.
 * Contient les champs nécessaires à la logique métier et les validations de base.
 */
public class CreditMoyenTermeRequestDto {

    @NotNull(message = "L'ID de la banque est obligatoire")
    private Long banqueId;
    @NotBlank(message = "La désignation est obligatoire")
    private String designation;
    @NotNull(message = "Le nominal est obligatoire")
    @DecimalMin(value = "0.01", message = "Le nominal doit être supérieur à zéro")
    private BigDecimal nominal;
    @NotNull(message = "La périodicité est obligatoire")
    private Integer periodicite;
    @NotNull(message = "La durée est obligatoire")
    private Integer duree;
    @NotNull(message = "Le montant de l'échéance est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant de l'échéance doit être supérieur à zéro")
    private BigDecimal echeance;
    @NotNull(message = "Le taux est obligatoire")
    @DecimalMin(value = "0.01", message = "Le taux doit être supérieur à zéro")
    private BigDecimal taux;
    @NotNull(message = "La date de début est obligatoire")
    private OffsetDateTime dateDebut;

    // Constructeur par défaut
    public CreditMoyenTermeRequestDto() {}

    // Getters et Setters pour tous les champs
    public Long getBanqueId() { return banqueId; }
    public void setBanqueId(Long banqueId) { this.banqueId = banqueId; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public BigDecimal getNominal() { return nominal; }
    public void setNominal(BigDecimal nominal) { this.nominal = nominal; }
    public Integer getPeriodicite() { return periodicite; }
    public void setPeriodicite(Integer periodicite) { this.periodicite = periodicite; }
    public Integer getDuree() { return duree; }
    public void setDuree(Integer duree) { this.duree = duree; }
    public BigDecimal getEcheance() { return echeance; }
    public void setEcheance(BigDecimal echeance) { this.echeance = echeance; }
    public BigDecimal getTaux() { return taux; }
    public void setTaux(BigDecimal taux) { this.taux = taux; }
    public OffsetDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(OffsetDateTime dateDebut) { this.dateDebut = dateDebut; }
}