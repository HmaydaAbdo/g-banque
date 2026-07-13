package staport.finance.gBanque.moyensDePaiements.lcn.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Réponse agrégée du suivi des LCN pour une ligne fonctionnelle (nom de banque, total, page de
 * résultats). Déplacé depuis {@code droitConstate.dtos} (13/07/2026) — DTO du module LCN, mal
 * rangé dans un package sans rapport lors d'un ajout antérieur ; consommé par
 * {@code LigneFonctionnelleService}/{@code ILigneFonctionnelleService}/
 * {@code LigneFonctionnelleController} (suivi de ligne générale), aucun changement de comportement.
 */
public class AgregatedSuivieLcnLigneResponseDto {

    private String banqueName;
    private BigDecimal total;
    private Page<LcnResponseDTO> page;

    public AgregatedSuivieLcnLigneResponseDto() {
    }

    public String getBanqueName() {
        return banqueName;
    }

    public void setBanqueName(String banqueName) {
        this.banqueName = banqueName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Page<LcnResponseDTO> getPage() {
        return page;
    }

    public void setPage(Page<LcnResponseDTO> page) {
        this.page = page;
    }
}
