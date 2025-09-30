package staport.finance.gBanque.cmt.echeances.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entité représentant une échéance individuelle d'un crédit à moyen terme.
 */
@Entity
@Table(name = "echeance_cmt")
public class EcheanceCMT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Le crédit moyen terme est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_moyen_terme_id", nullable = false)
    private CreditMoyenTerme creditMoyenTerme;

    @NotNull(message = "Le numéro d'échéance est obligatoire")
    @Column(name = "numero_echeance", nullable = false)
    private Integer numeroEcheance;

    @NotNull(message = "La date d'échéance est obligatoire")
    @Column(name = "date_echeance", nullable = false)
    private OffsetDateTime dateEcheance;

    private OffsetDateTime dateOperation;
    private  OffsetDateTime dateValeur;

    @NotNull(message = "Le montant de l'échéance est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant de l'échéance doit être supérieur à zéro")
    @Column(name = "montant", nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @NotNull(message = "Le statut de paiement est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_paiement", nullable = false)
    private StatutEcheanceCMT statutPaiement;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;


    // Constructeurs
    public EcheanceCMT() {}
    public EcheanceCMT(CreditMoyenTerme creditMoyenTerme, Integer numeroEcheance, OffsetDateTime dateEcheance, BigDecimal montant, StatutEcheanceCMT statutPaiement) {
        this.creditMoyenTerme = creditMoyenTerme;
        this.numeroEcheance = numeroEcheance;
        this.dateEcheance = dateEcheance;
        this.montant = montant;
        this.statutPaiement = statutPaiement;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CreditMoyenTerme getCreditMoyenTerme() { return creditMoyenTerme; }
    public void setCreditMoyenTerme(CreditMoyenTerme creditMoyenTerme) { this.creditMoyenTerme = creditMoyenTerme; }
    public Integer getNumeroEcheance() { return numeroEcheance; }
    public void setNumeroEcheance(Integer numeroEcheance) { this.numeroEcheance = numeroEcheance; }
    public OffsetDateTime getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(OffsetDateTime dateEcheance) { this.dateEcheance = dateEcheance; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public StatutEcheanceCMT getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(StatutEcheanceCMT statutPaiement) { this.statutPaiement = statutPaiement; }
    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }
    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }

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

    // Callbacks JPA
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.dateCreation = now;
        this.dateModification = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = OffsetDateTime.now();
    }

    // equals() et hashCode()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EcheanceCMT that = (EcheanceCMT) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}