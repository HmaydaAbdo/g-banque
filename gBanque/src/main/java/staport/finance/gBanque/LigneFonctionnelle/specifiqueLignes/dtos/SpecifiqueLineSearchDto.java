package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos;

public class SpecifiqueLineSearchDto {

    private Long banqueId;
    private Long marcheId;

    public SpecifiqueLineSearchDto() {
    }

    public SpecifiqueLineSearchDto(Long banqueId, Long marcheId) {
        this.banqueId = banqueId;
        this.marcheId = marcheId;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public Long getMarcheId() {
        return marcheId;
    }

    public void setMarcheId(Long marcheId) {
        this.marcheId = marcheId;
    }
}
