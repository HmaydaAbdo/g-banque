package staport.finance.gBanque.impayees.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.impayees.dtos.ImpayeeRequestDto;
import staport.finance.gBanque.impayees.dtos.ImpayeeResponseDto;
import staport.finance.gBanque.impayees.entities.Impayee;

@Component
public class ImpayeeMapper {

    private static final Logger log = LoggerFactory.getLogger(ImpayeeMapper.class);

    /**
     * Convertit un DTO de requête en entité Impayee
     */
    public Impayee toEntity(ImpayeeRequestDto dto, Banque banqueEncaissement) {

        if (dto == null) {
            log.warn("Tentative de conversion d'un ImpayeeRequestDto null en entité");
            return null;
        }

        if (banqueEncaissement == null) {
            log.error("Tentative de création d'une impayée avec une banque d'encaissement null '");
            throw new IllegalArgumentException("La banque d'encaissement ne peut pas être null lors de la création d'une impayée");
        }

        log.debug("Conversion du DTO vers entité Impayee - Libellé: '{}', Banque d'encaissement: '{}', Montant: {}",
                dto.getLibelle(), banqueEncaissement.getNom(), dto.getMontant());

        Impayee impayee = new Impayee();
        impayee.setLibelle(dto.getLibelle());
        impayee.setObservation(dto.getObservation());
        impayee.setMontant(dto.getMontant());
        impayee.setBanqueEncaissement(banqueEncaissement);
        impayee.setMotif(dto.getMotif());
        impayee.setDateEmission(dto.getDateEmission());

        // Note: Le statut est généralement géré par défaut dans l'entité ou la DB (ex: PREVU)
        log.debug("Entité Impayee créée avec succès pour la banque d'encaissement ID: {}", banqueEncaissement.getId());

        return impayee;
    }

    /**
     * Convertit une entité Impayee en DTO de réponse
     */
    public ImpayeeResponseDto toResponseDto(Impayee entity) {
        if (entity == null) {
            log.warn("Tentative de conversion d'une entité Impayee null en DTO");
            return null;
        }

        log.debug("Conversion de l'entité Impayee ID: {} vers DTO de réponse", entity.getId());

        ImpayeeResponseDto dto = new ImpayeeResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setObservation(entity.getObservation());
        dto.setMotif(entity.getMotif());
        dto.setMontant(entity.getMontant());
        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());

        if (entity.getBanqueEncaissement() != null) {
            dto.setBanqueEncaissementId(entity.getBanqueEncaissement().getId());
            dto.setBanqueEncaissementNom(entity.getBanqueEncaissement().getNom());
            log.trace("Banque d'encaissement associée à l'impayée: ID={}, Nom='{}'",
                    entity.getBanqueEncaissement().getId(), entity.getBanqueEncaissement().getNom());
        } else {
            log.warn("L'impayée ID: {} n'a pas de banque d'encaissement associée", entity.getId());
        }

        // Mapping des dates d'opération et valeur
        dto.setDateOperation(entity.getDateOperation());
        dto.setDateValeur(entity.getDateValeur());

        // Mapping des dates de création et modification
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());

        log.debug("DTO de réponse créé avec succès pour Impayee ID: {}, Statut: {}",
                entity.getId(), entity.getStatus());

        return dto;
    }
}