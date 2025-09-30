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

    private Page<EcheanceCMTResponseDto> echeances;

    // Constructeurs, Getters et Setters

    public EncoursCMTResponseDTO() {
    }

    public EncoursCMTResponseDTO(Long banqueId, String nomBanque, BigDecimal totalEncours , Page<EcheanceCMTResponseDto> echeances) {
        this.banqueId = banqueId;
        this.nomBanque = nomBanque;
        this.totalEncours = totalEncours;
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

    public Page<EcheanceCMTResponseDto> getEcheances() {
        return echeances;
    }

    public void setEcheances(Page<EcheanceCMTResponseDto> echeances) {
        this.echeances = echeances;
    }
}