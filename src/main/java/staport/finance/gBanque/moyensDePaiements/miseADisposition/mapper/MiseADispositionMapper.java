package staport.finance.gBanque.moyensDePaiements.miseADisposition.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos.MiseADispositionResponseDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos.MiseAdispositionRequestDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

public class MiseADispositionMapper {

    private static final Logger log = LoggerFactory.getLogger(MiseADispositionMapper.class);

    /**
     * Converts a MiseADisposition entity to a MiseADispositionResponseDto.
     *
     * @param miseADisposition The MiseADisposition entity to convert.
     * @return The corresponding MiseADispositionResponseDto.
     */
    public static MiseADispositionResponseDto toDto(MiseADisposition miseADisposition) {
        if (miseADisposition == null) {
            log.warn("Tentative de conversion d'une mise à disposition null en DTO");
            return null;
        }

        log.debug("Conversion de la mise à disposition ID: {} en DTO", miseADisposition.getId());

        MiseADispositionResponseDto dto = new MiseADispositionResponseDto();
        dto.setId(miseADisposition.getId());

        dto.setNumero(miseADisposition.getNumero() != null ? miseADisposition.getNumero() : null);
        dto.setDateEmission(miseADisposition.getDateEmission());
        dto.setBeneficiaire(miseADisposition.getBeneficiaire());
        dto.setMontant(miseADisposition.getMontant());
        dto.setCinBeneficiaire(miseADisposition.getCinBeneficiaire());
        dto.setStatut(miseADisposition.getStatut());
        dto.setType(miseADisposition.getType());
        dto.setEmetteur(miseADisposition.getEmetteur() != null ? miseADisposition.getEmetteur() : null);

        if (miseADisposition.getBanque() != null) {
            dto.setBanqueName(miseADisposition.getBanque().getNom());
            dto.setBanqueId(miseADisposition.getBanque().getId());
        }

        dto.setDateOperation(miseADisposition.getDateOperation() != null ? miseADisposition.getDateOperation() : null);
        dto.setDateValeur(miseADisposition.getDateValeur() != null ? miseADisposition.getDateValeur() : null);
        dto.setObservation(" MAD " + miseADisposition.getNumero());
        dto.setLibelle(miseADisposition.getBeneficiaire());

        log.debug("Conversion réussie de la mise à disposition ID: {} (Type: {}, Montant: {})",
                miseADisposition.getId(), miseADisposition.getType(), miseADisposition.getMontant());

        return dto;
    }

    public static MiseADisposition toEntity(MiseAdispositionRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un DTO null en entité mise à disposition");
            return null;
        }

        if (banque == null) {
            log.warn("Tentative de création d'une mise à disposition avec une banque null");
            return null;
        }

        log.debug("Conversion du DTO en entité mise à disposition - Banque: {} (ID: {}), Montant: {}",
                banque.getNom(), banque.getId(), dto.getMontant());

        MiseADisposition miseADisposition = new MiseADisposition();

        miseADisposition.setDateEmission(dto.getDateEmission());
        miseADisposition.setBeneficiaire(dto.getBeneficiaire());
        miseADisposition.setMontant(dto.getMontant());
        miseADisposition.setBanque(banque);
        miseADisposition.setStatut(StatutMoyenPaiement.ENCOURS);
        miseADisposition.setType(dto.getType());
        miseADisposition.setEmetteur(dto.getEmetteur());

        log.debug("Entité mise à disposition créée avec succès - Type: {}, Émetteur: {}, Montant: {}",
                dto.getType(), dto.getEmetteur(), dto.getMontant());

        return miseADisposition;
    }
}