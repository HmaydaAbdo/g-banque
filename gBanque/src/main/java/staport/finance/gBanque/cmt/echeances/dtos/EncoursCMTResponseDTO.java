package staport.finance.gBanque.cmt.echeances.dtos;


import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * DTO de réponse pour l'affichage des encours d'échéances de crédit à moyen terme.
 */
public class EncoursCMTResponseDTO {

    private Long banqueId;
    private String nomBanque;
    private BigDecimal totalEncours;
    private long nombreEcheancesEnAttente;
    private long nombreCreditsConcernes;
    private Page<EcheanceCMTResponseDto> echeances;

    // Constructeurs, Getters et Setters

    public EncoursCMTResponseDTO() {
    }

    public EncoursCMTResponseDTO(Long banqueId, String nomBanque, BigDecimal totalEncours, long nombreEcheancesEnAttente, long nombreCreditsConcernes, Page<EcheanceCMTResponseDto> echeances) {
        this.banqueId = banqueId;
        this.nomBanque = nomBanque;
        this.totalEncours = totalEncours;
        this.nombreEcheancesEnAttente = nombreEcheancesEnAttente;
        this.nombreCreditsConcernes = nombreCreditsConcernes;
        this.echeances = echeances;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public BigDecimal getTotalEncours() {
        return totalEncours;
    }

    public void setTotalEncours(BigDecimal totalEncours) {
        this.totalEncours = totalEncours;
    }

    public long getNombreEcheancesEnAttente() {
        return nombreEcheancesEnAttente;
    }

    public void setNombreEcheancesEnAttente(long nombreEcheancesEnAttente) {
        this.nombreEcheancesEnAttente = nombreEcheancesEnAttente;
    }

    public long getNombreCreditsConcernes() {
        return nombreCreditsConcernes;
    }

    public void setNombreCreditsConcernes(long nombreCreditsConcernes) {
        this.nombreCreditsConcernes = nombreCreditsConcernes;
    }

    public Page<EcheanceCMTResponseDto> getEcheances() {
        return echeances;
    }

    public void setEcheances(Page<EcheanceCMTResponseDto> echeances) {
        this.echeances = echeances;
    }
}