package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * DTO de réponse pour l'affichage des encours d'échéances de leasing.
 * Fournit à la fois un résumé financier et la liste détaillée des échéances concernées,
 * avec support de la pagination et du tri.
 */
public class EncoursLeasingResponseDTO {

    private Long banqueId;
    private String nomBanque;
    private BigDecimal totalEncours;
    private long nombreEcheancesEnAttente;
    private long nombreContratsConcernes;
    private Page<EcheanceLeasingResponseDTO> echeances;

    // --- Constructeurs, Getters et Setters ---

    public EncoursLeasingResponseDTO() {}

    public EncoursLeasingResponseDTO(Long banqueId, String nomBanque, BigDecimal totalEncours, long nombreEcheancesEnAttente, long nombreContratsConcernes, Page<EcheanceLeasingResponseDTO> echeances) {
        this.banqueId = banqueId;
        this.nomBanque = nomBanque;
        this.totalEncours = totalEncours;
        this.nombreEcheancesEnAttente = nombreEcheancesEnAttente;
        this.nombreContratsConcernes = nombreContratsConcernes;
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

    public long getNombreContratsConcernes() {
        return nombreContratsConcernes;
    }

    public void setNombreContratsConcernes(long nombreContratsConcernes) {
        this.nombreContratsConcernes = nombreContratsConcernes;
    }

    public Page<EcheanceLeasingResponseDTO> getEcheances() {
        return echeances;
    }

    public void setEcheances(Page<EcheanceLeasingResponseDTO> echeances) {
        this.echeances = echeances;
    }
}