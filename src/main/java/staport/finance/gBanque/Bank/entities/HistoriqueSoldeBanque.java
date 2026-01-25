package staport.finance.gBanque.Bank.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "historique_solde_banque",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"banque_id", "mois", "annee"},
                name = "uk_banque_mois_annee"
        ))
public class HistoriqueSoldeBanque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @NotNull
    @Min(1)
    @Max(12)
    @Column(name = "mois", nullable = false)
    private Integer mois;

    @NotNull
    @Min(2000)
    @Max(2100)
    @Column(name = "annee", nullable = false)
    private Integer annee;

    @NotNull
    @Column(name = "solde_depart", nullable = false, precision = 19, scale = 2)
    private BigDecimal soldeDepart;

    @NotNull
    @Column(name = "solde_fin", nullable = false, precision = 19, scale = 2)
    private BigDecimal soldeFin;

    // --- Constructors ---

    public HistoriqueSoldeBanque() {}

    public HistoriqueSoldeBanque(Banque banque, Integer mois, Integer annee,
                                 BigDecimal soldeDepart, BigDecimal soldeFin) {
        this.banque = banque;
        this.mois = mois;
        this.annee = annee;
        this.soldeDepart = soldeDepart;
        this.soldeFin = soldeFin;
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Banque getBanque() {
        return banque;
    }

    public void setBanque(Banque banque) {
        this.banque = banque;
    }

    public Integer getMois() {
        return mois;
    }

    public void setMois(Integer mois) {
        this.mois = mois;
    }

    public Integer getAnnee() {
        return annee;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }

    public BigDecimal getSoldeDepart() {
        return soldeDepart;
    }

    public void setSoldeDepart(BigDecimal soldeDepart) {
        this.soldeDepart = soldeDepart;
    }

    public BigDecimal getSoldeFin() {
        return soldeFin;
    }

    public void setSoldeFin(BigDecimal soldeFin) {
        this.soldeFin = soldeFin;
    }

    // --- equals / hashCode ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistoriqueSoldeBanque)) return false;
        HistoriqueSoldeBanque that = (HistoriqueSoldeBanque) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- toString ---

    @Override
    public String toString() {
        return "HistoriqueSoldeBanque{" +
                "id=" + id +
                ", mois=" + mois +
                ", annee=" + annee +
                ", soldeDepart=" + soldeDepart +
                ", soldeFin=" + soldeFin +
                '}';
    }
}