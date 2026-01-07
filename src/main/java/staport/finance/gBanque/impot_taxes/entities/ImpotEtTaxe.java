package staport.finance.gBanque.impot_taxes.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class ImpotEtTaxe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Column
    private OffsetDateTime dateEmission;

    @Column
    private OffsetDateTime dateValeur;

    @Column
    private OffsetDateTime dateOperation;

    @Column(length = 500)
    private String libelle;

    @Column(length = 500)
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 50)
    private Motif motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status=Status.PREVU;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // JPA callbacks for audit fields
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

    public ImpotEtTaxe() {
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public OffsetDateTime getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(OffsetDateTime dateValeur) {
        this.dateValeur = dateValeur;
    }

    public OffsetDateTime getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(OffsetDateTime dateOperation) {
        this.dateOperation = dateOperation;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Motif getMotif() {
        return motif;
    }

    public void setMotif(Motif motif) {
        this.motif = motif;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public OffsetDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(OffsetDateTime dateModification) {
        this.dateModification = dateModification;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImpotEtTaxe that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(montant, that.montant) && Objects.equals(banque, that.banque) && Objects.equals(dateEmission, that.dateEmission) && Objects.equals(dateValeur, that.dateValeur) && Objects.equals(dateOperation, that.dateOperation) && Objects.equals(libelle, that.libelle) && Objects.equals(observation, that.observation) && motif == that.motif && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, montant, banque, dateEmission, dateValeur, dateOperation, libelle, observation, motif, status);
    }

    @Override
    public String toString() {
        return "ImpotEtTaxe{" +
                "id=" + id +
                ", montant=" + montant +
                ", banque=" + banque +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", motif=" + motif +
                ", status=" + status +
                '}';
    }
}

