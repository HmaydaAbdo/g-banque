package staport.finance.gBanque.Agios.dtos;

import staport.finance.gBanque.Agios.enums.StatutTrimestre;
import staport.finance.gBanque.Agios.enums.TypeAgios;

public class AgiosTrimestrielSearchDto {

    private Long banqueId;
    private TypeAgios type;
    private Integer annee;
    private Integer trimestre;
    private StatutTrimestre statut;
    private String search;

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public StatutTrimestre getStatut() {
        return statut;
    }

    public void setStatut(StatutTrimestre statut) {
        this.statut = statut;
    }

    public Integer getTrimestre() {
        return trimestre;
    }

    public void setTrimestre(Integer trimestre) {
        this.trimestre = trimestre;
    }

    public TypeAgios getType() {
        return type;
    }

    public void setType(TypeAgios type) {
        this.type = type;
    }

    public Integer getAnnee() {
        return annee;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    @Override
    public String toString() {
        return "AgiosTrimestrielSearchDto{" +
                "banqueId=" + banqueId +
                ", type=" + type +
                ", annee=" + annee +
                ", trimestre=" + trimestre +
                ", statut=" + statut +
                ", search='" + search + '\'' +
                '}';
    }
}