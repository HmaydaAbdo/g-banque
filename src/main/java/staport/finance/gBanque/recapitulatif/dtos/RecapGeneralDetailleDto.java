package staport.finance.gBanque.recapitulatif.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecapGeneralDetailleDto {
    private Long banqueId;
    private String banqueNom;
    private LocalDate dateRecap;             // aujourd'hui
    private ResumeSectionDto resume;         // aujourd'hui
    private DecaissementSectionDto decaissements;  // demain → fin mois
    private EncaissementSectionDto encaissements;  // demain → fin mois
    private BigDecimal soldePrevisionnelFinMois;   // soldeProjete - totalDécaissements + totalEncaissements

    public RecapGeneralDetailleDto() {
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getBanqueNom() {
        return banqueNom;
    }

    public void setBanqueNom(String banqueNom) {
        this.banqueNom = banqueNom;
    }

    public LocalDate getDateRecap() {
        return dateRecap;
    }

    public void setDateRecap(LocalDate dateRecap) {
        this.dateRecap = dateRecap;
    }

    public ResumeSectionDto getResume() {
        return resume;
    }

    public void setResume(ResumeSectionDto resume) {
        this.resume = resume;
    }

    public DecaissementSectionDto getDecaissements() {
        return decaissements;
    }

    public void setDecaissements(DecaissementSectionDto decaissements) {
        this.decaissements = decaissements;
    }

    public EncaissementSectionDto getEncaissements() {
        return encaissements;
    }

    public void setEncaissements(EncaissementSectionDto encaissements) {
        this.encaissements = encaissements;
    }

    public BigDecimal getSoldePrevisionnelFinMois() {
        return soldePrevisionnelFinMois;
    }

    public void setSoldePrevisionnelFinMois(BigDecimal soldePrevisionnelFinMois) {
        this.soldePrevisionnelFinMois = soldePrevisionnelFinMois;
    }

    @Override
    public String toString() {
        return "RecapGeneralDetailleDto{" +
                "banqueId=" + banqueId +
                ", banqueNom='" + banqueNom + '\'' +
                ", dateRecap=" + dateRecap +
                ", resume=" + resume +
                ", decaissements=" + decaissements +
                ", encaissements=" + encaissements +
                ", soldePrevisionnelFinMois=" + soldePrevisionnelFinMois +
                '}';
    }
}