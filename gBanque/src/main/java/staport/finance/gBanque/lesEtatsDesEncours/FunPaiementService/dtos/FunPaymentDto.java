package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.deserializer.ZeroDateToNullLocalDateDeserializer;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;


public class FunPaymentDto {

    public FunPaymentDto() {}

    public FunPaymentDto(String numero, OffsetDateTime dateCheque, OffsetDateTime datePaiement, OffsetDateTime dateEcheance, BigDecimal montant, String beneficiaire, String ville, String banque, int type, int codeErreur) {
        this.numero = numero;
        this.dateCheque = dateCheque;
        this.datePaiement = datePaiement;
        this.dateEcheance = dateEcheance;
        this.montant = montant;
        this.beneficiaire = beneficiaire;
        this.ville = ville;
        this.banque = banque;
        this.type = type;
        this.codeErreur = codeErreur;
    }

    private String numero;

    @JsonProperty("date_chaque")
    @JsonDeserialize(using = ZeroDateToNullLocalDateDeserializer.class)
    private OffsetDateTime dateCheque;

    @JsonProperty("date_paiement")
    @JsonDeserialize(using = ZeroDateToNullLocalDateDeserializer.class)
    private OffsetDateTime datePaiement;

    @JsonProperty("date_echeance")
    @JsonDeserialize(using = ZeroDateToNullLocalDateDeserializer.class)
    private OffsetDateTime dateEcheance;

    private BigDecimal montant;

    @JsonProperty("beneficiare")
    private String beneficiaire;

    private String ville;

    @JsonProperty("banque_")
    private String banque;

    private int type;

    @JsonProperty("CodeErreur")
    private int codeErreur;

    // --- GETTERS AND SETTERS: Update types for dateCheque and datePaiement to LocalDate ---
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public OffsetDateTime getDateCheque() { return dateCheque; } // <--- CHANGED
    public void setDateCheque(OffsetDateTime dateCheque) { this.dateCheque = dateCheque; } // <--- CHANGED

    public OffsetDateTime getDatePaiement() { return datePaiement; } // <--- CHANGED
    public void setDatePaiement(OffsetDateTime datePaiement) { this.datePaiement = datePaiement; } // <--- CHANGED

    public OffsetDateTime getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(OffsetDateTime dateEcheance) { this.dateEcheance = dateEcheance; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public String getBanque() { return banque; }
    public void setBanque(String banque) { this.banque = banque; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public int getCodeErreur() { return codeErreur; }
    public void setCodeErreur(int codeErreur) { this.codeErreur = codeErreur; }

    @Override
    public String toString() {
        return "FunPaymentDto{" +
                "type=" + type +
                ", banque='" + banque + '\'' +
                ", ville='" + ville + '\'' +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", montant=" + montant +
                ", dateEcheance=" + dateEcheance +
                ", datePaiement=" + datePaiement +
                ", dateCheque=" + dateCheque +
                ", numero='" + numero + '\'' +
                '}';
    }
}