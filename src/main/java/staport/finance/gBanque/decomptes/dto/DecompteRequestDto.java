package staport.finance.gBanque.decomptes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DecompteRequestDto {

    @NotNull(message = "Marche ID cannot be null")
    private Long marcheId;

    @NotNull(message = "banqueEncaissementId  cannot be null")
    private Long banqueEncaissementId;

    @NotNull(message = "Date of emission cannot be null")
    private LocalDate dateEmission ;

    @NotNull(message = "Montant cannot be null")
    @DecimalMin(value = "0.01", message = "Montant must be a positive value")
    private BigDecimal montant;

    @NotNull(message = "Libelle cannot be null")
    @NotBlank(message = "Libelle cannot be empty")
    private String libelle;

    public DecompteRequestDto() {
    }


    public Long getMarcheId() {
        return marcheId;
    }

    public void setMarcheId(Long marcheId) {
        this.marcheId = marcheId;
    }


    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Long getBanqueEncaissementId() {
        return banqueEncaissementId;
    }

    public void setBanqueEncaissementId(Long banqueEncaissementId) {
        this.banqueEncaissementId = banqueEncaissementId;
    }

    @Override
    public String toString() {
        return "DecompteRequestDto{" +
                "marcheId=" + marcheId +
                ", banqueEncaissementId=" + banqueEncaissementId +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", libelle='" + libelle + '\'' +
                '}';
    }
}
