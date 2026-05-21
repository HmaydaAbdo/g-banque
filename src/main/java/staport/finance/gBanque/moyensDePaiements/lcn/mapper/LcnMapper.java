package staport.finance.gBanque.moyensDePaiements.lcn.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.LcnRequestDto;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.LcnResponseDTO;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

public class LcnMapper {

    private static final Logger log = LoggerFactory.getLogger(LcnMapper.class);

    public static LcnResponseDTO toDto(Lcn lcn) {

        if (lcn == null) {
            log.warn("Tentative de conversion d'un LCN null en DTO");
            return null;
        }

        log.debug("Conversion du LCN ID: {} en DTO", lcn.getId());

        LcnResponseDTO dto = new LcnResponseDTO();
        dto.setId(lcn.getId());
        dto.setNumero(lcn.getNumero() != null ? lcn.getNumero() : null);
        dto.setDateEmission(lcn.getDateEmission());
        dto.setBeneficiaire(lcn.getBeneficiaire());
        dto.setDateEcheance(lcn.getDateEcheance());
        dto.setMontant(lcn.getMontant());
        dto.setStatut(lcn.getStatut());

        // We only expose the bank's name, not the whole Banque object.
        if (lcn.getBanque() != null) {
            dto.setBanqueName(lcn.getBanque().getNom());
            dto.setBanqueId(lcn.getBanque().getId());
        }

        dto.setDateOperation(lcn.getDateOperation() != null ? lcn.getDateOperation() : null);
        dto.setDateValeur(lcn.getDateValeur() != null ? lcn.getDateValeur() : null);
        dto.setObservation(" LCN N° " + lcn.getNumero());
        dto.setLibelle(lcn.getType() == Type.DECAISSEMENT ? lcn.getBeneficiaire() : lcn.getEmetteur());
        dto.setType(lcn.getType());
        dto.setEmetteur(lcn.getEmetteur() != null ? lcn.getEmetteur() : null);
        dto.setNatureLcn(lcn.getNatureLcn());

        log.debug("Conversion réussie du LCN ID: {} (Type: {}, Montant: {}, Échéance: {})", lcn.getId(), lcn.getType(), lcn.getMontant(), lcn.getDateEcheance());

        return dto;
    }

    public static Lcn toEntity(LcnRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un DTO null en entité LCN");
            return null;
        }

        if (banque == null) {
            log.warn("Tentative de création d'un LCN avec une banque null");
            return null;
        }

        log.debug("Conversion du DTO en entité LCN - Banque: {} (ID: {}), Montant: {}, Échéance: {}",
                banque.getNom(), banque.getId(), dto.getMontant(), dto.getDateEcheance());

        Lcn lcn = new Lcn();
        lcn.setDateEmission(dto.getDateEmission());
        lcn.setDateEcheance(dto.getDateEcheance());
        lcn.setBeneficiaire(dto.getBeneficiaire());
        lcn.setMontant(dto.getMontant());
        lcn.setBanque(banque);
        lcn.setStatut(StatutMoyenPaiement.ENCOURS);
        lcn.setNumero(dto.getNumero());
        lcn.setType(dto.getType());
        lcn.setEmetteur(dto.getEmetteur());
        lcn.setNatureLcn(dto.getNatureLcn());

        log.debug("Entité LCN créée avec succès - Numéro: {}, Type: {}, Nature: {}, Émetteur: {}, Montant: {}",
                dto.getNumero(), dto.getType(), dto.getNatureLcn(), dto.getEmetteur(), dto.getMontant());

        return lcn;
    }
}