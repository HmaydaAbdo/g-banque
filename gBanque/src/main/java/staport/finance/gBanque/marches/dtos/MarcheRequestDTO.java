package staport.finance.gBanque.marches.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class MarcheRequestDTO {

    @NotNull(message = "Le marche doit etre rattache a une banque")
    private Long banqueId;

    @NotBlank(message = "Le numero du marche ne peut pas etre vide")
    @Size(max = 50, message = "Le numero doit avoir au maximum 50 caracteres")
    private String numero;

    @NotBlank(message = "Le nom du marche ne peut pas etre vide")
    @Size(max = 255, message = "Le nom doit avoir au maximum 255 caracteres")
    private String nom;

    @NotNull(message = "Le montant ne peut pas etre nul")
    @DecimalMin(value = "0.01", message = "Le montant doit etre superieur a zero")
    private BigDecimal montant;

    @NotBlank(message = "Le maitre d'ouvrage ne peut pas etre vide")
    @Size(max = 255, message = "Le maitre d'ouvrage doit avoir au maximum 255 caracteres")
    private String maitreDouvrage;

    @NotNull(message = "La date de debut des travaux ne peut pas etre nulle")
    private OffsetDateTime dateDebutDesTravaux;

    @NotNull(message = "La date d'achevement des travaux ne peut pas etre nulle")
    private OffsetDateTime dateDAchievementDesTravaux;

    // Getters and Setters


    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getMaitreDouvrage() {
        return maitreDouvrage;
    }

    public void setMaitreDouvrage(String maitreDouvrage) {
        this.maitreDouvrage = maitreDouvrage;
    }

    public OffsetDateTime getDateDebutDesTravaux() {
        return dateDebutDesTravaux;
    }

    public void setDateDebutDesTravaux(OffsetDateTime dateDebutDesTravaux) {
        this.dateDebutDesTravaux = dateDebutDesTravaux;
    }

    public OffsetDateTime getDateDAchievementDesTravaux() {
        return dateDAchievementDesTravaux;
    }

    public void setDateDAchievementDesTravaux(OffsetDateTime dateDAchievementDesTravaux) {
        this.dateDAchievementDesTravaux = dateDAchievementDesTravaux;
    }
}