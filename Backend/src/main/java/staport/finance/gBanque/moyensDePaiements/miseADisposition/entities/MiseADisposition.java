package staport.finance.gBanque.moyensDePaiements.miseADisposition.entities;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Audited
@Table(name = "MISES_A_DISPOSITION", indexes = {
        @Index(name = "idx_mise_a_dispo_numero", columnList = "numero"),
        @Index(name = "idx_mise_a_dispo_banque_id", columnList = "banque_id"),
        @Index(name = "idx_mise_a_dispo_statut", columnList = "statut"),
        @Index(name = "idx_mise_a_dispo_date_emission", columnList = "date_emission"),
        @Index(name = "idx_mise_a_dispo_type", columnList = "type")
})
public class MiseADisposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "numero")
    private String numero;

    /**
     * Identifiant technique unique côté FunPaiement — clé de matching exclusive de la synchro
     * (remplace le matching par numéro, non fiable — cf. AbstractPaymentProcessor).
     */
    @Column(name = "id_paiement", unique = true, length = 50)
    private String idPaiement;

    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission ;

    private LocalDate dateOperation ;

    private LocalDate dateValeur ;

    @Column(name = "beneficiaire", nullable = false)
    private String beneficiaire;

    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Column(name = "cin_beneficiaire")
    private String cinBeneficiaire;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutMoyenPaiement statut;

    @Column(nullable = false)
    private String observation;

    @Column(nullable = false)
    private String libelle;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String emetteur;

    public MiseADisposition() {}


    // Getters & Setters
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getIdPaiement() { return idPaiement; }
    public void setIdPaiement(String idPaiement) { this.idPaiement = idPaiement; }
    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }
    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }
    public String getCinBeneficiaire() { return cinBeneficiaire; }
    public void setCinBeneficiaire(String cinBeneficiaire) { this.cinBeneficiaire = cinBeneficiaire; }

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

    public StatutMoyenPaiement getStatut() {
        return statut;
    }

    public void setStatut(StatutMoyenPaiement statut) {
        this.statut = statut;
    }

    public Long getId() {
        return id;
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
        MiseADisposition that = (MiseADisposition) o;
        return Objects.equals(id, that.id) && Objects.equals(numero, that.numero) && Objects.equals(dateEmission, that.dateEmission) && Objects.equals(dateOperation, that.dateOperation) && Objects.equals(dateValeur, that.dateValeur) && Objects.equals(beneficiaire, that.beneficiaire) && Objects.equals(montant, that.montant) && Objects.equals(banque, that.banque) && Objects.equals(cinBeneficiaire, that.cinBeneficiaire) && Objects.equals(observation, that.observation) && Objects.equals(libelle, that.libelle) && type == that.type && Objects.equals(emetteur, that.emetteur);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numero, dateEmission, dateOperation, dateValeur, beneficiaire, montant, banque, cinBeneficiaire, observation, libelle, type, emetteur);
    }

    @Override
    public String toString() {
        return "MiseADisposition{" +
                "id=" + id +
                ", idPaiement='" + idPaiement + '\'' +
                ", numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", montant=" + montant +
                ", banque=" + banque +
                ", cinBeneficiaire='" + cinBeneficiaire + '\'' +
                ", statut=" + statut +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                ", type=" + type +
                ", emetteur='" + emetteur + '\'' +
                '}';
    }
}
