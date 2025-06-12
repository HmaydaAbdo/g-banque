package staport.finance.gBanque.LigneFonctionnelle.services;

import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.mappers.LigneFonctionnelleMapper;
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifications.LigneFonctionnelleSpecification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class LigneFonctionnelleService {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleService.class);

    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    private final LigneFonctionnelleMapper ligneFonctionnelleMapper;
    private final BanqueRepository banqueRepository;
    private final NatureLigneFonctionnelleService natureLigneFonctionnelleService;

    @Autowired
    public LigneFonctionnelleService(LigneFonctionnelleRepository ligneFonctionnelleRepository,
                                     LigneFonctionnelleMapper ligneFonctionnelleMapper,
                                     BanqueRepository banqueRepository,
                                     NatureLigneFonctionnelleService natureLigneFonctionnelleService) {
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        this.ligneFonctionnelleMapper = ligneFonctionnelleMapper;
        this.banqueRepository = banqueRepository;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
    }

    @Transactional
    public LigneFonctionnelleResponse createLigneFonctionnelle(LigneFonctionnelleRequest request) {
        log.debug("Requête de création de ligne fonctionnelle: {}", request);
        validateDateRange(request.getDateDepart(), request.getDateFin());

        // Récupérer la banque et la nature pour l'entité LigneFonctionnelle
        Banque banque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId()));
        log.debug("Banque trouvée: {}", banque.getNom());

        NatureLigneFonctionnelle natureLigneFonctionnelle = natureLigneFonctionnelleService.getNatureLigneFonctionnelleEntityById(request.getNatureLigneFonctionnelleId());
        log.debug("NatureLigneFonctionnelle trouvée: {}", natureLigneFonctionnelle.getLabel());


        // La contrainte DB gérera les cas concurrents ou les erreurs qui passeraient ici.
        if (ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleId(request.getBanqueId(), request.getNatureLigneFonctionnelleId()).isPresent()) {
            log.warn("Tentative de création d'un doublon (fail-fast) pour la combinaison: Banque '{}' (ID: {}) et Nature '{}' (ID: {}).",
                    banque.getNom(), request.getBanqueId(), natureLigneFonctionnelle.getLabel(), request.getNatureLigneFonctionnelleId());
            throw new DuplicateEntryException(
                    "Une ligne fonctionnelle de type '" + natureLigneFonctionnelle.getLabel() +
                            "' existe déjà pour la banque '" + banque.getNom() + "'."
            );
        }

        LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleMapper.toEntity(request);
        ligneFonctionnelle.setBanque(banque);
        ligneFonctionnelle.setNatureLigneFonctionnelle(natureLigneFonctionnelle);

        try {
            LigneFonctionnelle savedLigneFonctionnelle = ligneFonctionnelleRepository.save(ligneFonctionnelle);
            log.info("Ligne fonctionnelle créée avec succès, ID: {}", savedLigneFonctionnelle.getId());
            return ligneFonctionnelleMapper.toDto(savedLigneFonctionnelle);
        } catch (DataIntegrityViolationException e) {
            // Cette exception est levée par la contrainte UNIQUE de la base de données
            log.error("Erreur d'intégrité des données (doublon) lors de la création de la ligne fonctionnelle: {}", e.getMessage(), e);
            throw new DuplicateEntryException(
                    "Impossible de créer la ligne fonctionnelle. La combinaison Banque '" + banque.getNom() +
                            "' et Nature '" + natureLigneFonctionnelle.getLabel() + "' existe déjà." );
        }
    }

    public LigneFonctionnelleResponse getLigneFonctionnelleById(Long id) {
        log.debug("Récupération de ligne fonctionnelle par ID: {}", id);
        LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Ligne Fonctionnelle non trouvée avec l'ID: {}", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });
        log.debug("Ligne fonctionnelle trouvée: {}", ligneFonctionnelle.getId());
        return ligneFonctionnelleMapper.toDto(ligneFonctionnelle);
    }

    public Page<LigneFonctionnelleResponse> getCurrentLigneFonctionnelles(LigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        log.debug("Récupération de toutes les lignes fonctionnelles avec critères: {} et pageable: {}", criteria, pageable);
        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable);
        log.debug("Nombre de lignes fonctionnelles trouvées: {}", ligneFonctionnellesPage.getTotalElements());
        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }

    public List<LigneFonctionnelleResponse> getLignesFonctionnellesByBanqueId(Long banqueId) {
        log.debug("Récupération des lignes fonctionnelles par Banque ID: {}", banqueId);
        if (!banqueRepository.existsById(banqueId)) {
            log.warn("Banque non trouvée avec l'ID: {}", banqueId);
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId + ". Impossible de récupérer les lignes fonctionnelles.");
        }
        List<LigneFonctionnelle> lignesFonctionnelles = ligneFonctionnelleRepository.findByBanqueId(banqueId);
        log.debug("Nombre de lignes fonctionnelles trouvées pour la banque {}: {}", banqueId, lignesFonctionnelles.size());
        return ligneFonctionnelleMapper.toDtoList(lignesFonctionnelles);
    }

    public Optional<LigneFonctionnelleResponse> getLigneFonctionnellesByBanqueIdAndNature(Long banqueId, Long natureLigneFonctionnelleId){
        log.debug("Recherche de ligne fonctionnelle par Banque ID: {} et Nature ID: {}", banqueId, natureLigneFonctionnelleId);
        return ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleId(banqueId, natureLigneFonctionnelleId)
                .map(ligneFonctionnelleMapper::toDto);
    }

    @Transactional
    public LigneFonctionnelleResponse updateLigneFonctionnelle(Long id, LigneFonctionnelleRequest request) {
        log.debug("--- Début de la mise à jour de LigneFonctionnelle (ID: {}) ---", id);
        log.debug("Requête reçue: {}", request);

        LigneFonctionnelle existingLigneFonctionnelle = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Ligne Fonctionnelle non trouvée avec l'ID: {} pour la mise à jour.", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });
        log.debug("Ligne fonctionnelle existante (avant toute modification): {}", existingLigneFonctionnelle.toString());

        validateDateRange(request.getDateDepart(), request.getDateFin());

        // Récupérer les entités Banque et NatureLigneFonctionnelle basées sur la requête
        Banque targetBanque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> {
                    log.warn("Banque cible non trouvée avec l'ID: {}.", request.getBanqueId());
                    return new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId());
                });
        log.debug("Banque cible trouvée: {}", targetBanque.getNom());

        NatureLigneFonctionnelle targetNature = natureLigneFonctionnelleService.getNatureLigneFonctionnelleEntityById(request.getNatureLigneFonctionnelleId());
        log.debug("NatureLigneFonctionnelle cible trouvée: {}", targetNature.getLabel());

        // Appliquer les mises à jour des champs simples via le mapper
        // Cela met à jour les champs du DTO dans l'entité existante, SAUF les relations
        ligneFonctionnelleMapper.updateLigneFonctionnelleFromDto(request, existingLigneFonctionnelle);

        // Définir les entités Banque et NatureLigneFonctionnelle gérées par JPA
        // C'est ici que les références aux entités gérées sont mises à jour
        existingLigneFonctionnelle.setBanque(targetBanque);
        existingLigneFonctionnelle.setNatureLigneFonctionnelle(targetNature);

        try {
            // Sauvegarder l'entité mise à jour.
            // Si la combinaison (banque_id, nature_ligne_id) est dupliquée (par rapport à une AUTRE ligne),
            // une DataIntegrityViolationException sera levée ici par la DB.
            LigneFonctionnelle updatedLigneFonctionnelle = ligneFonctionnelleRepository.save(existingLigneFonctionnelle);
            log.info("Ligne fonctionnelle avec ID {} mise à jour avec succès.", updatedLigneFonctionnelle.getId());
            log.debug("--- Fin de la mise à jour de LigneFonctionnelle (ID: {}) ---", updatedLigneFonctionnelle.getId());
            return ligneFonctionnelleMapper.toDto(updatedLigneFonctionnelle);
        } catch (DataIntegrityViolationException e) {
            log.error("Erreur d'intégrité des données (doublon) lors de la mise à jour de la ligne fonctionnelle (ID: {}): {}", id, e.getMessage(), e);
            // On lève notre exception métier avec un message clair
            throw new DuplicateEntryException(
                    "Impossible de mettre à jour la ligne fonctionnelle. La combinaison Banque '" + targetBanque.getNom() +
                            "' et Nature '" + targetNature.getLabel() + "' existe déjà.");
        }
    }

    @Transactional
    public void deleteLigneFonctionnelle(Long id) {
        log.debug("Requête de suppression de ligne fonctionnelle pour ID: {}", id);
        if (!ligneFonctionnelleRepository.existsById(id)) {
            log.warn("Ligne Fonctionnelle non trouvée avec l'ID: {} pour la suppression.", id);
            throw new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
        }
        ligneFonctionnelleRepository.deleteById(id);
        log.info("Ligne fonctionnelle avec ID {} supprimée avec succès.", id);
    }

    private void validateDateRange(OffsetDateTime dateDepart, OffsetDateTime dateFin) {
        if (dateDepart != null && dateFin != null && dateFin.isBefore(dateDepart)) {
            log.warn("Validation de date échouée: dateFin ({}) est antérieure à dateDepart ({}).", dateFin, dateDepart);
            throw new IllegalArgumentException("La date de fin (" + dateFin + ") ne peut pas être antérieure à la date de départ (" + dateDepart + ").");
        }
    }
}
