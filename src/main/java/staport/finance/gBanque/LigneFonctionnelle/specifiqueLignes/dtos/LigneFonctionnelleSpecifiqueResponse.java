    package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos;

    import java.math.BigDecimal;
    import java.time.LocalDate;
import java.time.OffsetDateTime;

    /**
     * DTO de réponse complet pour les lignes fonctionnelles spécifiques.
     *
     * ✅ AMÉLIORATIONS:
     * - Ajout de 'encours' et 'solde' pour les natures qui le nécessitent
     * - Permet d'avoir UN SEUL DTO pour toutes les vues
     * - Les champs encours/solde seront NULL pour les natures qui ne les calculent pas
     */
    public class LigneFonctionnelleSpecifiqueResponse {

        private Long id;
        private Long banqueId;
        private String banqueNom;
        private Long natureLigneFonctionnelleId;
        private String natureLigneFonctionnelleLabel;
        private String natureLigneFonctionnelleDescription;
        private BigDecimal plafond;

        // ✅ NOUVEAUX CHAMPS pour le suivi financier
        private BigDecimal encours;  // NULL si non applicable à la nature
        private BigDecimal solde;    // NULL si non applicable à la nature

        private BigDecimal taux;
        private LocalDate dateDepart ;
        private LocalDate dateFin ;
        private OffsetDateTime dateCreation;
        private OffsetDateTime dateModification;
        private boolean actif;
        private boolean expired;
        private Long marcheId;
        private String marcheNom;
        private String marcheNumero;

        // --- Constructors ---
        public LigneFonctionnelleSpecifiqueResponse() {
        }

        public LigneFonctionnelleSpecifiqueResponse(
                Long id,
                Long banqueId,
                String banqueNom,
                Long natureLigneFonctionnelleId,
                String natureLigneFonctionnelleLabel,
                String natureLigneFonctionnelleDescription,
                BigDecimal plafond,
                BigDecimal encours,  // ✅ Ajouté
                BigDecimal solde,    // ✅ Ajouté
                BigDecimal taux,
                LocalDate dateDepart,
                LocalDate dateFin,
                OffsetDateTime dateCreation,
                OffsetDateTime dateModification,
                boolean actif,
                Long marcheId,
                String marcheNom,
                String marcheNumero) {
            this.id = id;
            this.banqueId = banqueId;
            this.banqueNom = banqueNom;
            this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
            this.natureLigneFonctionnelleLabel = natureLigneFonctionnelleLabel;
            this.natureLigneFonctionnelleDescription = natureLigneFonctionnelleDescription;
            this.plafond = plafond;
            this.encours = encours;  // ✅ Ajouté
            this.solde = solde;      // ✅ Ajouté
            this.taux = taux;
            this.dateDepart = dateDepart;
            this.dateFin = dateFin;
            this.dateCreation = dateCreation;
            this.dateModification = dateModification;
            this.actif = actif;
            this.marcheId = marcheId;
            this.marcheNom = marcheNom;
            this.marcheNumero = marcheNumero;
        }

        // --- Getters and Setters ---
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getBanqueId() { return banqueId; }
        public void setBanqueId(Long banqueId) { this.banqueId = banqueId; }

        public String getBanqueNom() { return banqueNom; }
        public void setBanqueNom(String banqueNom) { this.banqueNom = banqueNom; }

        public Long getNatureLigneFonctionnelleId() { return natureLigneFonctionnelleId; }
        public void setNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId) {
            this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
        }

        public String getNatureLigneFonctionnelleLabel() { return natureLigneFonctionnelleLabel; }
        public void setNatureLigneFonctionnelleLabel(String natureLigneFonctionnelleLabel) {
            this.natureLigneFonctionnelleLabel = natureLigneFonctionnelleLabel;
        }

        public String getNatureLigneFonctionnelleDescription() { return natureLigneFonctionnelleDescription; }
        public void setNatureLigneFonctionnelleDescription(String natureLigneFonctionnelleDescription) {
            this.natureLigneFonctionnelleDescription = natureLigneFonctionnelleDescription;
        }

        public BigDecimal getPlafond() { return plafond; }
        public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }

        // ✅ NOUVEAUX GETTERS/SETTERS
        public BigDecimal getEncours() { return encours; }
        public void setEncours(BigDecimal encours) { this.encours = encours; }

        public BigDecimal getSolde() { return solde; }
        public void setSolde(BigDecimal solde) { this.solde = solde; }

        public BigDecimal getTaux() { return taux; }
        public void setTaux(BigDecimal taux) { this.taux = taux; }

        public LocalDate getDateDepart() { return dateDepart; }
        public void setDateDepart(LocalDate dateDepart) { this.dateDepart = dateDepart; }

        public LocalDate getDateFin() { return dateFin; }
        public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

        public OffsetDateTime getDateCreation() { return dateCreation; }
        public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }

        public OffsetDateTime getDateModification() { return dateModification; }
        public void setDateModification(OffsetDateTime dateModification) {
            this.dateModification = dateModification;
        }

        public boolean isActif() { return actif; }
        public void setActif(boolean actif) { this.actif = actif; }

        public boolean isExpired() { return expired; }
        public void setExpired(boolean expired) { this.expired = expired; }

        public Long getMarcheId() { return marcheId; }
        public void setMarcheId(Long marcheId) { this.marcheId = marcheId; }

        public String getMarcheNom() { return marcheNom; }
        public void setMarcheNom(String marcheNom) { this.marcheNom = marcheNom; }

        public String getMarcheNumero() { return marcheNumero; }
        public void setMarcheNumero(String marcheNumero) { this.marcheNumero = marcheNumero; }

        @Override
        public String toString() {
            return "LigneFonctionnelleSpecifiqueResponse{" +
                    "id=" + id +
                    ", banqueId=" + banqueId +
                    ", banqueNom='" + banqueNom + '\'' +
                    ", natureLigneFonctionnelleId=" + natureLigneFonctionnelleId +
                    ", natureLigneFonctionnelleLabel='" + natureLigneFonctionnelleLabel + '\'' +
                    ", natureLigneFonctionnelleDescription='" + natureLigneFonctionnelleDescription + '\'' +
                    ", plafond=" + plafond +
                    ", encours=" + encours +
                    ", solde=" + solde +
                    ", taux=" + taux +
                    ", dateDepart=" + dateDepart +
                    ", dateFin=" + dateFin +
                    ", dateCreation=" + dateCreation +
                    ", dateModification=" + dateModification +
                    ", actif=" + actif +
                    ", expired=" + expired +
                    ", marcheId=" + marcheId +
                    ", marcheNom='" + marcheNom + '\'' +
                    ", marcheNumero='" + marcheNumero + '\'' +
                    '}';
        }
    }