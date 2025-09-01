package staport.finance.gBanque.cheques.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ChequeResponseDto {

    private String numero;
    private OffsetDateTime dateEmission;
    private OffsetDateTime dateEcheance;
    private String beneficiaire;
    private BigDecimal montant;
    private boolean active;
    private String nomBanque;

    // Default constructor
    public ChequeResponseDto() {
    }

    // Constructor with all fields
    public ChequeResponseDto(String numero, OffsetDateTime dateEmission,OffsetDateTime dateEcheance, String beneficiaire, BigDecimal montant, boolean active, String nomBanque) {
        this.numero = numero;
        this.dateEmission = dateEmission;
        this.dateEcheance = dateEcheance;
        this.beneficiaire = beneficiaire;
        this.montant = montant;
        this.active = active;
        this.nomBanque = nomBanque;
    }

    // Getters and Setters
    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public OffsetDateTime getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(OffsetDateTime dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }
}