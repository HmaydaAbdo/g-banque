package staport.finance.gBanque.marches.dtos;


import java.math.BigDecimal;
import java.time.OffsetDateTime;


public class MarcheResponseDTO {

    private Long id;
    private String numero;
    private String nom;
    private BigDecimal montant;
    private String maitreDouvrage;
    private OffsetDateTime delai;
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

    public OffsetDateTime getDelai() {
        return delai;
    }

    public void setDelai(OffsetDateTime delai) {
        this.delai = delai;
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