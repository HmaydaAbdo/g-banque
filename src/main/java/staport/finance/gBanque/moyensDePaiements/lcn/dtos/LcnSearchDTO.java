package staport.finance.gBanque.moyensDePaiements.lcn.dtos;


import java.math.BigDecimal;
import java.time.LocalDate;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

public class LcnSearchDTO {

    private LocalDate dateEmission ;
    private LocalDate dateEcheance ;
    private String emetteur;
    private BigDecimal montant;
    private String numero;
    private String beneficiaire;
    private String nomBanque;
    private StatutMoyenPaiement statut;

    public LcnSearchDTO() {
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public StatutMoyenPaiement getStatut() { return statut; }

    public void setStatut(StatutMoyenPaiement statut) { this.statut = statut; }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    @Override
    public String toString() {
        return "LcnSearchDTO{" +
                "dateEmission=" + dateEmission +
                ", dateEcheance=" + dateEcheance +
                ", emetteur='" + emetteur + '\'' +
                ", montant=" + montant +
                ", numero='" + numero + '\'' +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", nomBanque='" + nomBanque + '\'' +
                ", statut=" + statut +
                '}';
    }
}