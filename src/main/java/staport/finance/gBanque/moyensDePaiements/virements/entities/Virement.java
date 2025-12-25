package staport.finance.gBanque.moyensDePaiements.virements.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "VIREMENTS")
public class Virement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero")
    private String numero;

    @Column(name = "date_emission", nullable = false)
    private OffsetDateTime dateEmission;

    private OffsetDateTime dateOperation;
    private OffsetDateTime dateValeur;

    @Column(name = "beneficiaire", nullable = false)
    private String beneficiaire;

    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @ManyToOne(optional = true)
    @JoinColumn(name = "banque_emettrice_id", nullable = true)
    private Banque banqueEmettrice;
    private String emetteur;

    @ManyToOne(optional = true)
    @JoinColumn(name = "banque_beneficiare_id",  nullable =true)
    private Banque banqueBeneficiare;

    private String nomBanqueBeneficiaireLibre;

    @Column(name = "rib_beneficiaire")
    private String ribBeneficiaire;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String observation;
    private String libelle;

    public Virement() {}


    // Getters & Setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public OffsetDateTime getDateEmission() { return dateEmission; }
    public void setDateEmission(OffsetDateTime dateEmission) { this.dateEmission = dateEmission; }
    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public String getRibBeneficiaire() { return ribBeneficiaire; }
    public void setRibBeneficiaire(String ribBeneficiaire) { this.ribBeneficiaire = ribBeneficiaire; }

    public Banque getBanqueEmettrice() {
        return banqueEmettrice;
    }

    public void setBanqueEmettrice(Banque banqueEmettrice) {
        this.banqueEmettrice = banqueEmettrice;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public Banque getBanqueBeneficiare() {
        return banqueBeneficiare;
    }

    public void setBanqueBeneficiare(Banque banqueBeneficiare) {
        this.banqueBeneficiare = banqueBeneficiare;
    }

    public String getNomBanqueBeneficiaireLibre() {
        return nomBanqueBeneficiaireLibre;
    }

    public void setNomBanqueBeneficiaireLibre(String nomBanqueBeneficiaireLibre) {
        this.nomBanqueBeneficiaireLibre = nomBanqueBeneficiaireLibre;
    }

    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    public Type getType() {return type;}
    public void setType(Type type) { this.type = type;}

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

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Virement virement = (Virement) o;
        return active == virement.active && Objects.equals(id, virement.id) && Objects.equals(numero, virement.numero) && Objects.equals(dateEmission, virement.dateEmission) && Objects.equals(dateOperation, virement.dateOperation) && Objects.equals(dateValeur, virement.dateValeur) && Objects.equals(beneficiaire, virement.beneficiaire) && Objects.equals(montant, virement.montant) && Objects.equals(banqueEmettrice, virement.banqueEmettrice) && Objects.equals(emetteur, virement.emetteur) && Objects.equals(banqueBeneficiare, virement.banqueBeneficiare) && Objects.equals(nomBanqueBeneficiaireLibre, virement.nomBanqueBeneficiaireLibre) && Objects.equals(ribBeneficiaire, virement.ribBeneficiaire) && type == virement.type && Objects.equals(observation, virement.observation) && Objects.equals(libelle, virement.libelle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numero, dateEmission, dateOperation, dateValeur, beneficiaire, montant, banqueEmettrice, emetteur, banqueBeneficiare, nomBanqueBeneficiaireLibre, ribBeneficiaire, active, type, observation, libelle);
    }

    @Override
    public String toString() {
        return "Virement{" +
                "id=" + id +
                ", numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", montant=" + montant +
                ", banqueEmettrice=" + banqueEmettrice +
                ", emetteur='" + emetteur + '\'' +
                ", banqueBeneficiare=" + banqueBeneficiare +
                ", nomBanqueBeneficiaireLibre='" + nomBanqueBeneficiaireLibre + '\'' +
                ", ribBeneficiaire='" + ribBeneficiaire + '\'' +
                ", active=" + active +
                ", type=" + type +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                '}';
    }
}
