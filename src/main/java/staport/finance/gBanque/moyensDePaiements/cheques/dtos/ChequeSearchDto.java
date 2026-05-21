package staport.finance.gBanque.moyensDePaiements.cheques.dtos;


import java.math.BigDecimal;
import java.time.LocalDate;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

public class ChequeSearchDto {

    private LocalDate dateEmission ;
    private BigDecimal montant;
    private String emetteur;

    private String numero;
    private String beneficiaire;
    private String nomBanque;
    private StatutMoyenPaiement statut;


    // Default constructor
    public ChequeSearchDto() {
    }

    // Getters and Setters
    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public StatutMoyenPaiement getStatut() { return statut; }

    public void setStatut(StatutMoyenPaiement statut) { this.statut = statut; }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    @Override
    public String toString() {
        return "ChequeSearchDto{" +
                "dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", emetteur='" + emetteur + '\'' +
                ", numero='" + numero + '\'' +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", nomBanque='" + nomBanque + '\'' +
                ", statut=" + statut +
                '}';
    }
}