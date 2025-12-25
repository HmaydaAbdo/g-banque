package staport.finance.gBanque.validation.dtos.prefinancements;


import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PrefinancementTransactionResponse {

    private Long id;
    private String banqueName;
    private String marcheNumero;
    private String libelle;
    private String observation;
    private OffsetDateTime dateEmission;
    private OffsetDateTime dateValeur;
    private OffsetDateTime dateOperations;
    private BigDecimal debit;
    private BigDecimal credit;
    private final TransactionSource transactionSource = TransactionSource.PREFINANCEMENT;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }



    public TransactionSource getTransactionSource() {
        return transactionSource;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }



    public OffsetDateTime getDateOperations() {
        return dateOperations;
    }

    public void setDateOperations(OffsetDateTime dateOperations) {
        this.dateOperations = dateOperations;
    }

    public OffsetDateTime getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(OffsetDateTime dateValeur) {
        this.dateValeur = dateValeur;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getMarcheNumero() {
        return marcheNumero;
    }

    public void setMarcheNumero(String marcheNumero) {
        this.marcheNumero = marcheNumero;
    }

    public String getBanqueName() {
        return banqueName;
    }

    public void setBanqueName(String banqueName) {
        this.banqueName = banqueName;
    }
}
