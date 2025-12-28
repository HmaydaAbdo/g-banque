package staport.finance.gBanque.decomptes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.decomptes.dto.*;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.mapper.DecompteMapper;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.decomptes.spec.DecompteSpec;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;

@Service
public class DecompteService {

    private static final Logger log = LoggerFactory.getLogger(DecompteService.class);
    private final DecompteRepository decompteRepository;
    private final DecompteMapper decompteMapper;
    private final MarcheRepository marcheRepository;

    public DecompteService(DecompteRepository decompteRepository, DecompteMapper decompteMapper, MarcheRepository marcheRepository) {
        this.decompteRepository = decompteRepository;
        this.decompteMapper = decompteMapper;
        this.marcheRepository = marcheRepository;
    }

    @Transactional
    public DecompteResponseDto createDecompte(DecompteRequestDto requestDTO) {
        log.info("Début de la création d'un décompte pour le marché ID: {}", requestDTO.getMarcheId());
        log.debug("Données reçues: {}", requestDTO);

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marché non trouvé avec l'ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        log.debug("Marché récupéré: {} (Numéro: {})", marche.getNom(), marche.getNumero());

        Decompte decompte = decompteMapper.toEntity(requestDTO, marche);

        decompte = decompteRepository.save(decompte);

        log.info("Décompte créé avec succès - ID: {}, Montant: {}, Libellé: {}", decompte.getId(), decompte.getMontant(), decompte.getLibelle());

        return decompteMapper.toDto(decompte);
    }

    @Transactional
    public DecompteResponseDto updateDecompte(Long id, DecompteRequestDto requestDTO) {
        log.info("Début de la modification du décompte ID: {}", id);
        log.debug("Nouvelles données reçues: {}", requestDTO);

        Decompte existingDecompte = decompteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Décompte non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("décompte not found with ID: " + id);
                });

        log.debug("Décompte existant récupéré - Ancien montant: {}, Ancien libellé: {}",
                existingDecompte.getMontant(), existingDecompte.getLibelle());

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marché non trouvé avec l'ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        log.debug("Marché récupéré pour la mise à jour: {}", marche.getNom());

        existingDecompte.setMarche(marche);
        existingDecompte.setDateEmission(requestDTO.getDateEmission());
        existingDecompte.setMontant(requestDTO.getMontant());
        existingDecompte.setObservation("Marche: " + marche.getNumero() );
        Decompte updatedDecompte = decompteRepository.save(existingDecompte);

        log.info("Décompte modifié avec succès - ID: {}, Nouveau montant: {}",
                id, updatedDecompte.getMontant());

        return decompteMapper.toDto(updatedDecompte);
    }

    @Transactional
    public void deleteDecompte(Long id) {
        log.info("Tentative de suppression du décompte ID: {}", id);

        if (!decompteRepository.existsById(id)) {
            log.error("Impossible de supprimer - Décompte non trouvé avec l'ID: {}", id);
            throw new ResourceNotFoundException("décompte not found with ID: " + id);
        }

        decompteRepository.deleteById(id);
        log.info("Décompte ID: {} supprimé avec succès", id);
    }

    @Transactional(readOnly = true)
    public DecompteResponseDto findById(Long id) {
        log.debug("Recherche du décompte avec l'ID: {}", id);

        Decompte decompte = decompteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Décompte non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Décompte not found with ID: " + id);
                });

        log.debug("Décompte trouvé - Marché: {}, Montant: {}",
                decompte.getMarche().getNom(), decompte.getMontant());

        return decompteMapper.toDto(decompte);
    }

    @Transactional(readOnly = true)
    public Page<DecompteResponseDto> findAll(DecompteSearchDto searchDTO, Pageable pageable) {
        log.info("Recherche de décomptes avec critères: {}", searchDTO);
        log.debug("Pagination demandée - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());

        Specification<Decompte> spec = DecompteSpec.bySearchCriteria(searchDTO);
        Page<Decompte> decomptes = decompteRepository.findAll(spec, pageable);

        log.info("Recherche terminée - {} résultat(s) trouvé(s) sur {} élément(s) au total", decomptes.getNumberOfElements(), decomptes.getTotalElements());

        return decomptes.map(decompteMapper::toDto);
    }

    @Transactional
    public DecompteResponseDto toglleStatus(Long id ) {
        log.info("Changement du statut du décompte ID: {}", id);

        Decompte decompte = decompteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Décompte non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("décompte not found with ID: " + id);
                });

        Status existingStatus = decompte.getStatus();
        Status newStatus = existingStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        log.debug("Changement de statut - Ancien: {}, Nouveau: {}", existingStatus, newStatus);

        decompte.setStatus(newStatus);
        Decompte updatedDecompte = decompteRepository.save(decompte);

        log.info("Statut du décompte ID: {} modifié avec succès de {} vers {}",
                id, existingStatus, newStatus);

        return decompteMapper.toDto(updatedDecompte);
    }

    @Transactional(readOnly = true)
    public EncoursDecompteDto getDecomptesEncoursForBanque(EncoursDecompteSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de décomptes pour la banque ID: {}", banqueId);
        log.debug("Critères de recherche: {}", searchDto);

        // Build the specification to filter the records
        Specification<Decompte> spec = DecompteSpec.byBanqueId(banqueId)
                .and(DecompteSpec.outstandingDecomptesForCurrentMonth())
                .and(DecompteSpec.byMarcheName(searchDto.getMarcheName()))
                .and(DecompteSpec.byMarcheNumero(searchDto.getMarcheNumero()))
                .and(DecompteSpec.hasMontant(searchDto.getMontant()));

        // Fetch the paginated list of decomptes  records
        Page<Decompte> decomptePage = decompteRepository.findAll(spec, pageable);

        log.debug("Nombre de résultats paginés: {}", decomptePage.getNumberOfElements());

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = decompteRepository.findAll(spec).stream()
                .map(Decompte::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours de décomptes calculé: {} pour {} enregistrement(s)",
                totalDesEncours, decomptePage.getTotalElements());

        // Map the page of entities to a page of DTOs
        Page<DecompteResponseDto> pageDto = decomptePage.map(decompteMapper::toDto);

        // Create and return the final DTO
        EncoursDecompteDto result = new EncoursDecompteDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }

}
