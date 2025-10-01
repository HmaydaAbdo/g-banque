package staport.finance.gBanque.miseADisposition.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "MISES_A_DISPOSITION")
public class MiseADisposition {

    @Id
    @Column(name = "numero", nullable = false, unique = true)
    private String numero;

    @Column(name = "date_emission", nullable = false)
    private OffsetDateTime dateEmission;

    private OffsetDateTime dateOperation;
    private OffsetDateTime dateValeur;

    @Column(name = "beneficiaire", nullable = false)
    private String beneficiaire;

    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Column(name = "cin_beneficiaire")
    private String cinBeneficiaire;

    @Column(name = "active", nullable = false)
    private boolean active;

    public MiseADisposition() {}

    public MiseADisposition(String numero, OffsetDateTime dateEmission, String beneficiaire,
                            BigDecimal montant, Banque banque, String cinBeneficiaire) {
        this.numero = numero;
        this.dateEmission = dateEmission;
        this.beneficiaire = beneficiaire;
        this.montant = montant;
        this.banque = banque;
        this.cinBeneficiaire = cinBeneficiaire;
    }

    // Getters & Setters
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public OffsetDateTime getDateEmission() { return dateEmission; }
    public void setDateEmission(OffsetDateTime dateEmission) { this.dateEmission = dateEmission; }
    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }
    public String getCinBeneficiaire() { return cinBeneficiaire; }
    public void setCinBeneficiaire(String cinBeneficiaire) { this.cinBeneficiaire = cinBeneficiaire; }

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MiseADisposition)) return false;
        MiseADisposition mad = (MiseADisposition) o;
        return numero != null && Objects.equals(numero, mad.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return "MiseADisposition{" +
                "numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", montant=" + montant +
                ", banque=" + banque +
                ", cinBeneficiaire='" + cinBeneficiaire + '\'' +
                ", active=" + active +
                '}';
    }
}
