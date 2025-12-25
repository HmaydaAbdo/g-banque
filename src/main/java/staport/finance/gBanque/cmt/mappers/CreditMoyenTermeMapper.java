package staport.finance.gBanque.cmt.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CreditMoyenTermeMapper.class);

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
        log.debug("Début du mappage RequestDto vers Entity");

        if (requestDto == null) {
            log.warn("Tentative de mappage avec un RequestDto null");
            return null;
        }

        log.debug("Création d'une nouvelle entité CreditMoyenTerme pour la banque ID: {}",
                banque != null ? banque.getId() : "null");

        CreditMoyenTerme entity = new CreditMoyenTerme();
        entity.setBanque(banque);
        entity.setDesignation(requestDto.getDesignation());
        entity.setNominal(requestDto.getNominal());
        entity.setPeriodicite(requestDto.getPeriodicite());
        entity.setDuree(requestDto.getDuree());
        entity.setEcheance(requestDto.getEcheance());
        entity.setTaux(requestDto.getTaux());
        entity.setDateDebut(requestDto.getDateDebut());

        log.debug("Entité CreditMoyenTerme créée avec succès - Désignation: {}", requestDto.getDesignation());
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
        log.debug("Début de la mise à jour de l'entité CreditMoyenTerme");

        if (requestDto == null || entity == null) {
            log.warn("Tentative de mise à jour avec RequestDto ou Entity null - RequestDto: {}, Entity: {}",
                    requestDto != null, entity != null);
            return entity;
        }

        log.debug("Mise à jour de l'entité ID: {} avec les nouvelles données", entity.getId());

        entity.setBanque(banque);
        entity.setDesignation(requestDto.getDesignation());
        entity.setNominal(requestDto.getNominal());
        entity.setPeriodicite(requestDto.getPeriodicite());
        entity.setDuree(requestDto.getDuree());
        entity.setEcheance(requestDto.getEcheance());
        entity.setTaux(requestDto.getTaux());
        entity.setDateDebut(requestDto.getDateDebut());

        log.debug("Entité CreditMoyenTerme mise à jour avec succès - ID: {}, Désignation: {}",
                entity.getId(), requestDto.getDesignation());
        return entity;
    }

    /**
     * Convertit une entité CreditMoyenTerme en un DTO de réponse.
     *
     * @param entity L'entité CreditMoyenTerme à convertir.
     * @return Un DTO de réponse contenant les informations du crédit.
     */
    public CreditMoyenTermeResponseDto toResponseDto(CreditMoyenTerme entity) {
        log.debug("Début du mappage Entity vers ResponseDto");

        if (entity == null) {
            log.warn("Tentative de mappage avec une Entity null");
            return null;
        }

        log.debug("Conversion de l'entité CreditMoyenTerme ID: {} vers ResponseDto", entity.getId());

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
            log.debug("Mappage de la Banque associée - ID: {}", entity.getBanque().getId());
            responseDto.setBanque(toBanqueDto(entity.getBanque()));
        } else {
            log.debug("Aucune banque associée à l'entité CreditMoyenTerme ID: {}", entity.getId());
        }

        log.debug("ResponseDto créé avec succès pour l'entité ID: {}", entity.getId());
        return responseDto;
    }

    /**
     * Convertit une entité Banque en son DTO imbriqué.
     * @param banque L'entité Banque à convertir.
     * @return Le DTO de la Banque.
     */
    private CreditMoyenTermeResponseDto.BanqueDto toBanqueDto(Banque banque) {
        log.trace("Conversion de l'entité Banque vers BanqueDto");

        if (banque == null) {
            log.warn("Tentative de conversion d'une Banque null vers BanqueDto");
            return null;
        }

        log.trace("Création du BanqueDto pour la banque ID: {}, Nom: {}", banque.getId(), banque.getNom());

        CreditMoyenTermeResponseDto.BanqueDto banqueDto = new CreditMoyenTermeResponseDto.BanqueDto();
        banqueDto.setId(banque.getId());
        banqueDto.setNom(banque.getNom());

        log.trace("BanqueDto créé avec succès");
        return banqueDto;
    }


}