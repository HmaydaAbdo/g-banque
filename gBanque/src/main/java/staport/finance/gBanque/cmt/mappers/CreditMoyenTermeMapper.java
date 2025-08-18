package staport.finance.gBanque.cmt.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeRequestDto;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeResponseDto;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Composant Spring pour le mappage manuel entre l'entité CreditMoyenTerme et ses DTOs.
 * Cette classe a pour seule responsabilité de convertir des objets, sans interagir
 * avec la logique métier ou la couche de persistance.
 */
@Component
public class CreditMoyenTermeMapper {

    /**
     * Convertit un DTO de requête en une nouvelle entité CreditMoyenTerme.
     * Cette méthode reçoit l'entité Banque déjà chargée par le service,
     * respectant ainsi le principe de la responsabilité unique.
     *
     * @param requestDto Le DTO contenant les données du crédit.
     * @param banque L'entité Banque associée (déjà récupérée par le service).
     * @return Une nouvelle instance de CreditMoyenTerme.
     */
    public CreditMoyenTerme toEntity(CreditMoyenTermeRequestDto requestDto, Banque banque) {
        if (requestDto == null) {
            return null;
        }

        CreditMoyenTerme entity = new CreditMoyenTerme();
        entity.setBanque(banque);
        entity.setDesignation(requestDto.getDesignation());
        entity.setNominal(requestDto.getNominal());
        entity.setPeriodicite(requestDto.getPeriodicite());
        entity.setDuree(requestDto.getDuree());
        entity.setEcheance(requestDto.getEcheance());
        entity.setTaux(requestDto.getTaux());
        entity.setDateDebut(requestDto.getDateDebut());

        return entity;
    }

    /**
     * Met à jour une entité existante à partir d'un DTO de requête et d'une entité Banque.
     * La logique de recherche de la banque et de l'entité existante est gérée par le service.
     *
     * @param requestDto Le DTO de mise à jour.
     * @param entity L'entité CreditMoyenTerme existante à modifier.
     * @param banque La nouvelle entité Banque associée (récupérée par le service).
     * @return L'entité mise à jour.
     */
    public CreditMoyenTerme updateEntity(CreditMoyenTermeRequestDto requestDto, CreditMoyenTerme entity, Banque banque) {
        if (requestDto == null || entity == null) {
            return entity;
        }

        entity.setBanque(banque);
        entity.setDesignation(requestDto.getDesignation());
        entity.setNominal(requestDto.getNominal());
        entity.setPeriodicite(requestDto.getPeriodicite());
        entity.setDuree(requestDto.getDuree());
        entity.setEcheance(requestDto.getEcheance());
        entity.setTaux(requestDto.getTaux());
        entity.setDateDebut(requestDto.getDateDebut());

        return entity;
    }

    /**
     * Convertit une entité CreditMoyenTerme en un DTO de réponse.
     *
     * @param entity L'entité CreditMoyenTerme à convertir.
     * @return Un DTO de réponse contenant les informations du crédit.
     */
    public CreditMoyenTermeResponseDto toResponseDto(CreditMoyenTerme entity) {
        if (entity == null) {
            return null;
        }

        CreditMoyenTermeResponseDto responseDto = new CreditMoyenTermeResponseDto();
        responseDto.setId(entity.getId());
        responseDto.setDesignation(entity.getDesignation());
        responseDto.setNominal(entity.getNominal());
        responseDto.setPeriodicite(entity.getPeriodicite());
        responseDto.setDuree(entity.getDuree());
        responseDto.setEcheance(entity.getEcheance());
        responseDto.setTaux(entity.getTaux());
        responseDto.setDateDebut(entity.getDateDebut());
        responseDto.setDateCreation(entity.getDateCreation());
        responseDto.setDateModification(entity.getDateModification());
        // Ajout de la nouvelle ligne de mappage pour le champ 'completed'
        responseDto.setCompleted(entity.isCompleted());

        // Mappage de l'entité Banque vers son DTO imbriqué
        if (entity.getBanque() != null) {
            responseDto.setBanque(toBanqueDto(entity.getBanque()));
        }

        return responseDto;
    }

    /**
     * Convertit une entité Banque en son DTO imbriqué.
     * @param banque L'entité Banque à convertir.
     * @return Le DTO de la Banque.
     */
    private CreditMoyenTermeResponseDto.BanqueDto toBanqueDto(Banque banque) {
        if (banque == null) {
            return null;
        }
        CreditMoyenTermeResponseDto.BanqueDto banqueDto = new CreditMoyenTermeResponseDto.BanqueDto();
        banqueDto.setId(banque.getId());
        banqueDto.setNom(banque.getNom());
        return banqueDto;
    }

    /**
     * Convertit une liste d'entités CreditMoyenTerme en une liste de DTOs de réponse.
     *
     * @param entities La liste d'entités à convertir.
     * @return La liste correspondante de DTOs de réponse.
     */
    public List<CreditMoyenTermeResponseDto> toResponseDtoList(List<CreditMoyenTerme> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }
}