package staport.finance.gBanque.leasing.contratLeasing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ContratLeasingPhaseResponseDTO {

    private Long id;
    private Integer numeroPhase;
    private Integer dureeMois;
    private BigDecimal mensualiteTTC;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateModification;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getNumeroPhase() { return numeroPhase; }
    public void setNumeroPhase(Integer numeroPhase) { this.numeroPhase = numeroPhase; }
    public Integer getDureeMois() { return dureeMois; }
    public void setDureeMois(Integer dureeMois) { this.dureeMois = dureeMois; }
    public BigDecimal getMensualiteTTC() { return mensualiteTTC; }
    public void setMensualiteTTC(BigDecimal mensualiteTTC) { this.mensualiteTTC = mensualiteTTC; }
    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }
    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }
}