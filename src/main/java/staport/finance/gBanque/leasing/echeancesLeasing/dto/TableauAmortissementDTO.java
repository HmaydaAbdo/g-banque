package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * DTO représentant le tableau d'amortissement complet d'un contrat de leasing.
 * Inclut les échéances détaillées et les agrégats financiers.
 */
public class TableauAmortissementDTO {
    private Long contratLeasingId;
    private String numeroContrat;
    private BigDecimal totalIndemniteAPayer; // Somme des mensualités TTC du contrat + Valeur Résiduelle
    private BigDecimal montantTotalDejaPaye; // Somme des mensualités TTC des échéances PAYE
    private BigDecimal montantRestantAPayer; // totalIndemniteAPayer - montantTotalDejaPaye
    private Page<EcheanceLeasingResponseDTO> echeances; // Liste détaillée des échéances

    // --- Constructeurs, Getters et Setters ---
    public TableauAmortissementDTO() {}

    public TableauAmortissementDTO(Long contratLeasingId, String numeroContrat, BigDecimal totalIndemniteAPayer, BigDecimal montantTotalDejaPaye, BigDecimal montantRestantAPayer, Page<EcheanceLeasingResponseDTO> echeances) {
        this.contratLeasingId = contratLeasingId;
        this.numeroContrat = numeroContrat;
        this.totalIndemniteAPayer = totalIndemniteAPayer;
        this.montantTotalDejaPaye = montantTotalDejaPaye;
        this.montantRestantAPayer = montantRestantAPayer;
        this.echeances = echeances;
    }

    public Long getContratLeasingId() {
        return contratLeasingId;
    }

    public void setContratLeasingId(Long contratLeasingId) {
        this.contratLeasingId = contratLeasingId;
    }

    public String getNumeroContrat() {
        return numeroContrat;
    }

    public void setNumeroContrat(String numeroContrat) {
        this.numeroContrat = numeroContrat;
    }

    public BigDecimal getTotalIndemniteAPayer() {
        return totalIndemniteAPayer;
    }

    public void setTotalIndemniteAPayer(BigDecimal totalIndemniteAPayer) {
        this.totalIndemniteAPayer = totalIndemniteAPayer;
    }

    public BigDecimal getMontantTotalDejaPaye() {
        return montantTotalDejaPaye;
    }

    public void setMontantTotalDejaPaye(BigDecimal montantTotalDejaPaye) {
        this.montantTotalDejaPaye = montantTotalDejaPaye;
    }

    public BigDecimal getMontantRestantAPayer() {
        return montantRestantAPayer;
    }

    public void setMontantRestantAPayer(BigDecimal montantRestantAPayer) {
        this.montantRestantAPayer = montantRestantAPayer;
    }

    public Page<EcheanceLeasingResponseDTO> getEcheances() {
        return echeances;
    }

    public void setEcheances(Page<EcheanceLeasingResponseDTO> echeances) {
        this.echeances = echeances;
    }

    @Override
    public String toString() {
        return "TableauAmortissementDTO{" +
                "contratLeasingId=" + contratLeasingId +
                ", numeroContrat='" + numeroContrat + '\'' +
                ", totalIndemniteAPayer=" + totalIndemniteAPayer +
                ", montantTotalDejaPaye=" + montantTotalDejaPaye +
                ", montantRestantAPayer=" + montantRestantAPayer +
                ", echeances=" + echeances +
                '}';
    }
}