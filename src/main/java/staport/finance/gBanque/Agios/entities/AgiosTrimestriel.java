package staport.finance.gBanque.Agios.entities;


import jakarta.persistence.*;
import staport.finance.gBanque.Agios.enums.StatutTrimestre;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Bank.entities.Banque;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "agios_trimestriel",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_trim_banque_type",
                columnNames = {"trimestre", "annee", "banque_id", "type_agios"}
        ))
public class AgiosTrimestriel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_agios", nullable = false, length = 50)
    private TypeAgios typeAgios;

    @Column(nullable = false)
    private int trimestre;

    @Column(nullable = false)
    private int annee;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Column(name = "total_agios_ht", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAgiosHT = BigDecimal.ZERO;

    @Column(name = "total_tva", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalTVA = BigDecimal.ZERO;

    @Column(name = "total_agios_ttc", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAgiosTTC = BigDecimal.ZERO;

    @Column(name = "nb_jours_calcules", nullable = false)
    private int nbJoursCalcules = 0;

    @Column(name = "nb_jours_total", nullable = false)
    private int nbJoursTotal = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutTrimestre statut = StatutTrimestre.OUVERT;

    @Column(name = "date_dernier_calcul")
    private OffsetDateTime dateDernierCalcul;

    @Column(name = "date_cloture")
    private OffsetDateTime dateCloture;

    @Column(name = "agios_definitif_id")
    private Long agiosDefinitifId;

    @OneToMany(mappedBy = "trimestre", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AgiosJournalier> agiosJournaliers = new ArrayList<>();

    public AgiosTrimestriel() {}

    public boolean isOuvert() { return statut == StatutTrimestre.OUVERT; }

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TypeAgios getTypeAgios() { return typeAgios; }
    public void setTypeAgios(TypeAgios typeAgios) { this.typeAgios = typeAgios; }

    public int getTrimestre() { return trimestre; }
    public void setTrimestre(int trimestre) { this.trimestre = trimestre; }

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }

    public BigDecimal getTotalAgiosHT() { return totalAgiosHT; }
    public void setTotalAgiosHT(BigDecimal v) { this.totalAgiosHT = v; }

    public BigDecimal getTotalTVA() { return totalTVA; }
    public void setTotalTVA(BigDecimal v) { this.totalTVA = v; }

    public BigDecimal getTotalAgiosTTC() { return totalAgiosTTC; }
    public void setTotalAgiosTTC(BigDecimal v) { this.totalAgiosTTC = v; }

    public int getNbJoursCalcules() { return nbJoursCalcules; }
    public void setNbJoursCalcules(int v) { this.nbJoursCalcules = v; }

    public int getNbJoursTotal() { return nbJoursTotal; }
    public void setNbJoursTotal(int v) { this.nbJoursTotal = v; }

    public StatutTrimestre getStatut() { return statut; }
    public void setStatut(StatutTrimestre statut) { this.statut = statut; }

    public OffsetDateTime getDateDernierCalcul() { return dateDernierCalcul; }
    public void setDateDernierCalcul(OffsetDateTime v) { this.dateDernierCalcul = v; }

    public OffsetDateTime getDateCloture() { return dateCloture; }
    public void setDateCloture(OffsetDateTime v) { this.dateCloture = v; }

    public Long getAgiosDefinitifId() { return agiosDefinitifId; }
    public void setAgiosDefinitifId(Long v) { this.agiosDefinitifId = v; }

    public List<AgiosJournalier> getAgiosJournaliers() { return agiosJournaliers; }
    public void setAgiosJournaliers(List<AgiosJournalier> v) { this.agiosJournaliers = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgiosTrimestriel that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return typeAgios + " T" + trimestre + "/" + annee +
                " [" + (banque != null ? banque.getNom() : "?") + "] " +
                statut + " TTC=" + totalAgiosTTC;
    }
}