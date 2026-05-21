package staport.finance.gBanque.moyensDePaiements.virements.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.virements.dtos.VirementRequestDto;
import staport.finance.gBanque.moyensDePaiements.virements.dtos.VirementResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

@Component
public class VirementMapper {

    private static final Logger log = LoggerFactory.getLogger(VirementMapper.class);
    private final BanqueRepository banqueRepository;

    public VirementMapper(BanqueRepository banqueRepository) {
        this.banqueRepository = banqueRepository;
    }

    /**
     * Converts a Virement entity to a VirementResponseDto.
     *
     * @param virement The Virement entity to convert.
     * @return The corresponding VirementResponseDto.
     */
    public VirementResponseDto toDto(Virement virement) {
        if (virement == null) {
            log.warn("Tentative de conversion d'un virement null en DTO");
            return null;
        }

        log.debug("Conversion du virement ID: {} en DTO", virement.getId());

        VirementResponseDto response = new VirementResponseDto();
        response.setId(virement.getId());
        response.setNumero(virement.getNumero() != null ? virement.getNumero() : null);
        response.setBeneficiaire(virement.getBeneficiaire());
        response.setDateEmission(virement.getDateEmission());
        response.setMontant(virement.getMontant());
        response.setBanqueEmettriceNom(virement.getBanqueEmettrice() != null ? virement.getBanqueEmettrice().getNom() : null);
        response.setBanqueBeneficiaire(virement.getBanqueBeneficiare() != null ? virement.getBanqueBeneficiare().getNom() : virement.getNomBanqueBeneficiaireLibre());
        response.setRibBeneficiaire(virement.getRibBeneficiaire() != null ? virement.getRibBeneficiaire() : null);
        response.setStatut(virement.getStatut());
        response.setDateOperation(virement.getDateOperation() != null ? virement.getDateOperation() : null);
        response.setDateValeur(virement.getDateValeur() != null ? virement.getDateValeur() : null);
        response.setType(virement.getType());
        response.setObservation(" VIREMENT N° " + virement.getNumero());
        response.setLibelle(virement.getType() == Type.DECAISSEMENT ? virement.getBeneficiaire() : virement.getEmetteur());
        response.setEmetteur(virement.getEmetteur() != null ? virement.getEmetteur() : null);
        response.setBanqueBeneficiaireId(virement.getBanqueBeneficiare() != null ? virement.getBanqueBeneficiare().getId() : null);
        response.setBanqueEmettriceId(virement.getBanqueEmettrice() != null ? virement.getBanqueEmettrice().getId() : null);

        log.debug("Conversion réussie du virement ID: {} (Type: {}, Montant: {})",
                virement.getId(), virement.getType(), virement.getMontant());

        return response;
    }

    public Virement toEntity(VirementRequestDto dto) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un DTO null en entité virement");
            return null;
        }

        log.debug("Conversion du DTO en entité virement - Banque bénéficiaire ID: {}, Montant: {}",
                dto.getBanqueBeneficiaireId(), dto.getMontant());

        Virement virement = new Virement();

        log.debug("Recherche de la banque bénéficiaire ID: {}", dto.getBanqueBeneficiaireId());
        Banque banque = this.banqueRepository.findById(dto.getBanqueBeneficiaireId()).orElseThrow(
                () -> {
                    log.error("Banque introuvable avec l'ID: {}", dto.getBanqueBeneficiaireId());
                    return new ResourceNotFoundException("Banque with id: " + dto.getBanqueBeneficiaireId() + " not found");
                }
        );

        log.debug("Banque bénéficiaire trouvée: {} (ID: {})", banque.getNom(), banque.getId());

        virement.setBanqueBeneficiare(banque);
        virement.setDateEmission(dto.getDateReception());
        virement.setBeneficiaire(dto.getBeneficiaire());
        virement.setMontant(dto.getMontant());
        virement.setEmetteur(dto.getEmetteur());
        virement.setType(dto.getType());
        virement.setStatut(StatutMoyenPaiement.ENCOURS);
        log.debug("Entité virement créée avec succès - Type: {}, Émetteur: {}, Montant: {}",
                dto.getType(), dto.getEmetteur(), dto.getMontant());

        return virement;
    }
}