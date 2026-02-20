package staport.finance.gBanque.Agios.dtos;


import staport.finance.gBanque.Agios.enums.StatutTrimestre;
import staport.finance.gBanque.Agios.enums.TypeAgios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class AgiosTrimestrielResponseDto {

    private Long id;
    private TypeAgios typeAgios;
    private int trimestre;
    private int annee;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Long banqueId;
    private String banqueNom;
    private BigDecimal totalAgiosHT;
    private BigDecimal totalTVA;
    private BigDecimal totalAgiosTTC;
    private int nbJoursCalcules;
    private int nbJoursTotal;
    private StatutTrimestre statut;
    private OffsetDateTime dateDernierCalcul;
    private OffsetDateTime dateCloture;
    private Long agiosDefinitifId;
    private List<AgiosJournalierResponseDto> detailJournalier;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TypeAgios getTypeAgios() { return typeAgios; }
    public void setTypeAgios(TypeAgios typeAgios) { this.typeAgios = typeAgios; }
    public int getTrimestre() { return trimestre; }
    public void setTrimestre(int trimestre) { this.trimestre = trimestre; }
    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public Long getBanqueId() { return banqueId; }
    public void setBanqueId(Long banqueId) { this.banqueId = banqueId; }
    public String getBanqueNom() { return banqueNom; }
    public void setBanqueNom(String banqueNom) { this.banqueNom = banqueNom; }
    public BigDecimal getTotalAgiosHT() { return totalAgiosHT; }
    public void setTotalAgiosHT(BigDecimal totalAgiosHT) { this.totalAgiosHT = totalAgiosHT; }
    public BigDecimal getTotalTVA() { return totalTVA; }
    public void setTotalTVA(BigDecimal totalTVA) { this.totalTVA = totalTVA; }
    public BigDecimal getTotalAgiosTTC() { return totalAgiosTTC; }
    public void setTotalAgiosTTC(BigDecimal totalAgiosTTC) { this.totalAgiosTTC = totalAgiosTTC; }
    public int getNbJoursCalcules() { return nbJoursCalcules; }
    public void setNbJoursCalcules(int nbJoursCalcules) { this.nbJoursCalcules = nbJoursCalcules; }
    public int getNbJoursTotal() { return nbJoursTotal; }
    public void setNbJoursTotal(int nbJoursTotal) { this.nbJoursTotal = nbJoursTotal; }
    public StatutTrimestre getStatut() { return statut; }
    public void setStatut(StatutTrimestre statut) { this.statut = statut; }
    public OffsetDateTime getDateDernierCalcul() { return dateDernierCalcul; }
    public void setDateDernierCalcul(OffsetDateTime dateDernierCalcul) { this.dateDernierCalcul = dateDernierCalcul; }
    public OffsetDateTime getDateCloture() { return dateCloture; }
    public void setDateCloture(OffsetDateTime dateCloture) { this.dateCloture = dateCloture; }
    public Long getAgiosDefinitifId() { return agiosDefinitifId; }
    public void setAgiosDefinitifId(Long agiosDefinitifId) { this.agiosDefinitifId = agiosDefinitifId; }
    public List<AgiosJournalierResponseDto> getDetailJournalier() { return detailJournalier; }
    public void setDetailJournalier(List<AgiosJournalierResponseDto> detailJournalier) { this.detailJournalier = detailJournalier; }
}