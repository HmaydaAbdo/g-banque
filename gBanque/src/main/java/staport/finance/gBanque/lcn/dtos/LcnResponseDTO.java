package staport.finance.gBanque.lcn.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class LcnResponseDTO {

    private String numero;
    private OffsetDateTime dateEmission;
    private String beneficiaire;
    private OffsetDateTime dateEcheance;
    private OffsetDateTime dateOperation;
    private OffsetDateTime dateValeur;
    private String libelle;
    private String observation;
    private BigDecimal montant;
    private boolean active;
    private String banqueName;

    public LcnResponseDTO() {}

    public LcnResponseDTO(String numero, OffsetDateTime dateEmission, String beneficiaire,
                          OffsetDateTime dateEcheance, BigDecimal montant, boolean active, String banqueName) {
        this.numero = numero;
        this.dateEmission = dateEmission;
        this.beneficiaire = beneficiaire;
        this.dateEcheance = dateEcheance;
        this.montant = montant;
        this.active = active;
        this.banqueName = banqueName;
    }

    // Getters & Setters
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public OffsetDateTime getDateEmission() { return dateEmission; }
    public void setDateEmission(OffsetDateTime dateEmission) { this.dateEmission = dateEmission; }
    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }
    public OffsetDateTime getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(OffsetDateTime dateEcheance) { this.dateEcheance = dateEcheance; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getBanqueName() {
        return banqueName;
    }
    public void setBanqueName(String banqueName) {
        this.banqueName = banqueName;
    }

    public OffsetDateTime getDateOperation() {
        return dateOperation;
    }
    public void setDateOperation(OffsetDateTime dateOperation) {
        this.dateOperation = dateOperation;
    }
    public OffsetDateTime getDateValeur() {
        return dateValeur;
    }
    public void setDateValeur(OffsetDateTime dateValeur) {
        this.dateValeur = dateValeur;
    }
    public String getLibelle() {
        return libelle;
    }
    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
    public String getObservation() {
        return observation;
    }
    public void setObservation(String observation) {
        this.observation = observation;
    }

    @Override
    public String toString() {
        return "LcnResponseDTO{" +
                "numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", dateEcheance=" + dateEcheance +
                ", montant=" + montant +
                ", active=" + active +
                ", banqueName='" + banqueName + '\'' +
                '}';
    }
}