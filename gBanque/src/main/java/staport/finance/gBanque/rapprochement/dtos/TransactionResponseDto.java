package staport.finance.gBanque.rapprochement.dtos;

import staport.finance.gBanque.rapprochement.enums.TransactionSource;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionResponseDto {

    private Long id;
    private OffsetDateTime dateEmission;
    private OffsetDateTime dateValeur;
    private OffsetDateTime dateOperations;
    private String observation;
    private Double tvaDeductible;
    private Double tvaCollectee;
    private String libelle;
    private TransactionSource transactionSource;
    private BigDecimal debit;
    private BigDecimal credit;
    private String traces;
    private Long banqueDePrelevementId;
    private String banqueDePrelevementNom;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Constructors, Getters, and Setters
    public TransactionResponseDto() {
    }

    public TransactionResponseDto(Long id, OffsetDateTime dateEmission, OffsetDateTime dateValeur, OffsetDateTime dateOperations, String observation, Double tvaDeductible, Double tvaCollectee, String libelle, TransactionSource transactionSource, BigDecimal debit, BigDecimal credit, String traces, Long banqueDePrelevementId, String banqueDePrelevementNom, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.dateEmission = dateEmission;
        this.dateValeur = dateValeur;
        this.dateOperations = dateOperations;
        this.observation = observation;
        this.tvaDeductible = tvaDeductible;
        this.tvaCollectee = tvaCollectee;
        this.libelle = libelle;
        this.transactionSource = transactionSource;
        this.debit = debit;
        this.credit = credit;
        this.traces = traces;
        this.banqueDePrelevementId = banqueDePrelevementId;
        this.banqueDePrelevementNom = banqueDePrelevementNom;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public OffsetDateTime getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(OffsetDateTime dateValeur) {
        this.dateValeur = dateValeur;
    }

    public OffsetDateTime getDateOperations() {
        return dateOperations;
    }

    public void setDateOperations(OffsetDateTime dateOperations) {
        this.dateOperations = dateOperations;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public Double getTvaDeductible() {
        return tvaDeductible;
    }

    public void setTvaDeductible(Double tvaDeductible) {
        this.tvaDeductible = tvaDeductible;
    }

    public Double getTvaCollectee() {
        return tvaCollectee;
    }

    public void setTvaCollectee(Double tvaCollectee) {
        this.tvaCollectee = tvaCollectee;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public TransactionSource getTransactionSource() {
        return transactionSource;
    }

    public void setTransactionSource(TransactionSource transactionSource) {
        this.transactionSource = transactionSource;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public String getTraces() {
        return traces;
    }

    public void setTraces(String traces) {
        this.traces = traces;
    }

    public Long getBanqueDePrelevementId() {
        return banqueDePrelevementId;
    }

    public void setBanqueDePrelevementId(Long banqueDePrelevementId) {
        this.banqueDePrelevementId = banqueDePrelevementId;
    }

    public String getBanqueDePrelevementNom() {
        return banqueDePrelevementNom;
    }

    public void setBanqueDePrelevementNom(String banqueDePrelevementNom) {
        this.banqueDePrelevementNom = banqueDePrelevementNom;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
