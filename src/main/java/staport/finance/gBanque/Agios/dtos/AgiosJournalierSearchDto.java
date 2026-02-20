package staport.finance.gBanque.Agios.dtos;

import staport.finance.gBanque.Agios.enums.TypeAgios;

public class AgiosJournalierSearchDto {

    private Long banqueId;
    private TypeAgios type;
    private Integer trimestre;
    private Integer annee;

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public TypeAgios getType() {
        return type;
    }

    public void setType(TypeAgios type) {
        this.type = type;
    }

    public Integer getTrimestre() {
        return trimestre;
    }

    public void setTrimestre(Integer trimestre) {
        this.trimestre = trimestre;
    }

    public Integer getAnnee() {
        return annee;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }

    @Override
    public String toString() {
        return "AgiosJournalierSearchDto{" +
                "banqueId=" + banqueId +
                ", type=" + type +
                ", trimestre=" + trimestre +
                ", annee=" + annee +
                '}';
    }
}