package staport.finance.gBanque.marches.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class MarcheResponseDTO {

    private Long id;
    private String numero;
    private String nom;
    private BigDecimal montant;
    private String maitreDouvrage;
    private OffsetDateTime dateDebutDesTravaux;
    private OffsetDateTime dateDAchievementDesTravaux;
    private Long banqueId;
    private String banqueNom;
    private boolean isNanti;

    private OffsetDateTime createdAt;
    private OffsetDateTime modifiedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getBanqueNom() {
        return banqueNom;
    }

    public void setBanqueNom(String banqueNom) {
        this.banqueNom = banqueNom;
    }

    public boolean isNanti() {
        return isNanti;
    }

    public void setNanti(boolean nanti) {
        isNanti = nanti;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(OffsetDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}