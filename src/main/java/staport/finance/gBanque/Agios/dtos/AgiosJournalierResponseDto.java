package staport.finance.gBanque.Agios.dtos;

import staport.finance.gBanque.Agios.enums.TypeAgios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class AgiosJournalierResponseDto {

    private Long id;
    private TypeAgios typeAgios;
    private LocalDate dateValeur;
    private Long banqueId;
    private String banqueNom;
    private BigDecimal soldeJournalier;
    private BigDecimal baseCalcul;
    private BigDecimal tauxApplique;
    private BigDecimal agiosHT;
    private BigDecimal tva;
    private BigDecimal agiosTTC;
    private OffsetDateTime dateCalcul;
    private int nbRecalculs;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TypeAgios getTypeAgios() { return typeAgios; }
    public void setTypeAgios(TypeAgios typeAgios) { this.typeAgios = typeAgios; }
    public LocalDate getDateValeur() { return dateValeur; }
    public void setDateValeur(LocalDate dateValeur) { this.dateValeur = dateValeur; }
    public Long getBanqueId() { return banqueId; }
    public void setBanqueId(Long banqueId) { this.banqueId = banqueId; }
    public String getBanqueNom() { return banqueNom; }
    public void setBanqueNom(String banqueNom) { this.banqueNom = banqueNom; }
    public BigDecimal getSoldeJournalier() { return soldeJournalier; }
    public void setSoldeJournalier(BigDecimal soldeJournalier) { this.soldeJournalier = soldeJournalier; }
    public BigDecimal getBaseCalcul() { return baseCalcul; }
    public void setBaseCalcul(BigDecimal baseCalcul) { this.baseCalcul = baseCalcul; }
    public BigDecimal getTauxApplique() { return tauxApplique; }
    public void setTauxApplique(BigDecimal tauxApplique) { this.tauxApplique = tauxApplique; }
    public BigDecimal getAgiosHT() { return agiosHT; }
    public void setAgiosHT(BigDecimal agiosHT) { this.agiosHT = agiosHT; }
    public BigDecimal getTva() { return tva; }
    public void setTva(BigDecimal tva) { this.tva = tva; }
    public BigDecimal getAgiosTTC() { return agiosTTC; }
    public void setAgiosTTC(BigDecimal agiosTTC) { this.agiosTTC = agiosTTC; }
    public OffsetDateTime getDateCalcul() { return dateCalcul; }
    public void setDateCalcul(OffsetDateTime dateCalcul) { this.dateCalcul = dateCalcul; }
    public int getNbRecalculs() { return nbRecalculs; }
    public void setNbRecalculs(int nbRecalculs) { this.nbRecalculs = nbRecalculs; }
}