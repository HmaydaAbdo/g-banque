package staport.finance.gBanque.rapprochement.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.rapprochement.dtos.RapprochementAggregates;
import staport.finance.gBanque.rapprochement.dtos.RapprochementResponseDto;
import staport.finance.gBanque.rapprochement.dtos.ReportDto;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;

@Component
public class RapprochementMapper {

    /**
     * Maps a Rapprochement entity to its response DTO.
     * Aggregates (totalDebit, totalCredit, TVA) are computed on-the-fly and
     * passed in — never read from stored entity fields.
     */
    public RapprochementResponseDto toResponseDto(Rapprochement entity, RapprochementAggregates agg) {
        if (entity == null) return null;

        RapprochementResponseDto dto = new RapprochementResponseDto();

        dto.setId(entity.getId());
        dto.setBanqueId(entity.getBanque() != null ? entity.getBanque().getId() : null);
        dto.setBanqueNom(entity.getBanque() != null ? entity.getBanque().getNom() : null);
        dto.setDateRapprochement(entity.getDateRapprochement());
        dto.setSoldeDebut(entity.getSoldeDebut());
        dto.setStatutRapprochement(entity.getStatut());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Computed fields — derived from transactions, never stored on entity
        dto.setTotalDebit(agg.totalDebit());
        dto.setTotalCredit(agg.totalCredit());
        dto.setTvaDeductible(agg.tvaDeductible());
        dto.setTvaCollectee(agg.tvaCollectee());
        dto.setTvaDue(agg.tvaDue());
        dto.setSoldeFin(agg.soldeFin(entity.getSoldeDebut()));

        return dto;
    }

    /**
     * Maps a Rapprochement entity to a report DTO (used for PDF generation).
     * Same aggregate injection pattern as toResponseDto.
     */
    public ReportDto toReportDto(Rapprochement entity, RapprochementAggregates agg) {
        if (entity == null) return null;

        ReportDto dto = new ReportDto();

        dto.setId(entity.getId());
        dto.setBanqueId(entity.getBanque() != null ? entity.getBanque().getId() : null);
        dto.setBanqueNom(entity.getBanque() != null ? entity.getBanque().getNom() : null);
        dto.setDateRapprochement(entity.getDateRapprochement());
        dto.setSoldeDebut(entity.getSoldeDebut());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Computed fields
        dto.setTotalDebit(agg.totalDebit());
        dto.setTotalCredit(agg.totalCredit());
        dto.setTvaDeductible(agg.tvaDeductible());
        dto.setTvaCollectee(agg.tvaCollectee());
        dto.setTvaDue(agg.tvaDue());
        dto.setSoldeFin(agg.soldeFin(entity.getSoldeDebut()));

        return dto;
    }
}
