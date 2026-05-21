package staport.finance.gBanque.moyensDePaiements.cheques.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "CHEQUES")
public class Cheque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = true)
    private String numero;

    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission ;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance ;

    @Column(name = "beneficiaire", nullable = false)
    private String beneficiaire;

    @Column(name = "montant", nullable = false, precision = 19, scale = 2) // Precision and scale for BigDecimal
    private BigDecimal montant;

    private LocalDate dateOperation ;
    private LocalDate dateValeur ;



    // Many cheques can come from one bank. This is a mandatory (not nullable) relationship.
    @ManyToOne(optional = false) // 'optional = false' makes the foreign key non-nullable
    @JoinColumn(name = "banque_id", nullable = false) // Defines the foreign key column in the CHEQUES table
    private Banque banque;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutMoyenPaiement statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Type type;

    private String libelle;
    private String observation;

    private String emetteur;

    // --- Constructors ---
    public Cheque() {
        // Default constructor required by JPA
    }



    // --- Getters and Setters ---


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

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
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

    public StatutMoyenPaiement getStatut() {
        return statut;
    }

    public void setStatut(StatutMoyenPaiement statut) {
        this.statut = statut;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Cheque cheque = (Cheque) o;
        return Objects.equals(id, cheque.id) && Objects.equals(numero, cheque.numero) && Objects.equals(dateEmission, cheque.dateEmission) && Objects.equals(beneficiaire, cheque.beneficiaire) && Objects.equals(dateEcheance, cheque.dateEcheance) && Objects.equals(dateOperation, cheque.dateOperation) && Objects.equals(dateValeur, cheque.dateValeur) && Objects.equals(montant, cheque.montant) && Objects.equals(banque, cheque.banque) && type == cheque.type && Objects.equals(libelle, cheque.libelle) && Objects.equals(observation, cheque.observation) && Objects.equals(emetteur, cheque.emetteur);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numero, dateEmission, beneficiaire, dateEcheance, dateOperation, dateValeur, montant, banque, type, libelle, observation, emetteur);
    }

    @Override
    public String toString() {
        return "Cheque{" +
                "id=" + id +
                ", numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", dateEcheance=" + dateEcheance +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                ", montant=" + montant +
                ", banque=" + banque +
                ", statut=" + statut +
                ", type=" + type +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", emetteur='" + emetteur + '\'' +
                '}';
    }
}