    package staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities;

    import jakarta.persistence.*;
    import jakarta.persistence.Index;
    import org.hibernate.envers.Audited;
    import org.hibernate.envers.NotAudited;
    import staport.finance.gBanque.Bank.entities.Banque;
    import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
    import staport.finance.gBanque.marches.entities.Marche;
    import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;

    import java.math.BigDecimal;
    import java.time.LocalDate;
import java.time.OffsetDateTime;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.Objects;

    @Entity
    @Audited
    @Table(
        name = "ligne_fonctionnelle",
        indexes = {
            @Index(name = "idx_lf_banque_id",  columnList = "banque_id"),
            @Index(name = "idx_lf_nature_id",  columnList = "nature_ligne_id"),
            @Index(name = "idx_lf_actif",      columnList = "actif"),
            @Index(name = "idx_lf_date_depart", columnList = "date_depart"),
            @Index(name = "idx_lf_ligne_mere_id", columnList = "ligne_mere_id")
        }
    )
    public class LigneFonctionnelle {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "banque_id", nullable = false)
        private Banque banque;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "nature_ligne_id", nullable = false)
        private NatureLigneFonctionnelle natureLigneFonctionnelle;

        /**
         * {@code Marche} est {@code @Audited} depuis le sweep de conformité du module Marchés —
         * les changements de rattachement d'un marché à cette ligne apparaissent donc aussi dans
         * l'historique Envers de {@code LigneFonctionnelle} (table de jointure incluse).
         */
        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(
                name = "ligne_fonctionnelle_marche",
                joinColumns = @JoinColumn(name = "ligne_id"),
                inverseJoinColumns = @JoinColumn(name = "marche_id")
        )
        private List<Marche> marches = new ArrayList<>();

        @Column
        private BigDecimal plafond;

        @Column(nullable = false)
        private BigDecimal taux;

        @Column(name = "date_depart", nullable = false)
        private LocalDate dateDepart ;

        @Column(name = "date_fin", nullable = false)
        private LocalDate dateFin ;

        @Column(name = "date_creation", nullable = false, updatable = false)
        private OffsetDateTime dateCreation;

        @Column(name = "date_modification", nullable = false)
        private OffsetDateTime dateModification;

        @Column(nullable = false)
        private boolean actif = true;


        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "ligne_mere_id")
        private LigneFonctionnelle ligneMere;


        @OneToMany(mappedBy = "ligneMere", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<LigneFonctionnelle> sousLignes = new ArrayList<>();

        @OneToMany(mappedBy = "ligneFonctionnelle", cascade = CascadeType.ALL)
        @NotAudited
        private List<Lcn> lcns;

        // --- Constructors ---
        public LigneFonctionnelle() {
            // Default constructor required by JPA
        }


        public LigneFonctionnelle(Long id, Banque banque, NatureLigneFonctionnelle natureLigneFonctionnelle, BigDecimal plafond, BigDecimal taux, LocalDate dateDepart, LocalDate dateFin, OffsetDateTime dateCreation, OffsetDateTime dateModification, boolean actif) {
            this.id = id;
            this.banque = banque;
            this.natureLigneFonctionnelle = natureLigneFonctionnelle;
            this.plafond = plafond;
            this.taux = taux;
            this.dateDepart = dateDepart;
            this.dateFin = dateFin;
            this.dateCreation = dateCreation;
            this.dateModification = dateModification;
            this.actif = actif; // Set actif
        }

        // --- Getters and Setters ---
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Banque getBanque() { return banque; }
        public void setBanque(Banque banque) { this.banque = banque; }

        public NatureLigneFonctionnelle getNatureLigneFonctionnelle() {
            return natureLigneFonctionnelle;
        }

        public void setNatureLigneFonctionnelle(NatureLigneFonctionnelle natureLigneFonctionnelle) {
            this.natureLigneFonctionnelle = natureLigneFonctionnelle;
        }

        public List<Marche> getMarches() {
            return marches;
        }

        public void setMarches(List<Marche> marches) {
            this.marches = marches;
        }

        public BigDecimal getPlafond() { return plafond; }
        public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }

        public BigDecimal getTaux() { return taux; }
        public void setTaux(BigDecimal taux) { this.taux = taux; }

        public LocalDate getDateDepart() { return dateDepart; }
        public void setDateDepart(LocalDate dateDepart) { this.dateDepart = dateDepart; }

        public LocalDate getDateFin() { return dateFin; }
        public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

        public OffsetDateTime getDateCreation() { return dateCreation; }
        public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }

        public OffsetDateTime getDateModification() { return dateModification; }
        public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }


        public boolean isActif() {
            return actif;
        }

        public void setActif(boolean actif) {
            this.actif = actif;
        }


        public LigneFonctionnelle getLigneMere() {
            return ligneMere;
        }

        public void setLigneMere(LigneFonctionnelle ligneMere) {
            this.ligneMere = ligneMere;
        }


        public List<LigneFonctionnelle> getSousLignes() {
            return sousLignes;
        }

        public void setSousLignes(List<LigneFonctionnelle> sousLignes) {
            this.sousLignes = sousLignes;
        }

        public List<Lcn> getLcns() {
            return lcns;
        }

        public void setLcns(List<Lcn> lcns) {
            this.lcns = lcns;
        }

        @PrePersist
        protected void onCreate() {
            OffsetDateTime now = OffsetDateTime.now();
            this.dateCreation = now;
            this.dateModification = now;
            if (!this.actif) { // Ensure default is true if not set
                this.actif = true;
            }
        }

        @PreUpdate
        protected void onUpdate() {
            this.dateModification = OffsetDateTime.now();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            LigneFonctionnelle that = (LigneFonctionnelle) o;
            return actif == that.actif && Objects.equals(id, that.id) && Objects.equals(banque, that.banque) && Objects.equals(natureLigneFonctionnelle, that.natureLigneFonctionnelle) && Objects.equals(plafond, that.plafond)  && Objects.equals(taux, that.taux) && Objects.equals(dateDepart, that.dateDepart) && Objects.equals(dateFin, that.dateFin) && Objects.equals(dateCreation, that.dateCreation) && Objects.equals(dateModification, that.dateModification) && Objects.equals(ligneMere, that.ligneMere) && Objects.equals(sousLignes, that.sousLignes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, banque, natureLigneFonctionnelle, plafond, taux, dateDepart, dateFin, dateCreation, dateModification, actif, ligneMere, sousLignes);
        }

        @Override
        public String toString() {
            return "LigneFonctionnelle{" +
                    "id=" + id +
                    ", banque=" + banque +
                    ", natureLigneFonctionnelle=" + natureLigneFonctionnelle +
                    ", marches=" + marches +
                    ", plafond=" + plafond +
                    ", taux=" + taux +
                    ", dateDepart=" + dateDepart +
                    ", dateFin=" + dateFin +
                    ", dateCreation=" + dateCreation +
                    ", dateModification=" + dateModification +
                    ", actif=" + actif +
                    ", ligneMere=" + ligneMere +
                    ", sousLignes=" + sousLignes +
                    '}';
        }
    }
