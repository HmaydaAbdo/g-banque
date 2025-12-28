package staport.finance.gBanque.droitConstate.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class DroitConstate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "marche_id")
    private Marche marche;

    private OffsetDateTime dateEmission;

    private BigDecimal montant;

    private BigDecimal decompteInitial;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String observation;
    private String libelle;

    @Enumerated(EnumType.STRING)
    private Status status=Status.PREVU;

    private OffsetDateTime dateOperation;
    private OffsetDateTime dateValeur;

    public DroitConstate() {
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

    public BigDecimal getDecompteInitial() {
        return decompteInitial;
    }

    public void setDecompteInitial(BigDecimal decompteInitial) {
        this.decompteInitial = decompteInitial;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DroitConstate that = (DroitConstate) o;
        return Objects.equals(id, that.id) && Objects.equals(marche, that.marche) && Objects.equals(dateEmission, that.dateEmission) && Objects.equals(montant, that.montant) && type == that.type && Objects.equals(observation, that.observation) && status == that.status && Objects.equals(dateOperation, that.dateOperation) && Objects.equals(dateValeur, that.dateValeur);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, marche, dateEmission, montant, type, observation, status, dateOperation, dateValeur);
    }

    @Override
    public String toString() {
        return "DroitConstate{" +
                "id=" + id +
                ", marche=" + marche +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", decompteInitial=" + decompteInitial +
                ", type=" + type +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                ", status=" + status +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                '}';
    }
}
