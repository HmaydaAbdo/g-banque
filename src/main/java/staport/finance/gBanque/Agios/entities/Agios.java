package staport.finance.gBanque.Agios.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
public class Agios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;


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

    public Agios() {
    }

    public Long getId() {
        return id;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Agios agios = (Agios) o;
        return Objects.equals(id, agios.id) && Objects.equals(montant, agios.montant) && Objects.equals(banque, agios.banque)  && Objects.equals(dateEmission, agios.dateEmission) && Objects.equals(dateValeur, agios.dateValeur) && Objects.equals(dateOperation, agios.dateOperation) && Objects.equals(libelle, agios.libelle) && Objects.equals(observation, agios.observation) && status == agios.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, montant, banque, dateEmission, dateValeur, dateOperation, libelle, observation, status);
    }


    @Override
    public String toString() {
        return "Agios{" +
                "id=" + id +
                ", montant=" + montant +
                ", banque=" + banque +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", status=" + status +
                '}';
    }
}
