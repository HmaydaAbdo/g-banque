package staport.finance.gBanque.moyensDePaiements.lcn.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.NatureLcn;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "LCNS")
public class Lcn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "beneficiaire", nullable = false)
    private String beneficiaire;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance ;

    private LocalDate dateOperation ;
    private LocalDate dateValeur ;

    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutMoyenPaiement statut;

    private String libelle;
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature_lcn")
    private NatureLcn natureLcn;

    private String emetteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_fonctionnelle_id", nullable = true)
    private LigneFonctionnelle ligneFonctionnelle;


    @PrePersist
    @PreUpdate
    private void validateBusinessRules() {
        if (this.natureLcn == NatureLcn.ESCOMPTE && this.type != Type.ENCAISSEMENT) {
            throw new IllegalStateException(
                    "Violation de règle métier: Un LCN de nature ESCOMPTE doit obligatoirement être de type ENCAISSEMENT. " +
                            "Type actuel: " + this.type + ", Nature: " + this.natureLcn
            );
        }

    }
    public Lcn() {}



    // Getters & Setters
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getIdPaiement() { return idPaiement; }
    public void setIdPaiement(String idPaiement) { this.idPaiement = idPaiement; }
    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }
    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }
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
    public LigneFonctionnelle getLigneFonctionnelle() {
        return ligneFonctionnelle;
    }
    public void setLigneFonctionnelle(LigneFonctionnelle ligneFonctionnelle) {
        this.ligneFonctionnelle = ligneFonctionnelle;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Lcn lcn = (Lcn) o;
        return Objects.equals(id, lcn.id) && Objects.equals(numero, lcn.numero) && Objects.equals(dateEmission, lcn.dateEmission) && Objects.equals(beneficiaire, lcn.beneficiaire) && Objects.equals(dateEcheance, lcn.dateEcheance) && Objects.equals(dateOperation, lcn.dateOperation) && Objects.equals(dateValeur, lcn.dateValeur) && Objects.equals(montant, lcn.montant) && Objects.equals(banque, lcn.banque) && Objects.equals(libelle, lcn.libelle) && Objects.equals(observation, lcn.observation) && type == lcn.type && natureLcn == lcn.natureLcn && Objects.equals(emetteur, lcn.emetteur);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numero, dateEmission, beneficiaire, dateEcheance, dateOperation, dateValeur, montant, banque, libelle, observation, type, natureLcn, emetteur);
    }

    @Override
    public String toString() {
        return "Lcn{" +
                "id=" + id +
                ", idPaiement='" + idPaiement + '\'' +
                ", numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", dateEcheance=" + dateEcheance +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                ", montant=" + montant +
                ", banque=" + banque +
                ", ligneFonctionnelle=" + (ligneFonctionnelle != null ? ligneFonctionnelle.getId() : null) +
                ", statut=" + statut +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", type=" + type +
                ", natureLcn=" + natureLcn +
                ", emetteur='" + emetteur + '\'' +
                '}';
    }
}

