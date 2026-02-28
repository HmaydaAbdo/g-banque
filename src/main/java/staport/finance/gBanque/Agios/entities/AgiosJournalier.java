package staport.finance.gBanque.Agios.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Bank.entities.Banque;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "agios_journalier",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agios_jour_banque_type",
                columnNames = {"date_valeur", "banque_id", "type_agios"}
        ))
public class AgiosJournalier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_agios", nullable = false, length = 50)
    private TypeAgios typeAgios;

    @Column(name = "date_valeur", nullable = false)
    private LocalDate dateValeur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    /** Solde reconstitué à la date de valeur */
    @Column(name = "solde_journalier", nullable = false, precision = 19, scale = 2)
    private BigDecimal soldeJournalier;

    /** Base de calcul (découvert pour FC, montant escompté pour escompte...) */
    @Column(name = "base_calcul", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseCalcul;

    /** Taux appliqué ce jour-là (en %) */
    @Column(name = "taux_applique", nullable = false, precision = 8, scale = 4)
    private BigDecimal tauxApplique;

    @Column(name = "agios_ht", nullable = false, precision = 19, scale = 2)
    private BigDecimal agiosHT;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tva;

    @Column(name = "agios_ttc", nullable = false, precision = 19, scale = 2)
    private BigDecimal agiosTTC;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trimestre_id")
    private AgiosTrimestriel trimestre;

    @Column(name = "date_calcul", nullable = false)
    private OffsetDateTime dateCalcul;

    @Column(name = "nb_recalculs", nullable = false)
    private int nbRecalculs = 0;

    /** Informations complémentaires libres (ID de ligne, détail...) */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    public AgiosJournalier() {}

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TypeAgios getTypeAgios() { return typeAgios; }
    public void setTypeAgios(TypeAgios typeAgios) { this.typeAgios = typeAgios; }

    public LocalDate getDateValeur() { return dateValeur; }
    public void setDateValeur(LocalDate dateValeur) { this.dateValeur = dateValeur; }

    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }

    public BigDecimal getSoldeJournalier() { return soldeJournalier; }
    public void setSoldeJournalier(BigDecimal soldeJournalier) { this.soldeJournalier = soldeJournalier; }

    public BigDecimal getBaseCalcul() { return baseCalcul; }
    public void setBaseCalcul(BigDecimal baseCalcul) { this.baseCalcul = baseCalcul; }

    public BigDecimal getTauxApplique() { return tauxApplique; }
    public void setTauxApplique(BigDecimal tauxApplique) { this.tauxApplique = tauxApplique; }

    public BigDecimal getAgiosHT() { return agiosHT; }
    public void setAgiosHT(BigDecimal agiosHT) { this.agiosHT = agiosHT; }

    public BigDecimal getTva() { return tva; }
    public void setTva(BigDecimal tva) { this.tva = tva; }

    public BigDecimal getAgiosTTC() { return agiosTTC; }
    public void setAgiosTTC(BigDecimal agiosTTC) { this.agiosTTC = agiosTTC; }

    public AgiosTrimestriel getTrimestre() { return trimestre; }
    public void setTrimestre(AgiosTrimestriel trimestre) { this.trimestre = trimestre; }

    public OffsetDateTime getDateCalcul() { return dateCalcul; }
    public void setDateCalcul(OffsetDateTime dateCalcul) { this.dateCalcul = dateCalcul; }

    public int getNbRecalculs() { return nbRecalculs; }
    public void setNbRecalculs(int nbRecalculs) { this.nbRecalculs = nbRecalculs; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgiosJournalier that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}