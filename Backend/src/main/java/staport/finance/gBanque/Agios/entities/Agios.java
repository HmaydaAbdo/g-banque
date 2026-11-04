package staport.finance.gBanque.Agios.entities;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.Agios.enums.TypeAgios;

import java.math.BigDecimal;
import java.time.LocalDate;

@Audited
@Entity
@Table(
        name = "agios",
        indexes = {
                @Index(name = "idx_agios_banque_id",    columnList = "banque_id"),
                @Index(name = "idx_agios_status",       columnList = "status"),
                @Index(name = "idx_agios_date_emission",columnList = "date_emission")
        }
)
public class Agios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_agios", length = 50)
    private TypeAgios typeAgios;

    /** Periodicité de la ligne calculée : TRIMESTRIEL ou MENSUEL (factoring). */
    @Column(name = "periodicite", length = 20)
    private String periodicite;

    @Column(name = "periode_numero")
    private Integer periodeNumero;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "montant_facture", precision = 19, scale = 4)
    private BigDecimal montantFacture;

    @Column(name = "montant_calcule", precision = 19, scale = 4)
    private BigDecimal montantCalcule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Column(name = "date_emission")
    private LocalDate dateEmission;

    @Column(name = "date_valeur")
    private LocalDate dateValeur;

    @Column(name = "date_operation")
    private LocalDate dateOperation;

    @Column(length = 150, nullable = false)
    private String libelle;

    @Column(length = 500, nullable = false)
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.PREVU;

    public Agios() {}

    public Long getId() {
        return id;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public TypeAgios getTypeAgios() { return typeAgios; }
    public void setTypeAgios(TypeAgios typeAgios) { this.typeAgios = typeAgios; }
    public String getPeriodicite() { return periodicite; }
    public void setPeriodicite(String periodicite) { this.periodicite = periodicite; }
    public Integer getPeriodeNumero() { return periodeNumero; }
    public void setPeriodeNumero(Integer periodeNumero) { this.periodeNumero = periodeNumero; }
    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }
    public BigDecimal getMontantFacture() { return montantFacture; }
    public void setMontantFacture(BigDecimal montantFacture) { this.montantFacture = montantFacture; }
    public BigDecimal getMontantCalcule() { return montantCalcule; }
    public void setMontantCalcule(BigDecimal montantCalcule) { this.montantCalcule = montantCalcule; }

    public Banque getBanque() {
        return banque;
    }

    public void setBanque(Banque banque) {
        this.banque = banque;
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
}
