package staport.finance.gBanque.decomptes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class DecompteRequestDto {

    @NotNull(message = "Marche ID cannot be null")
    private Long marcheId;

    @NotNull(message = "Date of emission cannot be null")
    private OffsetDateTime dateEmission;

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


    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
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

    @Override
    public String toString() {
        return "DecompteRequestDto{" +
                "marcheId=" + marcheId +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", libelle='" + libelle + '\'' +
                '}';
    }
}
