package staport.finance.gBanque.factoring.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
public class Factoring {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "marche_id")
    private Marche marche;

    private LocalDate dateEmission ;

    private BigDecimal montant;

    @Enumerated(EnumType.STRING) // Best practice for enums
    private Type type;

    private String observation;
    private String libelle;

    @Enumerated(EnumType.STRING) // Best practice for enums
    private Status status=Status.PREVU;

    private LocalDate dateOperation ;
    private LocalDate dateValeur ;

    public Factoring() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Marche getMarche() {
        return marche;
    }

    public void setMarche(Marche marche) {
        this.marche = marche;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Factoring factoring = (Factoring) o;
        return Objects.equals(id, factoring.id) && Objects.equals(marche, factoring.marche) && Objects.equals(dateEmission, factoring.dateEmission) && Objects.equals(montant, factoring.montant) && type == factoring.type && Objects.equals(observation, factoring.observation) && Objects.equals(libelle, factoring.libelle) && status == factoring.status && Objects.equals(dateOperation, factoring.dateOperation) && Objects.equals(dateValeur, factoring.dateValeur);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, marche, dateEmission, montant, type, observation, libelle, status, dateOperation, dateValeur);
    }


    @Override
    public String toString() {
        return "Factoring{" +
                "id=" + id +
                ", marche=" + marche +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", type=" + type +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                ", status=" + status +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                '}';
    }
}
