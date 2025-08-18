package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;

import java.math.BigDecimal;
// import java.time.OffsetDateTime; // Plus nécessaire pour les champs directs de date

/**
 * DTO utilisé pour les critères de recherche avancée des échéances.
 * Peut être utilisé pour filtrer le tableau d'amortissement ou pour une recherche globale.
 */
public class EcheanceLeasingSearchDTO {
    private Long contratLeasingId;      // Optionnel : si la recherche est globale et on veut filtrer par contrat
    private Integer numeroEcheance;
    private Integer month;              // Nouveau champ : mois de l'échéance (1-12)
    private Integer year;               // Nouveau champ : année de l'échéance
    private StatutEcheanceLeasing statutPaiement;


    // --- Getters et Setters ---
    public Long getContratLeasingId() {
        return contratLeasingId;
    }

    public void setContratLeasingId(Long contratLeasingId) {
        this.contratLeasingId = contratLeasingId;
    }

    public Integer getNumeroEcheance() {
        return numeroEcheance;
    }

    public void setNumeroEcheance(Integer numeroEcheance) {
        this.numeroEcheance = numeroEcheance;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }




    public StatutEcheanceLeasing getStatutPaiement() {
        return statutPaiement;
    }

    public void setStatutPaiement(StatutEcheanceLeasing statutPaiement) {
        this.statutPaiement = statutPaiement;
    }


}