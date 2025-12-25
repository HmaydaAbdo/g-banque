package staport.finance.gBanque.rapprochement.dtos;


import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;


public class ReportDto {

    private Long id;
    private Long banqueId;
    private String banqueNom;
    private YearMonth dateRapprochement;

    private BigDecimal soldeDebut;
    private BigDecimal soldeFin;

    private BigDecimal totalDebit;
    private BigDecimal totalCredit;

    private BigDecimal tvaDeductible;
    private BigDecimal tvaCollectee;
    private BigDecimal tvaDue;

    private List<TransactionResponseDto> transactions;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ReportDto() {
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }
    public String getBanqueNom() {
        return banqueNom;
    }
    public void setBanqueNom(String banqueNom) {
        this.banqueNom = banqueNom;
    }
    public YearMonth getDateRapprochement() {
        return dateRapprochement;
    }
    public void setDateRapprochement(YearMonth dateRapprochement) {
        this.dateRapprochement = dateRapprochement;
    }

    public BigDecimal getSoldeDebut() {
        return soldeDebut;
    }

    public void setSoldeDebut(BigDecimal soldeDebut) {
        this.soldeDebut = soldeDebut;
    }

    public BigDecimal getSoldeFin() {
        return soldeFin;
    }

    public void setSoldeFin(BigDecimal soldeFin) {
        this.soldeFin = soldeFin;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }

    public BigDecimal getTvaDeductible() {
        return tvaDeductible;
    }

    public void setTvaDeductible(BigDecimal tvaDeductible) {
        this.tvaDeductible = tvaDeductible;
    }

    public BigDecimal getTvaCollectee() {
        return tvaCollectee;
    }

    public void setTvaCollectee(BigDecimal tvaCollectee) {
        this.tvaCollectee = tvaCollectee;
    }

    public BigDecimal getTvaDue() {
        return tvaDue;
    }

    public void setTvaDue(BigDecimal tvaDue) {
        this.tvaDue = tvaDue;
    }

    public List<TransactionResponseDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionResponseDto> transactions) {
        this.transactions = transactions;
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