package staport.finance.gBanque.commision.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.cautions.entities.Caution;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "commission")
public class Commission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionSource operation;

    private LocalDate dateEmission ;
    private LocalDate dateValeur ;
    private LocalDate dateOperation ;

    @Column( length = 150)
    private String libelle;
    @Column(length = 500)
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status=Status.PREVU;

    /**
     * Caution à l'origine de cette commission — nullable.
     * Renseigné uniquement pour les commissions générées automatiquement depuis une caution.
     * Les commissions saisies manuellement laissent ce champ null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caution_id")
    private Caution caution;

    /**
     * Impôt/taxe à l'origine de cette commission — nullable.
     * Renseigné uniquement pour les commissions générées automatiquement à la création
     * d'un impôt/taxe SAHAM BANK (5,50 DH, quel que soit le motif).
     * Les commissions saisies manuellement ou générées depuis une caution laissent ce champ null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impot_et_taxe_id", unique = true)
    private ImpotEtTaxe impotEtTaxe;

    public Commission() {}


    public Long getId() {
        return id;
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

    public TransactionSource getOperation() {
        return operation;
    }

    public void setOperation(TransactionSource operation) {
        this.operation = operation;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }



    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Caution getCaution() {
        return caution;
    }

    public void setCaution(Caution caution) {
        this.caution = caution;
    }

    public ImpotEtTaxe getImpotEtTaxe() {
        return impotEtTaxe;
    }

    public void setImpotEtTaxe(ImpotEtTaxe impotEtTaxe) {
        this.impotEtTaxe = impotEtTaxe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Commission)) return false;
        Commission that = (Commission) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    @Override
    public String toString() {
        return "Commission{" +
                "id=" + id +
                ", libelle='" + libelle + '\'' +
                ", montant=" + montant +
                ", operation=" + operation +
                '}';
    }
}
