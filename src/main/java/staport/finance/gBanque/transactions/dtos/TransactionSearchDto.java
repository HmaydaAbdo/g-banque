package staport.finance.gBanque.transactions.dtos;

import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for filtering transactions with a focused set of criteria.
 */
public class TransactionSearchDto {

    private String libelle;
    private Long banqueId;
    private Long rapprochementId;
    private List<TransactionSource> transactionSources;
    private String observation;
    private StatutRapprochement statutRapprochement;
    private BigDecimal montant;
    private LocalDate dateOperationDebut;
    private LocalDate dateOperationFin;
    // Getters and Setters

    public Long getRapprochementId() {
        return rapprochementId;
    }

    public void setRapprochementId(Long rapprochementId) {
        this.rapprochementId = rapprochementId;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public StatutRapprochement getStatutRapprochement() {
        return statutRapprochement;
    }

    public void setStatutRapprochement(StatutRapprochement statutRapprochement) {
        this.statutRapprochement = statutRapprochement;
    }

    public List<TransactionSource> getTransactionSources() {
        return transactionSources;
    }

    public void setTransactionSources(List<TransactionSource> transactionSources) {
        this.transactionSources = transactionSources;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public LocalDate getDateOperationDebut() {
        return dateOperationDebut;
    }

    public void setDateOperationDebut(LocalDate dateOperationDebut) {
        this.dateOperationDebut = dateOperationDebut;
    }

    public LocalDate getDateOperationFin() {
        return dateOperationFin;
    }

    public void setDateOperationFin(LocalDate dateOperationFin) {
        this.dateOperationFin = dateOperationFin;
    }

    @Override
    public String toString() {
        return "TransactionSearchDto{" +
                "libelle='" + libelle + '\'' +
                ", banqueId=" + banqueId +
                ", transactionSources=" + transactionSources +
                ", observation='" + observation + '\'' +
                ", statutRapprochement=" + statutRapprochement +
                ", montant=" + montant +
                ", dateOperationDebut=" + dateOperationDebut +
                ", dateOperationFin=" + dateOperationFin +
                '}';
    }
}