package staport.finance.gBanque.Bank.mappers;


import staport.finance.gBanque.Bank.dtos.HistoriqueSoldeBanqueDto;
import staport.finance.gBanque.Bank.entities.HistoriqueSoldeBanque;

public class HistoriqueSoldeBanqueMapper {

    public static HistoriqueSoldeBanqueDto toDto(HistoriqueSoldeBanque entity) {
        if (entity == null) {
            return null;
        }

        HistoriqueSoldeBanqueDto dto = new HistoriqueSoldeBanqueDto();
        dto.setId(entity.getId());
        dto.setBanqueId(entity.getBanque().getId());
        dto.setNomBanque(entity.getBanque().getNom());
        dto.setMois(entity.getMois());
        dto.setAnnee(entity.getAnnee());
        dto.setSoldeDepart(entity.getSoldeDepart());
        dto.setSoldeFin(entity.getSoldeFin());

        return dto;
    }
}