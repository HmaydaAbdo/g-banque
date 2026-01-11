package staport.finance.gBanque.decomptes.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "decompte")
public class Decompte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marche_id")
    private Marche marche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_encaissement_id",nullable = false)
    private Banque banqueEncaissement;

    @Column(name = "date_emission")
    private OffsetDateTime dateEmission;

    @Column(name = "montant", precision = 19, scale = 2)
    private BigDecimal montant;

    private String observation;
    private String libelle;

    @Column(name = "date_operation")
    private OffsetDateTime dateOperation;

    @Column(name = "date_valeur")
    private OffsetDateTime dateValeur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PREVU;

    // Constructors
    public Decompte() {
    }

    public Decompte(Marche marche , OffsetDateTime dateEmission, BigDecimal montant,
                    String observation, OffsetDateTime dateOperation, OffsetDateTime dateValeur, Status status) {
        this.marche = marche;

        this.dateEmission = dateEmission;
        this.montant = montant;
        this.observation = observation;
        this.dateOperation = dateOperation;
        this.dateValeur = dateValeur;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Marche getMarche() {
        return marche;
    }

    public void setMarche(Marche marche) {
        this.marche = marche;
    }


    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Banque getBanqueEncaissement() {
        return banqueEncaissement;
    }

    public void setBanqueEncaissement(Banque banqueEncaissement) {
        this.banqueEncaissement = banqueEncaissement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Decompte)) return false;
        Decompte decompte = (Decompte) o;
        return Objects.equals(id, decompte.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Decompte{" +
                "id=" + id +
                ", marche=" + (marche != null ? marche.getId() : null) +
                ", banqueEncaissement=" + (banqueEncaissement != null ? banqueEncaissement.getId() : null) +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", observation='" + observation + '\'' +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                ", status=" + status +
                '}';
    }
}
