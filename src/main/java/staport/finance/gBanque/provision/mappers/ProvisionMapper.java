package staport.finance.gBanque.provision.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.provision.dtos.ProvisionRequestDto;
import staport.finance.gBanque.provision.dtos.ProvisionResponseDto;
import staport.finance.gBanque.provision.entities.Provision;

@Component
public class ProvisionMapper {

    private static final Logger log = LoggerFactory.getLogger(ProvisionMapper.class);

    /**
     * Convertit un DTO de requête en entité Provision.
     */
    public Provision toEntity(ProvisionRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un ProvisionRequestDto null en entité");
            return null;
        }

        if (banque == null) {
            log.error("Tentative de création d'une provision avec une banque null pour le libellé: '{}'",
                    dto.libelle());
            throw new IllegalArgumentException("La banque ne peut pas être null lors de la création d'une provision");
        }

        log.debug("Conversion du DTO vers entité Provision - Libellé: '{}', Banque: '{}', Montant: {}, Motif: {}, Sens: {}",
                dto.libelle(), banque.getNom(), dto.montant(), dto.motif(), dto.sens());

        Provision provision = new Provision();
        provision.setLibelle(dto.libelle());
        provision.setObservation(dto.observation());
        provision.setMontant(dto.montant());
        provision.setBanque(banque);
        provision.setDateEmission(dto.dateEmission());
        provision.setMotif(dto.motif());
        provision.setSens(dto.sens());

        log.debug("Entité Provision créée avec succès pour la banque ID: {} (statut par défaut: PREVU, motif: {}, sens: {})",
                banque.getId(), dto.motif(), dto.sens());
        return provision;
    }

    /**
     * Convertit une entité Provision en DTO de réponse.
     */
    public ProvisionResponseDto toResponseDto(Provision entity) {
        if (entity == null) {
            log.warn("Tentative de conversion d'une entité Provision null en DTO");
            return null;
        }

        Long banqueId = null;
        String banqueNom = null;
        if (entity.getBanque() != null) {
            banqueId = entity.getBanque().getId();
            banqueNom = entity.getBanque().getNom();
        } else {
            log.warn("Provision ID: {} n'a pas de banque associée", entity.getId());
        }

        return new ProvisionResponseDto(
                entity.getId(),
                entity.getLibelle(),
                entity.getObservation(),
                entity.getMontant(),
                banqueId,
                banqueNom,
                entity.getDateEmission(),
                entity.getDateValeur(),
                entity.getDateOperation(),
                entity.getStatus(),
                entity.getMotif(),
                entity.getSens()
        );
    }
}
