package staport.finance.gBanque.validation.dtos.prefinancements;


import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PrefinancementTransactionResponse {

    private Long id;
    private String banqueName;
    private String marcheNumero;
    private String libelle;
    private String observation;
    private LocalDate dateEmission ;
    private LocalDate dateValeur ;
    private LocalDate dateOperations ;
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



    public LocalDate getDateOperations() {
        return dateOperations;
    }

    public void setDateOperations(LocalDate dateOperations) {
        this.dateOperations = dateOperations;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
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
