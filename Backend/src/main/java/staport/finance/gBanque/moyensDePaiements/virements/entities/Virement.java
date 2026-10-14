package staport.finance.gBanque.moyensDePaiements.virements.entities;

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
@Table(name = "VIREMENTS", indexes = {
        @Index(name = "idx_virement_numero", columnList = "numero"),
        @Index(name = "idx_virement_banque_emettrice_id", columnList = "banque_emettrice_id"),
        @Index(name = "idx_virement_banque_beneficiare_id", columnList = "banque_beneficiare_id"),
        @Index(name = "idx_virement_statut", columnList = "statut"),
        @Index(name = "idx_virement_date_emission", columnList = "date_emission"),
        @Index(name = "idx_virement_type", columnList = "type")
})
public class Virement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero")
    private String numero;

    /**
     * Identifiant technique unique côté FunPaiement — clé de matching exclusive de la synchro
     * (remplace le matching par numéro, non fiable — cf. AbstractPaymentProcessor). Pour un
     * virement interne (bénéficiaire STAPORT, dédoublé en deux entrées), suffixé "-EN"/"-DE"
     * comme le numéro, afin de rester unique par ligne.
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

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_emettrice_id", nullable = true)
    private Banque banqueEmettrice;
    private String emetteur;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_beneficiare_id",  nullable =true)
    private Banque banqueBeneficiare;

    private String nomBanqueBeneficiaireLibre;

    @Column(name = "rib_beneficiaire")
    private String ribBeneficiaire;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutMoyenPaiement statut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String observation;
    @Column(nullable = false)
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
    public String getIdPaiement() { return idPaiement; }
    public void setIdPaiement(String idPaiement) { this.idPaiement = idPaiement; }
    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }
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

    public StatutMoyenPaiement getStatut() {
        return statut;
    }
    public void setStatut(StatutMoyenPaiement statut) {
        this.statut = statut;
    }

    public Type getType() {return type;}
    public void setType(Type type) { this.type = type;}

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Virement virement = (Virement) o;
        return Objects.equals(id, virement.id) && Objects.equals(numero, virement.numero) && Objects.equals(dateEmission, virement.dateEmission) && Objects.equals(dateOperation, virement.dateOperation) && Objects.equals(dateValeur, virement.dateValeur) && Objects.equals(beneficiaire, virement.beneficiaire) && Objects.equals(montant, virement.montant) && Objects.equals(banqueEmettrice, virement.banqueEmettrice) && Objects.equals(emetteur, virement.emetteur) && Objects.equals(banqueBeneficiare, virement.banqueBeneficiare) && Objects.equals(nomBanqueBeneficiaireLibre, virement.nomBanqueBeneficiaireLibre) && Objects.equals(ribBeneficiaire, virement.ribBeneficiaire) && type == virement.type && Objects.equals(observation, virement.observation) && Objects.equals(libelle, virement.libelle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numero, dateEmission, dateOperation, dateValeur, beneficiaire, montant, banqueEmettrice, emetteur, banqueBeneficiare, nomBanqueBeneficiaireLibre, ribBeneficiaire, type, observation, libelle);
    }

    @Override
    public String toString() {
        return "Virement{" +
                "id=" + id +
                ", idPaiement='" + idPaiement + '\'' +
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
                ", statut=" + statut +
                ", type=" + type +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                '}';
    }
}
