package staport.finance.gBanque.droitConstate.dtos;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating or updating a DroitConstate entity.
 * This DTO is used to receive data from the client, preventing exposure of the internal entity.
 */
public class DroitConstateRequestDto {

    @NotNull(message = "Marche ID cannot be null")
    private Long marcheId;

    /**
     * Identifiant de la banque liée à ce droit constaté. Obligatoire.
     * Pré-remplie depuis marche.banque dans l'UI, mais modifiable.
     * - DEBLOCAGE     → banque d'encaissement (reçoit les fonds du Trésor)
     * - REMBOURSEMENT → banque de décaissement (effectue le remboursement)
     */
    @NotNull(message = "La banque est obligatoire")
    private Long banqueId;

    @NotNull(message = "Date of emission cannot be null")
    private LocalDate dateEmission ;

    @NotNull(message = "Montant cannot be null")
    @DecimalMin(value = "0.01", message = "Montant must be a positive value")
    private BigDecimal montant;

    @NotNull(message = "Type cannot be null")
    private Type type;

    public DroitConstateRequestDto() {
    }

    public DroitConstateRequestDto(Long marcheId, Long banqueId, LocalDate dateEmission, BigDecimal montant, Type type) {
        this.marcheId = marcheId;
        this.banqueId = banqueId;
        this.dateEmission = dateEmission;
        this.montant = montant;
        this.type = type;
    }


    public Long getMarcheId() {
        return marcheId;
    }

    public void setMarcheId(Long marcheId) {
        this.marcheId = marcheId;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }


    @Override
    public String toString() {
        return "DroitConstateRequestDto{" +
                "marcheId=" + marcheId +
                ", banqueId=" + banqueId +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", type=" + type +
                '}';
    }
}
