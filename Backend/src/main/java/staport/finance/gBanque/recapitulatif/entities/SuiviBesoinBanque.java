package staport.finance.gBanque.recapitulatif.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Besoin de financement d'une banque un jour donné, pour un horizon donné (zéro si la banque est couverte).
 * Écrit chaque nuit par {@code SuiviBesoinScheduler} ; sert à savoir depuis quand une banque est en besoin.
 */
@Entity
@Table(name = "suivi_besoin_banque",
        uniqueConstraints = @UniqueConstraint(name = "uk_suivi_besoin_jour_banque_periode", columnNames = {"jour", "banque_id", "periode"}),
        indexes = @Index(name = "idx_suivi_besoin_periode_jour", columnList = "periode, jour"))
public class SuiviBesoinBanque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate jour;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PeriodeRecap periode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal besoin;

    protected SuiviBesoinBanque() {
    }

    public SuiviBesoinBanque(LocalDate jour, Banque banque, PeriodeRecap periode, BigDecimal besoin) {
        this.jour = jour;
        this.banque = banque;
        this.periode = periode;
        this.besoin = besoin;
    }

    public Long getId() { return id; }
    public LocalDate getJour() { return jour; }
    public Banque getBanque() { return banque; }
    public PeriodeRecap getPeriode() { return periode; }
    public BigDecimal getBesoin() { return besoin; }
}
