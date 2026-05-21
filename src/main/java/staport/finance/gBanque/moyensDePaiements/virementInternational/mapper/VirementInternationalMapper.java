package staport.finance.gBanque.moyensDePaiements.virementInternational.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.virementInternational.dtos.VirementInternationalRequestDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.dtos.VirementInternationalResponseDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

@Component
public class VirementInternationalMapper {

    private static final Logger log = LoggerFactory.getLogger(VirementInternationalMapper.class);
    private final BanqueRepository banqueRepository;

    public VirementInternationalMapper(BanqueRepository banqueRepository) {
        this.banqueRepository = banqueRepository;
    }

    /**
     * Converts a VirementInternational entity to a VirementInternationalResponseDto.
     *
     * @param virement The VirementInternational entity to convert.
     * @return The corresponding VirementInternationalResponseDto.
     */
    public VirementInternationalResponseDto toDto(VirementInternational virement) {
        if (virement == null) {
            log.warn("Tentative de conversion d'un virement international null en DTO");
            return null;
        }

        log.debug("Conversion du virement international ID: {} en DTO", virement.getId());

        VirementInternationalResponseDto response = new VirementInternationalResponseDto();
        response.setId(virement.getId());
        response.setBanqueId(virement.getBanque().getId());
        response.setBanqueNom(virement.getBanque().getNom());
        response.setBeneficiaire(virement.getBeneficiaire());
        response.setEmetteur(virement.getEmetteur());
        response.setMontant(virement.getMontant());
        response.setDateEmission(virement.getDateEmission());
        response.setDateOperation(virement.getDateOperation());
        response.setDateValeur(virement.getDateValeur());
        response.setLibelle(virement.getLibelle());
        response.setObservation(virement.getObservation());
        response.setStatut(virement.getStatut());
        response.setType(virement.getType());

        log.debug("Conversion réussie du virement international ID: {} (Type: {}, Montant: {})",
                virement.getId(), virement.getType(), virement.getMontant());

        return response;
    }

    /**
     * Converts a VirementInternationalRequestDto to a VirementInternational entity.
     * Sets libelle and observation based on the type:
     * - If ENCAISSEMENT: libelle = "emetteur montant", observation avec détails émetteur
     * - If DECAISSEMENT: libelle = "beneficiaire montant", observation avec détails bénéficiaire
     *
     * @param dto The VirementInternationalRequestDto to convert.
     * @return The corresponding VirementInternational entity.
     */
    public VirementInternational toEntity(VirementInternationalRequestDto dto) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un DTO null en entité virement international");
            return null;
        }

        log.debug("Conversion du DTO en entité virement international - Banque ID: {}, Montant: {}",
                dto.getBanqueId(), dto.getMontant());

        VirementInternational virement = new VirementInternational();

        // Récupération de la banque
        log.debug("Recherche de la banque ID: {}", dto.getBanqueId());
        Banque banque = this.banqueRepository.findById(dto.getBanqueId()).orElseThrow(
                () -> {
                    log.error("Banque introuvable avec l'ID: {}", dto.getBanqueId());
                    return new ResourceNotFoundException("Banque with id: " + dto.getBanqueId() + " not found");
                }
        );

        log.debug("Banque trouvée: {} (ID: {})", banque.getNom(), banque.getId());

        virement.setBanque(banque);
        virement.setBeneficiaire(dto.getBeneficiaire());
        virement.setEmetteur(dto.getEmetteur());
        virement.setMontant(dto.getMontant());
        virement.setDateEmission(dto.getDateEmission());
        virement.setType(dto.getType());

        // Génération automatique du libellé et observation selon le type
        String libelle;
        String observation;

        if (dto.getType() == Type.ENCAISSEMENT) {
            observation = "Encaissement de " + dto.getEmetteur() + " - Montant: " + dto.getMontant();
            libelle=observation;
        } else {
            observation = "Décaissement vers " + dto.getBeneficiaire() + " - Montant: " + dto.getMontant();
            libelle=observation;
        }

        virement.setLibelle(libelle);
        virement.setObservation(observation);
        virement.setStatut(StatutMoyenPaiement.ENCOURS);

        log.debug("Entité virement international créée avec succès - Type: {}, Libellé: {}, Montant: {}",
                dto.getType(), libelle, dto.getMontant());

        return virement;
    }

    /**
     * Updates an existing VirementInternational entity with data from a VirementInternationalRequestDto.
     * Updates libelle and observation based on the type.
     *
     * @param virement The existing VirementInternational entity to update.
     * @param dto The VirementInternationalRequestDto containing the new data.
     * @return The updated VirementInternational entity.
     */
    public VirementInternational updateEntity(VirementInternational virement, VirementInternationalRequestDto dto) {
        if (virement == null || dto == null) {
            log.warn("Tentative de mise à jour avec virement ou DTO null");
            return null;
        }

        log.debug("Mise à jour du virement international ID: {}", virement.getId());

        // Mise à jour de la banque si changée
        if (dto.getBanqueId() != null && !dto.getBanqueId().equals(virement.getBanque().getId())) {
            log.debug("Changement de banque détecté - Nouvelle banque ID: {}", dto.getBanqueId());
            Banque banque = this.banqueRepository.findById(dto.getBanqueId()).orElseThrow(
                    () -> {
                        log.error("Banque introuvable avec l'ID: {}", dto.getBanqueId());
                        return new ResourceNotFoundException("Banque with id: " + dto.getBanqueId() + " not found");
                    }
            );
            virement.setBanque(banque);
        }

        virement.setBeneficiaire(dto.getBeneficiaire());
        virement.setEmetteur(dto.getEmetteur());
        virement.setMontant(dto.getMontant());
        virement.setDateEmission(dto.getDateEmission());
        virement.setType(dto.getType());

        // Régénération du libellé et observation selon le type
        String libelle;
        String observation;

        if (dto.getType() == Type.ENCAISSEMENT) {
            observation = "Encaissement de " + dto.getEmetteur() + " - Montant: " + dto.getMontant();
            libelle=observation;
        } else {
            observation = "Décaissement vers " + dto.getBeneficiaire() + " - Montant: " + dto.getMontant();
            libelle=observation;
        }

        virement.setLibelle(libelle);
        virement.setObservation(observation);

        log.debug("Virement international ID: {} mis à jour avec succès", virement.getId());

        return virement;
    }
}