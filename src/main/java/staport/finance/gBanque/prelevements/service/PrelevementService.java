package staport.finance.gBanque.prelevements.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prelevements.dtos.EncoursPrelevement;
import staport.finance.gBanque.prelevements.dtos.PrelevementRequestDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementResponseDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementSearchDto;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.prelevements.mapper.PrelevementMapper;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;
import staport.finance.gBanque.prelevements.spec.PrelevementSpec;

import java.math.BigDecimal;

@Service
public class PrelevementService {

    private static final Logger log = LoggerFactory.getLogger(PrelevementService.class);
    private final PrelevementRepository prelevementRepository;
    private final BanqueRepository banqueRepository;
    private final PrelevementMapper prelevementMapper;

    public PrelevementService(PrelevementRepository prelevementRepository, BanqueRepository banqueRepository, PrelevementMapper prelevementMapper) {
        this.prelevementRepository = prelevementRepository;
        this.banqueRepository = banqueRepository;
        this.prelevementMapper = prelevementMapper;
        log.info("Initialisation du service Prelevement");
    }

    /**
     * Crée un nouveau prélèvement
     *
     * @param requestDto Données du prélèvement à créer
     * @return Le prélèvement créé
     */
    @Transactional
    public PrelevementResponseDto createPrelevement(PrelevementRequestDto requestDto) {
        log.info("Création d'un prélèvement - Banque ID: {}, Libellé: '{}', Montant: {}",
                requestDto.getBanqueId(), requestDto.getLibelle(), requestDto.getMontant());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID: {}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque non trouvée avec l'ID: " + requestDto.getBanqueId());
                });

        log.debug("Banque trouvée: {} (ID: {})", banque.getNom(), banque.getId());

        Prelevement prelevement = prelevementMapper.toEntity(requestDto, banque);
        Prelevement savedPrelevement = prelevementRepository.save(prelevement);

        log.info("Prélèvement créé avec succès - ID: {}, Statut: {}", savedPrelevement.getId(), savedPrelevement.getStatus());
        return prelevementMapper.toResponseDto(savedPrelevement);
    }

    /**
     * Récupère un prélèvement par son ID
     *
     * @param id Identifiant du prélèvement
     * @return Le prélèvement demandé
     */
    @Transactional(readOnly = true)
    public PrelevementResponseDto getById(Long id) {
        log.info("Récupération du prélèvement avec l'ID: {}", id);

        Prelevement prelevement = prelevementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prélèvement non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Prélèvement non trouvé avec l'ID: " + id);
                });

        log.debug("Prélèvement trouvé - Libellé: '{}', Montant: {}, Statut: {}",
                prelevement.getLibelle(), prelevement.getMontant(), prelevement.getStatus());
        return prelevementMapper.toResponseDto(prelevement);
    }

    /**
     * Recherche des prélèvements selon des critères avec pagination
     *
     * @param searchDto Critères de recherche
     * @param pageable Informations de pagination
     * @return Page de prélèvements correspondant aux critères
     */
    @Transactional(readOnly = true)
    public Page<PrelevementResponseDto> searchPrelevements(PrelevementSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de prélèvements avec critères: {}, Page: {}, Taille: {}",
                searchDto, pageable.getPageNumber(), pageable.getPageSize());

        Page<PrelevementResponseDto> result = prelevementRepository.findAll(
                        PrelevementSpec.buildSpecification(searchDto),
                        pageable
                )
                .map(prelevementMapper::toResponseDto);

        log.info("Recherche terminée - {} prélèvement(s) trouvé(s) sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * Met à jour un prélèvement existant
     *
     * @param id Identifiant du prélèvement à modifier
     * @param requestDto Nouvelles données du prélèvement
     * @return Le prélèvement mis à jour
     */
    @Transactional
    public PrelevementResponseDto updatePrelevement(Long id, PrelevementRequestDto requestDto) {
        log.info("Mise à jour du prélèvement ID: {} - Nouveau libellé: '{}', Nouveau montant: {}",
                id, requestDto.getLibelle(), requestDto.getMontant());

        Prelevement prelevement = prelevementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prélèvement non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Prélèvement non trouvé avec l'ID: " + id);
                });

        log.debug("Prélèvement actuel avant modification - Libellé: '{}', Montant: {}, Statut: {}",
                prelevement.getLibelle(), prelevement.getMontant(), prelevement.getStatus());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID: {}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque non trouvée avec l'ID: " + requestDto.getBanqueId());
                });

        prelevement.setLibelle(requestDto.getLibelle());
        prelevement.setMontant(requestDto.getMontant());
        prelevement.setBanque(banque);
        prelevement.setDateEmission(requestDto.getDateEmission());

        Prelevement updatedPrelevement = prelevementRepository.save(prelevement);
        log.info("Prélèvement ID: {} mis à jour avec succès", id);
        return prelevementMapper.toResponseDto(updatedPrelevement);
    }

    /**
     * Supprime un prélèvement
     *
     * @param id Identifiant du prélèvement à supprimer
     */
    @Transactional
    public void deletePrelevement(Long id) {
        log.info("Suppression du prélèvement avec l'ID: {}", id);

        if (!prelevementRepository.existsById(id)) {
            log.error("Prélèvement non trouvé avec l'ID: {}", id);
            throw new IllegalArgumentException("Prélèvement non trouvé avec l'ID: " + id);
        }

        prelevementRepository.deleteById(id);
        log.info("Prélèvement ID: {} supprimé avec succès", id);
    }

    /**
     * Récupère les prélèvements en cours avec leur montant total
     * Les prélèvements en cours sont ceux avec un statut 'PREVU' et une date d'émission dans le passé
     *
     * @param searchDto Critères de recherche optionnels
     * @param pageable Informations de pagination et de tri
     * @return Un EncoursPrelevement contenant la page de prélèvements et le montant total en cours
     */
    @Transactional(readOnly = true)
    public EncoursPrelevement getOutstandingPrelevements(PrelevementSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des prélèvements en cours - Critères: {}", searchDto);

        // Construction de la spécification pour filtrer les enregistrements
        Specification<Prelevement> spec = Specification.where(PrelevementSpec.outstandingPrelevement())
                .and(PrelevementSpec.hasBanque(searchDto.getBanqueId()))
                .and(PrelevementSpec.hasLibelle(searchDto.getLibelle()))
                .and(PrelevementSpec.hasMontant(searchDto.getMontant()));

        // Récupération de la liste paginée de prélèvements
        Page<Prelevement> prelevementPage = prelevementRepository.findAll(spec, pageable);
        log.debug("Page de prélèvements récupérée: {} éléments sur {}",
                prelevementPage.getNumberOfElements(), prelevementPage.getTotalElements());

        // Calcul du montant total des encours
        BigDecimal totalDesEncours = prelevementRepository.findAll(spec).stream()
                .map(Prelevement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Montant total des encours calculé: {}", totalDesEncours);

        // Mapping de la page d'entités vers une page de DTOs
        Page<PrelevementResponseDto> pageDto = prelevementPage.map(prelevementMapper::toResponseDto);

        // Création et retour du DTO final
        EncoursPrelevement result = new EncoursPrelevement();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        log.info("Encours de prélèvements récupérés avec succès - Total: {}, Nombre de prélèvements: {}",
                totalDesEncours, prelevementPage.getTotalElements());
        return result;
    }


    @Transactional
    public PrelevementResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut du prélèvement ID: {}", id);

        Prelevement prelevement = prelevementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prélèvement non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Prélèvement non trouvé avec l'ID: " + id);
                });

        Status oldStatus = prelevement.getStatus();
        Status newStatus=oldStatus==Status.PAYE ? Status.PREVU : Status.PAYE;

        prelevement.setStatus(newStatus);
        Prelevement updatedPrelevement = prelevementRepository.save(prelevement);

        log.info("Statut du prélèvement ID: {} basculé avec succès de {} vers {}",
                id, oldStatus, newStatus);

        return prelevementMapper.toResponseDto(updatedPrelevement);
    }
}