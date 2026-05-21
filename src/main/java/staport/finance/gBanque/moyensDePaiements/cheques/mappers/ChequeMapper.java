package staport.finance.gBanque.moyensDePaiements.cheques.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.cheques.dtos.ChequeRequestDto;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.dtos.ChequeResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

public class ChequeMapper {

    private static final Logger log = LoggerFactory.getLogger(ChequeMapper.class);

    public static ChequeResponseDto toDto(Cheque cheque) {
        if (cheque == null) {
            log.warn("Tentative de conversion d'un chèque null en DTO");
            return null;
        }

        log.debug("Conversion du chèque ID: {} en DTO", cheque.getId());

        ChequeResponseDto dto = new ChequeResponseDto();
        dto.setId(cheque.getId());
        dto.setNumero(cheque.getNumero() != null ? cheque.getNumero() : null);
        dto.setDateEmission(cheque.getDateEmission());
        dto.setDateEcheance(cheque.getDateEcheance() != null ? cheque.getDateEcheance() : null);
        dto.setBeneficiaire(cheque.getBeneficiaire());
        dto.setMontant(cheque.getMontant());
        dto.setStatut(cheque.getStatut() );
        dto.setType(cheque.getType());
        dto.setEmetteur(cheque.getEmetteur() != null ? cheque.getEmetteur() : null);

        // We only need the bank's name for the DTO, not the entire Banque object,
        // which avoids exposing unnecessary data and improves performance.
        if (cheque.getBanque() != null) {
            dto.setNomBanque(cheque.getBanque().getNom());
            dto.setBanqueId(cheque.getBanque().getId());
        }

        dto.setDateOperation(cheque.getDateOperation() != null ? cheque.getDateOperation() : null);
        dto.setDateValeur(cheque.getDateValeur() != null ? cheque.getDateValeur() : null);
        dto.setObservation("CHQ N°" + cheque.getNumero());
        dto.setLibelle(cheque.getType() == Type.DECAISSEMENT ? cheque.getBeneficiaire() : cheque.getEmetteur());

        log.debug("Conversion réussie du chèque ID: {} (Type: {}, Numéro: {}, Montant: {})",
                cheque.getId(), cheque.getType(), cheque.getNumero(), cheque.getMontant());

        return dto;
    }

    public static Cheque toEntity(ChequeRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un DTO null en entité chèque");
            return null;
        }

        if (banque == null) {
            log.warn("Tentative de création d'un chèque avec une banque null");
            return null;
        }

        log.debug("Conversion du DTO en entité chèque - Banque: {} (ID: {}), Numéro: {}, Montant: {}",
                banque.getNom(), banque.getId(), dto.getNumero(), dto.getMontant());

        Cheque cheque = new Cheque();

        cheque.setNumero(dto.getNumero());
        cheque.setDateEmission(dto.getDateEmission());

        if (dto.getDateEcheance() != null) {
            cheque.setDateEcheance(dto.getDateEcheance());
        }

        cheque.setBeneficiaire(dto.getBeneficiaire());
        cheque.setMontant(dto.getMontant());
        cheque.setBanque(banque);
        cheque.setStatut(StatutMoyenPaiement.ENCOURS);
        cheque.setType(dto.getType());
        cheque.setEmetteur(dto.getEmetteur());

        log.debug("Entité chèque créée avec succès - Numéro: {}, Type: {}, Émetteur: {}, Montant: {}",
                dto.getNumero(), dto.getType(), dto.getEmetteur(), dto.getMontant());

        return cheque;
    }
}