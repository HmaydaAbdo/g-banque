package staport.finance.gBanque.rapprochement.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.rapprochement.dtos.RapprochementResponseDto;
import staport.finance.gBanque.rapprochement.dtos.ReportDto;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import java.math.BigDecimal;

@Component
public class RapprochementMapper {


    public RapprochementResponseDto toResponseDto(Rapprochement entity) {
        if (entity == null) {
            return null;
        }

        RapprochementResponseDto dto = new RapprochementResponseDto();

        dto.setId(entity.getId());

        // Mappage de l'ID de la Banque
        dto.setBanqueId(entity.getBanque() != null ? entity.getBanque().getId() : null);
        dto.setBanqueNom(entity.getBanque() != null ? entity.getBanque().getNom() : null);
        // Mappage des champs principaux
        dto.setDateRapprochement(entity.getDateRapprochement());
        dto.setSoldeDebut(entity.getSoldeDebut());
        dto.setSoldeFin(entity.getSoldeFin());
        dto.setTotalDebit(entity.getTotalDebit());
        dto.setTotalCredit(entity.getTotalCredit());

        // Mappage des TVAs
        BigDecimal tvaCollectee = entity.getTvaCollectee() != null ? entity.getTvaCollectee() : BigDecimal.ZERO;
        BigDecimal tvaDeductible = entity.getTvaDeductible() != null ? entity.getTvaDeductible() : BigDecimal.ZERO;

        dto.setTvaDeductible(tvaDeductible);
        dto.setTvaCollectee(tvaCollectee);

        // Calcul de la TVA Due (à la différence des deux montants)
        dto.setTvaDue(tvaCollectee.subtract(tvaDeductible));

        // Mappage des champs d'audit
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Le champ transactions (Page<TransactionResponseDto>) sera défini
        // explicitement dans le RapprochementService (méthode getRapprochementById).

        return dto;
    }

    public ReportDto toReportDto(Rapprochement entity) {
        if (entity == null) {
            return null;
        }

        ReportDto dto = new ReportDto();

        dto.setId(entity.getId());

        // Mappage de l'ID et Nom de la Banque
        dto.setBanqueId(entity.getBanque() != null ? entity.getBanque().getId() : null);
        dto.setBanqueNom(entity.getBanque() != null ? entity.getBanque().getNom() : null);

        // Mappage des champs principaux
        dto.setDateRapprochement(entity.getDateRapprochement());
        dto.setSoldeDebut(entity.getSoldeDebut());
        dto.setSoldeFin(entity.getSoldeFin());
        dto.setTotalDebit(entity.getTotalDebit());
        dto.setTotalCredit(entity.getTotalCredit());

        // Mappage des TVAs
        BigDecimal tvaCollectee = entity.getTvaCollectee() != null ? entity.getTvaCollectee() : BigDecimal.ZERO;
        BigDecimal tvaDeductible = entity.getTvaDeductible() != null ? entity.getTvaDeductible() : BigDecimal.ZERO;

        dto.setTvaDeductible(tvaDeductible);
        dto.setTvaCollectee(tvaCollectee);

        // Calcul de la TVA Due
        dto.setTvaDue(tvaCollectee.subtract(tvaDeductible));

        // Mappage des champs d'audit
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // La liste des transactions sera ajoutée par le service.

        return dto;
    }
}