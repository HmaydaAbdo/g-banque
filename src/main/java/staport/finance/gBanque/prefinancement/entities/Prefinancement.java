package staport.finance.gBanque.prefinancement.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class Prefinancement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "marche_id")
    private Marche marche;

    private OffsetDateTime dateEmission;

    private BigDecimal montant;

    @Enumerated(EnumType.STRING) // Best practice for enums
    private Type type;

    private String observation;
    private String libelle;

    @Enumerated(EnumType.STRING) // Best practice for enums
    private Status status=Status.PREVU;

    private OffsetDateTime dateOperation;
    private OffsetDateTime dateValeur;

    // Default no-argument constructor is required by JPA
    public Prefinancement() {
    }

    // You can add a parameterized constructor for convenience
    public Prefinancement(Marche marche, OffsetDateTime dateEmission, BigDecimal montant, String observation, Type type, OffsetDateTime dateOperation, OffsetDateTime dateValeur, Status status) {
        this.marche = marche;
        this.dateEmission = dateEmission;
        this.montant = montant;
        this.observation = observation;
        this.type = type;
        this.dateOperation = dateOperation;
        this.dateValeur = dateValeur;
        this.status = status;
    }

    // Getters and Setters for all fields
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
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

    // toString(), equals(), and hashCode() are essential for good practice
    @Override
    public String toString() {
        return "Prefinancement{" +
                "id=" + id +
                ", marche=" + (marche != null ? marche.getId() : "null") +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", observation='" + observation + '\'' +
                ", type=" + type +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prefinancement that = (Prefinancement) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}