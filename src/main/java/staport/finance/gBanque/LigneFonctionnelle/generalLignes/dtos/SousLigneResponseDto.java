package staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos;

import java.math.BigDecimal;

public class SousLigneResponseDto {

    private Long id;

    private BigDecimal plafond;
    private Long natureLigneFonctionnelleId;
    private String natureLigneFonctionnelleLabel;
    private boolean actif;

    // --- Constructors ---
    public SousLigneResponseDto() {}



    // --- Getters and Setters (uniquement pour les champs conservés) ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }

    public Long getNatureLigneFonctionnelleId() { return natureLigneFonctionnelleId; }
    public void setNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId) { this.natureLigneFonctionnelleId = natureLigneFonctionnelleId; }

    public String getNatureLigneFonctionnelleLabel() { return natureLigneFonctionnelleLabel; }
    public void setNatureLigneFonctionnelleLabel(String natureLigneFonctionnelleLabel) { this.natureLigneFonctionnelleLabel = natureLigneFonctionnelleLabel; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }


}