package staport.finance.gBanque.moyensDePaiements.virementInternational.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "VIREMENTS_INTERNATIONAUX")
public class VirementInternational {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Column(name = "beneficiaire")
    private String beneficiaire;

    @Column(name = "emetteur")
    private String emetteur;

    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_emission", nullable = false)
    private OffsetDateTime dateEmission;

    @Column(name = "date_operation")
    private OffsetDateTime dateOperation;

    @Column(name = "date_valeur")
    private OffsetDateTime dateValeur;

    @Column(name = "libelle")
    private String libelle;

    @Column(name = "observation")
    private String observation;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "type",nullable = false)
    private Type type;

    public VirementInternational() {
    }

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Banque getBanque() {
        return banque;
    }

    public void setBanque(Banque banque) {
        this.banque = banque;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VirementInternational that = (VirementInternational) o;
        return active == that.active &&
                Objects.equals(id, that.id) &&
                Objects.equals(banque, that.banque) &&
                Objects.equals(beneficiaire, that.beneficiaire) &&
                Objects.equals(emetteur, that.emetteur) &&
                Objects.equals(montant, that.montant) &&
                Objects.equals(dateEmission, that.dateEmission) &&
                Objects.equals(dateOperation, that.dateOperation) &&
                Objects.equals(dateValeur, that.dateValeur) &&
                Objects.equals(libelle, that.libelle) &&
                Objects.equals(observation, that.observation) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, banque, beneficiaire, emetteur, montant, dateEmission,
                dateOperation, dateValeur, libelle, observation, active, type);
    }

    @Override
    public String toString() {
        return "VirementInternational{" +
                "id=" + id +
                ", banque=" + banque +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", emetteur='" + emetteur + '\'' +
                ", montant=" + montant +
                ", dateEmission=" + dateEmission +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", active=" + active +
                ", type=" + type +
                '}';
    }
}