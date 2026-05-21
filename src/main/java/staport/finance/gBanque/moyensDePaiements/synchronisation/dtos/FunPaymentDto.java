// File: staport/finance/gBanque.lesEtatsDesEncours.FunPaiementService.dtos/FunPaymentDto.java

package staport.finance.gBanque.moyensDePaiements.synchronisation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import staport.finance.gBanque.moyensDePaiements.synchronisation.deserializer.ZeroDateToNullLocalDateDeserializer;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FunPaymentDto {

    private String numero;

    @JsonProperty("date_chaque")
    @JsonDeserialize(using = ZeroDateToNullLocalDateDeserializer.class)
    private LocalDate dateCheque;

    @JsonProperty("date_echeance")
    @JsonDeserialize(using = ZeroDateToNullLocalDateDeserializer.class)
    private LocalDate dateEcheance;

    @JsonProperty("date_creation")
    @JsonDeserialize(using = ZeroDateToNullLocalDateDeserializer.class)
    private LocalDate dateCreation;

    private BigDecimal montant;

    @JsonProperty("beneficiare")
    private String beneficiaire;

    private String ville;

    @JsonProperty("banque_")
    private String banque;

    @JsonProperty("banque_beneficiare")
    private String banqueBeneficiaire;

    @JsonProperty("RIB_beneficiare")
    private String ribBeneficiaire;

    @JsonProperty("CIN_beneficiare")
    private String cinBeneficiaire;

    private int type;

    @JsonProperty("CodeErreur")
    private int codeErreur;

    /**
     * Statut reçu de FunPaiement : 1=ENCOURS, 2=REMIS(ignoré), 3=ANNULE, 4=VALIDE(ignoré).
     */
    @JsonProperty("statut")
    private int statut;

    public FunPaymentDto() {}

    // Constructor updated to include new fields
    public FunPaymentDto(String numero, LocalDate dateCheque, LocalDate dateEcheance, BigDecimal montant, String beneficiaire, String ville, String banque, String banqueBeneficiaire, String ribBeneficiaire, String cinBeneficiaire, int type, int codeErreur) {
        this.numero = numero;
        this.dateCheque = dateCheque;
        this.dateEcheance = dateEcheance;
        this.montant = montant;
        this.beneficiaire = beneficiaire;
        this.ville = ville;
        this.banque = banque;
        this.banqueBeneficiaire = banqueBeneficiaire;
        this.ribBeneficiaire = ribBeneficiaire;
        this.cinBeneficiaire = cinBeneficiaire;
        this.type = type;
        this.codeErreur = codeErreur;
    }

    // --- GETTERS AND SETTERS ---
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public LocalDate getDateCheque() { return dateCheque; }
    public void setDateCheque(LocalDate dateCheque) { this.dateCheque = dateCheque; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getBanque() { return banque; }
    public void setBanque(String banque) { this.banque = banque; }

    public String getBanqueBeneficiaire() { return banqueBeneficiaire; }
    public void setBanqueBeneficiaire(String banqueBeneficiaire) { this.banqueBeneficiaire = banqueBeneficiaire; }

    public String getRibBeneficiaire() { return ribBeneficiaire; }
    public void setRibBeneficiaire(String ribBeneficiaire) { this.ribBeneficiaire = ribBeneficiaire; }

    public String getCinBeneficiaire() { return cinBeneficiaire; }
    public void setCinBeneficiaire(String cinBeneficiaire) { this.cinBeneficiaire = cinBeneficiaire; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public int getCodeErreur() { return codeErreur; }
    public void setCodeErreur(int codeErreur) { this.codeErreur = codeErreur; }

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public int getStatut() { return statut; }
    public void setStatut(int statut) { this.statut = statut; }

    /**
     * Convertit le code entier en StatutMoyenPaiement.
     * Retourne ENCOURS par défaut pour tout code inconnu ou non géré (2, 4, etc.).
     */
    public StatutMoyenPaiement getStatutMoyenPaiement() {
        return statut == 3 ? StatutMoyenPaiement.ANNULE : StatutMoyenPaiement.ENCOURS;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return "FunPaymentDto{" +
                "numero='" + numero + '\'' +
                ", dateCheque=" + dateCheque +
                ", dateEcheance=" + dateEcheance +
                ", montant=" + montant +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", ville='" + ville + '\'' +
                ", banque='" + banque + '\'' +
                ", banqueBeneficiaire='" + banqueBeneficiaire + '\'' +
                ", ribBeneficiaire='" + ribBeneficiaire + '\'' +
                ", cinBeneficiaire='" + cinBeneficiaire + '\'' +
                ", type=" + type +
                ", codeErreur=" + codeErreur +
                ", statut=" + statut +
                '}';
    }
}
