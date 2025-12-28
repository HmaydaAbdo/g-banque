package staport.finance.gBanque.decomptes.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.decomptes.dto.DecompteRequestDto;
import staport.finance.gBanque.decomptes.dto.DecompteResponseDto;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;



@Component
public class DecompteMapper {


    private static final Logger log = LoggerFactory.getLogger(DecompteMapper.class);

    public Decompte toEntity(DecompteRequestDto requestDTO , Marche marche) {
        log.debug("Début de la conversion du DecompteRequestDto vers l'entité Decompte");

        if (requestDTO == null) {
            log.warn("Le DecompteRequestDto reçu est null");
            return null;
        }

        log.debug("Mapping du DTO vers l'entité pour le marché ID: {}", marche != null ? marche.getId() : "null");

        Decompte entity = new Decompte();

        entity.setMarche(marche);

        entity.setDateEmission(requestDTO.getDateEmission());
        entity.setMontant(requestDTO.getMontant());
        entity.setLibelle(requestDTO.getLibelle());
        entity.setObservation( marche.getNumero()  );
        entity.setStatus(Status.PREVU);

        log.info("Decompte créé avec succès - Montant: {}, Libellé: {}, Marché: {}",
                requestDTO.getMontant(), requestDTO.getLibelle(), marche != null ? marche.getNumero() : "null");

        return entity;
    }


    public DecompteResponseDto toDto(Decompte decompte) {
        log.debug("Début de la conversion de l'entité Decompte vers le DTO de réponse");

        if (decompte == null) {
            log.warn("L'entité Decompte reçue est null");
            return null;
        }

        log.debug("Conversion du Decompte ID: {} vers DTO", decompte.getId());

        DecompteResponseDto dto = new DecompteResponseDto();
        dto.setId(decompte.getId());
        dto.setDateEmission(decompte.getDateEmission());
        dto.setMontant(decompte.getMontant());
        dto.setObservation(decompte.getObservation());
        dto.setDateOperation(decompte.getDateOperation()!=null ? decompte.getDateOperation() : null);
        dto.setDateValeur(decompte.getDateValeur() != null ? decompte.getDateValeur() : null);
        dto.setStatus(decompte.getStatus());

        dto.setLibelle(decompte.getLibelle() );
        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = decompte.getMarche();

        if (marche != null) {
            log.debug("Récupération des informations du marché: {}", marche.getNom());

            dto.setMarcheId(marche.getId());
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
        } else {
            log.warn("Le marché associé au Decompte ID: {} est null", decompte.getId());
            throw new ResourceNotFoundException("Le marché associé au Decompte Id"+decompte.getId()+"est null");
        }

        Banque banque = decompte.getMarche().getBanque();
        if (banque != null) {
            log.debug("Récupération des informations de la banque: {}", banque.getNom());

            dto.setBanqueId(banque.getId());
            dto.setBanqueName(banque.getNom());
        } else {
            log.warn("La banque associée au Decompte ID: {} est null", decompte.getId());
            throw new ResourceNotFoundException("la banque associée au Decompte ID: "+decompte.getId()+" est null");
        }

        log.info("Conversion vers DecompteResponseDto terminée avec succès pour l'ID: {}", decompte.getId());

        return dto;
    }

}