package staport.finance.gBanque.cheques.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;

import java.math.BigDecimal;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "CHEQUES") // Good practice to use plural for table names
public class Cheque {

    @Id // 'numero' is the primary key
    @Column(name = "numero", nullable = false, unique = true)
    private String numero;

    @Column(name = "date_emission", nullable = false)
    private OffsetDateTime dateEmission;

    @Column(name = "beneficiaire", nullable = false)
    private String beneficiaire;

    @Column(name = "date_echeance") // nullable = true is default
    private OffsetDateTime dateEcheance;

    private OffsetDateTime dateOperation ;
    private OffsetDateTime dateValeur;

    @Column(name = "montant", nullable = false, precision = 19, scale = 2) // Precision and scale for BigDecimal
    private BigDecimal montant;

    // Many cheques can come from one bank. This is a mandatory (not nullable) relationship.
    @ManyToOne(optional = false) // 'optional = false' makes the foreign key non-nullable
    @JoinColumn(name = "banque_id", nullable = false) // Defines the foreign key column in the CHEQUES table
    private Banque banque;

    @Column(name = "active", nullable = false)
    private boolean active;

    // --- Constructors ---
    public Cheque() {
        // Default constructor required by JPA
    }

    public Cheque(String numero, OffsetDateTime dateEmission, String beneficiaire, OffsetDateTime dateEcheance, BigDecimal montant, Banque banque) {
        this.numero = numero;
        this.dateEmission = dateEmission;
        this.beneficiaire = beneficiaire;
        this.dateEcheance = dateEcheance;
        this.montant = montant;
        this.banque = banque;
    }

    // --- Getters and Setters ---
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

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public OffsetDateTime getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(OffsetDateTime dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public Banque getBanque() {
        return banque;
    }

    public void setBanque(Banque banque) {
        this.banque = banque;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    // --- Utility Methods (Recommended for Entities) ---
    @Override
    public String toString() {
        return "Cheque{" +
                "numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", dateEcheance=" + dateEcheance +
                ", montant=" + montant +
                ", banque=" + (banque != null ? banque.getNom() : "null") +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cheque cheque = (Cheque) o;
        return numero != null && Objects.equals(numero, cheque.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }
}