package staport.finance.gBanque.moyensDePaiements.virementInternational.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

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
    private LocalDate dateEmission ;

    @Column(name = "date_operation")
    private LocalDate dateOperation ;

    @Column(name = "date_valeur")
    private LocalDate dateValeur ;

    @Column(name = "libelle")
    private String libelle;

    @Column(name = "observation")
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutMoyenPaiement statut;

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

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
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

    public StatutMoyenPaiement getStatut() { return statut; }

    public void setStatut(StatutMoyenPaiement statut) { this.statut = statut; }

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
        return Objects.equals(id, that.id) &&
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
                dateOperation, dateValeur, libelle, observation, type);
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
                ", statut=" + statut +
                ", type=" + type +
                '}';
    }
}