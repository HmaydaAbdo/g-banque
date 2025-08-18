package staport.finance.gBanque.cmt.echeances.dtos;


import org.springframework.data.domain.Page;
import java.math.BigDecimal;

/**
 * DTO de réponse pour le tableau d'amortissement d'un crédit à moyen terme.
 * Il regroupe les informations agrégées du crédit et la liste paginée de ses échéances.
 */
public class TableauAmortissementCMTResponseDto {

    private Long creditId;
    private String designationCredit;
    private BigDecimal montantTotalAPayer;
    private BigDecimal montantTotalDejaPaye;
    private BigDecimal montantRestantAPayer;
    private Page<EcheanceCMTResponseDto> echeances;

    // Constructeur pour faciliter la création de l'objet
    public TableauAmortissementCMTResponseDto(Long creditId, String designationCredit, BigDecimal montantTotalAPayer, BigDecimal montantTotalDejaPaye, BigDecimal montantRestantAPayer, Page<EcheanceCMTResponseDto> echeances) {
        this.creditId = creditId;
        this.designationCredit = designationCredit;
        this.montantTotalAPayer = montantTotalAPayer;
        this.montantTotalDejaPaye = montantTotalDejaPaye;
        this.montantRestantAPayer = montantRestantAPayer;
        this.echeances = echeances;
    }

    // Getters et Setters
    public Long getCreditId() { return creditId; }
    public void setCreditId(Long creditId) { this.creditId = creditId; }
    public BigDecimal getMontantTotalAPayer() { return montantTotalAPayer; }
    public void setMontantTotalAPayer(BigDecimal montantTotalAPayer) { this.montantTotalAPayer = montantTotalAPayer; }
    public String getDesignationCredit() {
        return designationCredit;
    }
    public void setDesignationCredit(String designationCredit) {
        this.designationCredit = designationCredit;
    }
    public BigDecimal getMontantTotalDejaPaye() { return montantTotalDejaPaye; }
    public void setMontantTotalDejaPaye(BigDecimal montantTotalDejaPaye) { this.montantTotalDejaPaye = montantTotalDejaPaye; }
    public BigDecimal getMontantRestantAPayer() { return montantRestantAPayer; }
    public void setMontantRestantAPayer(BigDecimal montantRestantAPayer) { this.montantRestantAPayer = montantRestantAPayer; }
    public Page<EcheanceCMTResponseDto> getEcheances() { return echeances; }
    public void setEcheances(Page<EcheanceCMTResponseDto> echeances) { this.echeances = echeances; }
}