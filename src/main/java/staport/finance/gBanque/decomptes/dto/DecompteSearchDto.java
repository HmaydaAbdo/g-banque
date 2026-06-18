package staport.finance.gBanque.decomptes.dto;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DecompteSearchDto {

    private Long marcheId;
    private Long banqueId;
    private Status status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateValeurFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateValeurTo;

    private BigDecimal montantMin;
    private BigDecimal montantMax;

    public DecompteSearchDto() {}

    public Long getMarcheId() { return marcheId; }
    public void setMarcheId(Long marcheId) { this.marcheId = marcheId; }

    public Long getBanqueId() { return banqueId; }
    public void setBanqueId(Long banqueId) { this.banqueId = banqueId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDate getDateValeurFrom() { return dateValeurFrom; }
    public void setDateValeurFrom(LocalDate dateValeurFrom) { this.dateValeurFrom = dateValeurFrom; }

    public LocalDate getDateValeurTo() { return dateValeurTo; }
    public void setDateValeurTo(LocalDate dateValeurTo) { this.dateValeurTo = dateValeurTo; }

    public BigDecimal getMontantMin() { return montantMin; }
    public void setMontantMin(BigDecimal montantMin) { this.montantMin = montantMin; }

    public BigDecimal getMontantMax() { return montantMax; }
    public void setMontantMax(BigDecimal montantMax) { this.montantMax = montantMax; }

    @Override
    public String toString() {
        return "DecompteSearchDto{marcheId=" + marcheId + ", banqueId=" + banqueId +
                ", status=" + status + ", dateValeurFrom=" + dateValeurFrom +
                ", dateValeurTo=" + dateValeurTo + ", montantMin=" + montantMin +
                ", montantMax=" + montantMax + '}';
    }
}
