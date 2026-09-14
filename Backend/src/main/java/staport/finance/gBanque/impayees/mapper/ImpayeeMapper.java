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
    public Impayee toEntity(ImpayeeRequestDto dto, Banque banque) {

        if (dto == null) {
            log.warn("Tentative de conversion d'un ImpayeeRequestDto null en entité");
            return null;
        }

        if (banque == null) {
            log.error("Tentative de création d'une impayée avec une banque d'encaissement null '");
            throw new IllegalArgumentException("La banque d'encaissement ne peut pas être null lors de la création d'une impayée");
        }

        log.debug("Conversion du DTO vers entité Impayee - Libellé: '{}', Banque d'encaissement: '{}', Montant: {}",
                dto.libelle(), banque.getNom(), dto.montant());

        Impayee impayee = new Impayee();
        impayee.setLibelle(dto.libelle());
        impayee.setType(dto.type());
        impayee.setObservation(dto.observation());
        impayee.setMontant(dto.montant());
        impayee.setBanque(banque);
        impayee.setMotif(dto.motif());
        impayee.setNumero(dto.numero());
        impayee.setDateEmission(dto.dateEmission());

        // Note: Le statut est généralement géré par défaut dans l'entité ou la DB (ex: PREVU)
        log.debug("Entité Impayee créée avec succès pour la banque d'encaissement ID: {}", banque.getId());

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

        Long banqueId = null;
        String banqueNom = null;
        if (entity.getBanque() != null) {
            banqueId = entity.getBanque().getId();
            banqueNom = entity.getBanque().getNom();
            log.trace("Banque d'encaissement associée à l'impayée: ID={}, Nom='{}'",
                    entity.getBanque().getId(), entity.getBanque().getNom());
        } else {
            log.warn("L'impayée ID: {} n'a pas de banque d'encaissement associée", entity.getId());
        }

        ImpayeeResponseDto dto = new ImpayeeResponseDto(
                entity.getId(),
                entity.getLibelle(),
                entity.getObservation(),
                entity.getMontant(),
                banqueId,
                banqueNom,
                entity.getType(),
                entity.getMotif(),
                entity.getNumero(),
                entity.getDateEmission(),
                entity.getDateValeur(),
                entity.getDateOperation(),
                entity.getStatus(),
                entity.getDateCreation(),
                entity.getDateModification()
        );

        log.debug("DTO de réponse créé avec succès pour Impayee ID: {}, Statut: {}",
                entity.getId(), entity.getStatus());

        return dto;
    }
}
