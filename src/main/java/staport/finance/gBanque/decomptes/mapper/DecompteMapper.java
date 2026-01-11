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

    /**
     * Convertit DecompteRequestDto en entité Decompte
     *
     * @param requestDTO Le DTO de requête
     * @param marche Le marché associé
     * @param banqueEncaissement La banque d'encaissement (déterminée par le service)
     * @return L'entité Decompte
     */
    public Decompte toEntity(DecompteRequestDto requestDTO, Marche marche, Banque banqueEncaissement) {
        log.debug("Début de la conversion du DecompteRequestDto vers l'entité Decompte");

        if (requestDTO == null) {
            log.warn("Le DecompteRequestDto reçu est null");
            return null;
        }

        log.debug("Mapping du DTO vers l'entité pour le marché ID: {}, banque encaissement ID: {}",
                marche != null ? marche.getId() : "null",
                banqueEncaissement != null ? banqueEncaissement.getId() : "null");

        Decompte entity = new Decompte();

        entity.setMarche(marche);
        entity.setBanqueEncaissement(banqueEncaissement);
        entity.setDateEmission(requestDTO.getDateEmission());
        entity.setMontant(requestDTO.getMontant());
        entity.setLibelle(requestDTO.getLibelle());
        entity.setObservation(marche.getNumero());
        entity.setStatus(Status.PREVU);

        log.info("Decompte créé avec succès {}",requestDTO);

        return entity;
    }

    /**
     * Convertit l'entité Decompte en DecompteResponseDto
     *
     * @param decompte L'entité Decompte
     * @return Le DTO de réponse
     */
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
        dto.setDateOperation(decompte.getDateOperation() != null ? decompte.getDateOperation() : null);
        dto.setDateValeur(decompte.getDateValeur() != null ? decompte.getDateValeur() : null);
        dto.setStatus(decompte.getStatus());
        dto.setLibelle(decompte.getLibelle());

        // Mapper les informations du marché
        Marche marche = decompte.getMarche();
        if (marche != null) {
            log.debug("Récupération des informations du marché: {}", marche.getNom());

            dto.setMarcheId(marche.getId());
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
            dto.setMarcheNanti(marche.isNanti());

            // Mapper la banque du marché
            Banque marcheBanque = marche.getBanque();
            if (marcheBanque != null) {
                log.debug("Récupération de la banque du marché: {}", marcheBanque.getNom());
                dto.setMarcheBanqueId(marcheBanque.getId());
                dto.setMarcheBanqueName(marcheBanque.getNom());
            } else {
                log.warn("La banque du marché associée au Decompte ID: {} est null", decompte.getId());
                throw new ResourceNotFoundException("La banque du marché associée au Decompte ID: " + decompte.getId() + " est null");
            }
        } else {
            log.warn("Le marché associé au Decompte ID: {} est null", decompte.getId());
            throw new ResourceNotFoundException("Le marché associé au Decompte ID: " + decompte.getId() + " est null");
        }

        // Mapper la banque d'encaissement
        Banque banqueEncaissement = decompte.getBanqueEncaissement();
        if (banqueEncaissement != null) {
            log.debug("Récupération de la banque d'encaissement: {}", banqueEncaissement.getNom());
            dto.setBanqueEncaissementId(banqueEncaissement.getId());
            dto.setBanqueEncaissementName(banqueEncaissement.getNom());
        }

        log.info("Conversion vers DecompteResponseDto terminée avec succès pour l'ID: {} - Marché nanti: {}, Banque marché: {}, Banque encaissement: {}",
                decompte.getId(),
                marche != null ? marche.isNanti() : "N/A",
                marche != null && marche.getBanque() != null ? marche.getBanque().getNom() : "null",
                banqueEncaissement != null ? banqueEncaissement.getNom() : "null");

        return dto;
    }
}