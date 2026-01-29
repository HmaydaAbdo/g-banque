package staport.finance.gBanque.recapitulatif.dtos;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class RecaputilatifItem {
    private Long id;
    private OffsetDateTime dateEmission;
    private OffsetDateTime dateEcheance;
    private String libelle;
    private BigDecimal debit;
    private BigDecimal credit;
    private String observations;
    private TransactionSource source;

    public RecaputilatifItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Getters
    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public String getLibelle() {
        return libelle;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public String getObservations() {
        return observations;
    }

    // Setters
    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public OffsetDateTime getDateEcheance() {
        return dateEcheance;
    }
    public void setDateEcheance(OffsetDateTime dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public TransactionSource getSource() {
        return source;
    }

    public void setSource(TransactionSource source) {
        this.source = source;
    }
}