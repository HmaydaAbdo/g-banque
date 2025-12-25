package staport.finance.gBanque.moyensDePaiements.lcn.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.NatureLcn;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "LCNS")
public class Lcn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero")
    private String numero;

    @Column(name = "date_emission", nullable = false)
    private OffsetDateTime dateEmission;

    @Column(name = "beneficiaire", nullable = false)
    private String beneficiaire;

    @Column(name = "date_echeance", nullable = false)
    private OffsetDateTime dateEcheance;

    private OffsetDateTime dateOperation;
    private OffsetDateTime dateValeur;

    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Column(name = "active", nullable = false)
    private boolean active;

    private String libelle;
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature_lcn")
    private NatureLcn natureLcn;

    private String emetteur;

    public Lcn() {}



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
    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }
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
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Type getType() {
        return type;
    }
    public void setType(Type type) {
        this.type = type;
    }
    public String getEmetteur() {
        return emetteur;
    }
    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }
    public NatureLcn getNatureLcn() {
        return natureLcn;
    }
    public void setNatureLcn(NatureLcn natureLcn) {
        this.natureLcn = natureLcn;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Lcn lcn = (Lcn) o;
        return active == lcn.active && Objects.equals(id, lcn.id) && Objects.equals(numero, lcn.numero) && Objects.equals(dateEmission, lcn.dateEmission) && Objects.equals(beneficiaire, lcn.beneficiaire) && Objects.equals(dateEcheance, lcn.dateEcheance) && Objects.equals(dateOperation, lcn.dateOperation) && Objects.equals(dateValeur, lcn.dateValeur) && Objects.equals(montant, lcn.montant) && Objects.equals(banque, lcn.banque) && Objects.equals(libelle, lcn.libelle) && Objects.equals(observation, lcn.observation) && type == lcn.type && natureLcn == lcn.natureLcn && Objects.equals(emetteur, lcn.emetteur);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numero, dateEmission, beneficiaire, dateEcheance, dateOperation, dateValeur, montant, banque, active, libelle, observation, type, natureLcn, emetteur);
    }

    @Override
    public String toString() {
        return "Lcn{" +
                "id=" + id +
                ", numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", dateEcheance=" + dateEcheance +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                ", montant=" + montant +
                ", banque=" + banque +
                ", active=" + active +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", type=" + type +
                ", natureLcn=" + natureLcn +
                ", emetteur='" + emetteur + '\'' +
                '}';
    }
}

