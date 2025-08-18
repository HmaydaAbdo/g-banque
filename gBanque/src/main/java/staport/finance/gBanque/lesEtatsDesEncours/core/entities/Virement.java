package staport.finance.gBanque.lesEtatsDesEncours.core.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "VIREMENTS")
public class Virement {

    @Id
    @Column(name = "numero", nullable = false, unique = true)
    private String numero;

    @Column(name = "date_emission", nullable = false)
    private OffsetDateTime dateEmission;

    @Column(name = "beneficiaire", nullable = false)
    private String beneficiaire;

    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "banque_emettrice_id", nullable = false)
    private Banque banque;

    private String banqueBeneficiaire;

    @Column(name = "rib_beneficiaire")
    private String ribBeneficiaire;

    public Virement() {}

    public Virement(String numero, OffsetDateTime dateEmission, String beneficiaire,
                    BigDecimal montant, Banque banque, String banqueBeneficiaire, String ribBeneficiaire) {
        this.numero = numero;
        this.dateEmission = dateEmission;
        this.beneficiaire = beneficiaire;
        this.montant = montant;
        this.banque = banque;
        this.banqueBeneficiaire = banqueBeneficiaire;
        this.ribBeneficiaire = ribBeneficiaire;
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
    public String getBanqueBeneficiaire() { return banqueBeneficiaire; }
    public void setBanqueBeneficiaire(String banqueBeneficiaire) { this.banqueBeneficiaire = banqueBeneficiaire; }
    public String getRibBeneficiaire() { return ribBeneficiaire; }
    public void setRibBeneficiaire(String ribBeneficiaire) { this.ribBeneficiaire = ribBeneficiaire; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Virement)) return false;
        Virement virement = (Virement) o;
        return numero != null && Objects.equals(numero, virement.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return "Virement{" +
                "numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", montant=" + montant +
                ", banque=" + banque +
                ", banqueBeneficiaire=" + banqueBeneficiaire +
                ", ribBeneficiaire='" + ribBeneficiaire + '\'' +
                '}';
    }
}
