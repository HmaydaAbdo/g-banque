package staport.finance.gBanque.cmt.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.cmt.dtos.*;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.services.EcheanceCMTService;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.cmt.mappers.CreditMoyenTermeMapper;
import staport.finance.gBanque.cmt.repositories.CreditMoyenTermeRepository;
import staport.finance.gBanque.cmt.specifications.CreditMoyenTermeSpecification;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;

/**
 * Service pour la gestion des crédits à moyen terme (CMT).
 */
@Service
@Transactional(readOnly = true)
public class CreditMoyenTermeService {

    private static final Logger log = LoggerFactory.getLogger(CreditMoyenTermeService.class);

    private final CreditMoyenTermeRepository creditMoyenTermeRepository;
    private final BanqueRepository banqueRepository;
    private final CreditMoyenTermeMapper creditMoyenTermeMapper;
    private final EcheanceCMTService echeanceCmtService;

    public CreditMoyenTermeService(CreditMoyenTermeRepository creditMoyenTermeRepository,
                                   BanqueRepository banqueRepository,
                                   CreditMoyenTermeMapper creditMoyenTermeMapper,
                                   EcheanceCMTService echeanceCmtService) {
        this.creditMoyenTermeRepository = creditMoyenTermeRepository;
        this.banqueRepository = banqueRepository;
        this.creditMoyenTermeMapper = creditMoyenTermeMapper;
        this.echeanceCmtService = echeanceCmtService;
    }

    @Transactional
    public CreditMoyenTermeResponseDto create(CreditMoyenTermeRequestDto requestDto) {
        log.info("Début de création d'un crédit CMT - Banque ID: {}, Montant: {}", requestDto.getBanqueId(), requestDto.getNominal());
        log.debug("Détails du crédit à créer: {}", requestDto);

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque introuvable lors de la création du crédit CMT - ID: {}", requestDto.getBanqueId());
                    return new ResourceNotFoundException("Banque introuvable avec l'ID : " + requestDto.getBanqueId());
                });

        log.debug("Banque trouvée: {} - ID: {}", banque.getNom(), banque.getId());

        CreditMoyenTerme credit = creditMoyenTermeMapper.toEntity(requestDto, banque);
        CreditMoyenTerme savedCredit = creditMoyenTermeRepository.save(credit);

        log.info("Crédit CMT enregistré avec succès - ID: {}", savedCredit.getId());

        log.debug("Génération des échéances pour le crédit ID: {}", savedCredit.getId());
        echeanceCmtService.generateEcheances(savedCredit);
        log.info("Échéances générées avec succès pour le crédit ID: {}", savedCredit.getId());

        return creditMoyenTermeMapper.toResponseDto(savedCredit);
    }

    @Transactional(readOnly = true)
    public CreditMoyenTermeResponseDto findById(Long id) {
        log.debug("Recherche du crédit CMT - ID: {}", id);

        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Crédit CMT introuvable - ID: {}", id);
                    return new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + id);
                });

        log.debug("Crédit CMT trouvé - ID: {}, Désignation: {}", credit.getId(), credit.getDesignation());
        return creditMoyenTermeMapper.toResponseDto(credit);
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchActiveCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de crédits CMT actifs - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());
        log.debug("Critères de recherche: {}", searchDto);

        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getActiveCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.withSearchCriteria(searchDto);

        Page<CreditMoyenTermeResponseDto> resultPage = creditMoyenTermeRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(creditMoyenTermeMapper::toResponseDto);

        log.info("Recherche de crédits actifs terminée - {} résultat(s) trouvé(s) sur {} total", resultPage.getNumberOfElements(), resultPage.getTotalElements());

        return resultPage;
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchPendingClosureCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de crédits CMT en attente de main levée - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());
        log.debug("Critères de recherche: {}", searchDto);

        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getPendingClosureCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.withSearchCriteria(searchDto);

        Page<CreditMoyenTermeResponseDto> resultPage = creditMoyenTermeRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(creditMoyenTermeMapper::toResponseDto);

        log.info("Recherche de crédits en attente terminée - {} résultat(s) trouvé(s) sur {} total", resultPage.getNumberOfElements(), resultPage.getTotalElements());

        return resultPage;
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchArchivedCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de crédits CMT archivés - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());
        log.debug("Critères de recherche: {}", searchDto);

        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getArchivedCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.withSearchCriteria(searchDto);

        Page<CreditMoyenTermeResponseDto> resultPage = creditMoyenTermeRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(creditMoyenTermeMapper::toResponseDto);

        log.info("Recherche de crédits archivés terminée - {} résultat(s) trouvé(s) sur {} total",
                resultPage.getNumberOfElements(), resultPage.getTotalElements());

        return resultPage;
    }

    @Transactional
    public CreditMoyenTermeResponseDto update(Long id, CreditMoyenTermeRequestDto requestDto) {
        log.info("Début de mise à jour du crédit CMT - ID: {}", id);
        log.debug("Nouvelles données: {}", requestDto);

        CreditMoyenTerme existingCredit = creditMoyenTermeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Crédit CMT introuvable lors de la mise à jour - ID: {}", id);
                    return new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + id);
                });

        log.debug("Crédit existant trouvé - Désignation actuelle: {}", existingCredit.getDesignation());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque introuvable lors de la mise à jour du crédit CMT - ID: {}", requestDto.getBanqueId());
                    return new ResourceNotFoundException("Banque introuvable avec l'ID : " + requestDto.getBanqueId());
                });

        log.debug("Banque trouvée: {} - ID: {}", banque.getNom(), banque.getId());

        creditMoyenTermeMapper.updateEntity(requestDto, existingCredit, banque);
        CreditMoyenTerme updatedCredit = creditMoyenTermeRepository.save(existingCredit);

        log.info("Crédit CMT mis à jour avec succès - ID: {}", updatedCredit.getId());

        log.debug("Régénération des échéances pour le crédit ID: {}", updatedCredit.getId());
        echeanceCmtService.regenerateEcheances(updatedCredit.getId());
        log.info("Échéances régénérées avec succès pour le crédit ID: {}", updatedCredit.getId());

        return creditMoyenTermeMapper.toResponseDto(updatedCredit);
    }

    /**
     * Supprime un crédit à moyen terme ainsi que toutes ses échéances associées.
     * La suppression est bloquée si le crédit contient des échéances déjà payées.
     *
     * @param id L'ID du crédit à supprimer
     * @return Un DTO de confirmation de suppression
     * @throws ResourceNotFoundException Si le crédit n'existe pas
     * @throws ValidationException Si le crédit contient des échéances payées
     */
    @Transactional
    public DeleteResponceDto delete(Long id) {
        log.info("Début de la suppression du crédit CMT avec l'ID : {}", id);

        // 1. Récupérer le crédit
        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Crédit CMT introuvable lors de la suppression - ID: {}", id);
                    return new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + id);
                });

        log.debug("Crédit trouvé : {} - ID: {}", credit.getDesignation(), credit.getId());

        // 2. Vérifier s'il y a des échéances payées (protection métier)
        log.debug("Vérification des échéances payées pour le crédit ID: {}", id);
        if (echeanceCmtService.hasPaidEcheances(id)) {
            log.error("Tentative de suppression d'un crédit avec échéances payées - ID: {}", id);
            throw new ValidationException(
                    "Impossible de supprimer le crédit avec l'ID " + id +
                            " car certaines de ses échéances ont déjà été payées. " +
                            "Veuillez l'archiver au lieu de le supprimer."
            );
        }

        // 3. Supprimer les échéances associées
        log.debug("Suppression des échéances pour le crédit ID: {}", id);
        int echeancesDeleted = echeanceCmtService.deleteEcheancesByCreditMoyenTerme(credit);
        log.info("{} échéance(s) supprimée(s) pour le crédit CMT ID: {}", echeancesDeleted, id);

        // 4. Supprimer le crédit
        log.debug("Suppression du crédit CMT ID: {}", id);
        creditMoyenTermeRepository.delete(credit);

        log.info("Crédit CMT et toutes ses échéances supprimés avec succès - ID: {}, Désignation: {}",
                id, credit.getDesignation());

        return new DeleteResponceDto("Credit moyen terme deleted successfully");
    }
    @Transactional
    public CreditMoyenTermeResponseDto markAsArchived(Long id) {
        log.info("Tentative d'archivage du crédit CMT - ID: {}", id);

        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Crédit CMT introuvable lors de l'archivage - ID: {}", id);
                    return new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + id);
                });

        if (credit.isCompleted()) {
            log.warn("Tentative d'archivage d'un crédit déjà archivé - ID: {}", id);
            throw new ValidationException("Le crédit avec l'ID " + id + " est déjà archivé.");
        }

        log.debug("Vérification du statut des échéances pour le crédit ID: {}", id);

        boolean hasUnpaidInstallments = credit.getEcheanceCMTS().stream()
                .anyMatch(echeance -> echeance.getStatutPaiement() != StatutEcheanceCMT.PAYE);

        if (hasUnpaidInstallments) {
            log.error("Tentative d'archivage d'un crédit avec échéances impayées - ID: {}", id);
            throw new ValidationException("Le crédit ne peut être archivé que si toutes les échéances ont été payées.");
        }

        log.debug("Toutes les échéances sont payées - Marquage du crédit comme archivé");
        credit.setCompleted(true);
        CreditMoyenTerme updatedCredit = creditMoyenTermeRepository.save(credit);

        log.info("Crédit CMT archivé avec succès - ID: {}", id);
        return creditMoyenTermeMapper.toResponseDto(updatedCredit);
    }
}