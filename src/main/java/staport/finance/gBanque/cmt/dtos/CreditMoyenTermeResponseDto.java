package staport.finance.gBanque.cmt.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO utilisé pour renvoyer les informations d'un crédit moyen terme au client.
 */
public class CreditMoyenTermeResponseDto {

    private Long id;
    private BanqueDto banque;
    private String designation;
    private BigDecimal nominal;
    private Integer periodicite;
    private Integer duree;
    private BigDecimal echeance;
    private BigDecimal taux;
    private OffsetDateTime dateDebut;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateModification;
    private boolean completed;

    public CreditMoyenTermeResponseDto() {}

    // Getters et Setters pour tous les champs
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BanqueDto getBanque() { return banque; }
    public void setBanque(BanqueDto banque) { this.banque = banque; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public BigDecimal getNominal() { return nominal; }
    public void setNominal(BigDecimal nominal) { this.nominal = nominal; }
    public Integer getPeriodicite() { return periodicite; }
    public void setPeriodicite(Integer periodicite) { this.periodicite = periodicite; }
    public Integer getDuree() { return duree; }
    public void setDuree(Integer duree) { this.duree = duree; }
    public BigDecimal getEcheance() { return echeance; }
    public void setEcheance(BigDecimal echeance) { this.echeance = echeance; }
    public BigDecimal getTaux() { return taux; }
    public void setTaux(BigDecimal taux) { this.taux = taux; }
    public OffsetDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(OffsetDateTime dateDebut) { this.dateDebut = dateDebut; }
    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }
    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    /**
     * DTO imbriqué pour la banque.
     */
    public static class BanqueDto {
        private Long id;
        private String nom;

        public BanqueDto() {}
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        @Override
        public String toString() {
            return "BanqueDto{" +
                    "id=" + id +
                    ", nom='" + nom + '\'' +
                    '}';
        }
    }
}