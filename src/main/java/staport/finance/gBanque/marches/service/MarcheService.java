package staport.finance.gBanque.marches.service;

import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.dtos.request.MarcheRequestDTO;
import staport.finance.gBanque.marches.dtos.request.MarcheSearchDTO;
import staport.finance.gBanque.marches.dtos.response.MarcheResponseDTO;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.exceptions.MarcheBusinessException;
import staport.finance.gBanque.marches.mapper.MarcheMapper;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.marches.specs.MarcheSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Façade publique du module Marches (architecture.md, "Encapsulation des domaines").
 *
 * <p>N'injecte volontairement <b>aucune</b> dépendance vers {@code IBanqueService}/
 * {@code BanqueRepository} : la {@link Banque} nécessaire à {@link #createMarche}/
 * {@link #updateMarche} est résolue en amont par {@code MarcheController} (qui injecte
 * {@code IBanqueService}) et passée déjà chargée. Ce choix n'est pas cosmétique — il évite un
 * cycle de bean Spring au démarrage : {@code IBanqueService} (impl {@code BanqueServiceImpl})
 * dépend de {@code ILigneFonctionnelleService} (impl {@code LigneFonctionnelleService}), qui
 * dépend elle-même de {@code IMarcheService}. Si {@code MarcheService} dépendait à son tour de
 * {@code IBanqueService}, le cycle {@code MarcheService → IBanqueService →
 * LigneFonctionnelleService → IMarcheService → MarcheService} empêcherait le démarrage de
 * l'application ({@code ApplicationContext} failed to start). Comme {@code MarcheController}
 * n'est injecté par aucun autre bean, résoudre l'entité à ce niveau ne peut jamais fermer de
 * cycle — même pattern que {@code ValidationController} pour {@code ContratLeasing}/
 * {@code CreditMoyenTerme}.
 */
@Service
@Transactional
public class MarcheService implements IMarcheService {

    private static final Logger log = LoggerFactory.getLogger(MarcheService.class);

    private final MarcheRepository marcheRepository;
    private final MarcheMapper marcheMapper;

    public MarcheService(MarcheRepository marcheRepository, MarcheMapper marcheMapper) {
        this.marcheRepository = marcheRepository;
        this.marcheMapper = marcheMapper;
    }

    /**
     * Résout un Marche par ID pour les domaines qui ont besoin de l'entité JPA elle-même
     * (ex. relation ManyToMany côté {@code LigneFonctionnelle}) — jamais via
     * {@code MarcheRepository} directement (encapsulation des domaines, architecture.md).
     */
    @Transactional(readOnly = true)
    public Marche findOrThrow(Long id) {
        return marcheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marché non trouvé avec l'ID: " + id));
    }

    /**
     * Vérifie l'existence d'un Marche par ID, pour les domaines qui n'ont besoin que d'une
     * validation (pas de l'entité elle-même).
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return marcheRepository.existsById(id);
    }

    /**
     * Retourne les marchés <b>actifs</b> rattachés à une banque, pour les domaines qui ont
     * besoin de parcourir les marchés de la banque (ex. calculateurs d'agios) — jamais via
     * {@code MarcheRepository} directement (encapsulation des domaines, architecture.md).
     *
     * <p>Un marché archivé est exclu définitivement de ce résultat : il ne pèse plus dans le
     * calcul des agios ni dans l'encours des lignes fonctionnelles.</p>
     */
    @Transactional(readOnly = true)
    public List<Marche> findByBanqueId(Long banqueId) {
        return marcheRepository.findByBanqueIdAndActifTrue(banqueId);
    }

    /**
     * Crée un nouveau marché. La {@link Banque} est déjà résolue par l'appelant (voir javadoc
     * de la classe).
     */
    @AuditableAction(operation = AuditOperation.CREATE, entity = Marche.class)
    public MarcheResponseDTO createMarche(MarcheRequestDTO requestDTO, Banque banque) {
        log.info("Création d'un nouveau marché - numéro: {}", requestDTO.numero());

        if (marcheRepository.existsByNumero(requestDTO.numero())) {
            log.warn("Tentative de création d'un marché avec un numéro déjà existant: {}", requestDTO.numero());
            throw new DuplicateEntryException("Un marché avec ce numéro existe déjà.");
        }

        validerDates(requestDTO);

        Marche marche = marcheMapper.toEntity(requestDTO, banque);
        Marche savedMarche = marcheRepository.save(marche);
        log.info("Marché créé avec succès - id: {}", savedMarche.getId());
        return marcheMapper.toResponseDTO(savedMarche);
    }

    /**
     * Récupère un marché par son ID, avec sa banque chargée en une seule requête
     * ({@code findByIdAvecBanque}).
     */
    @Transactional(readOnly = true)
    public MarcheResponseDTO getMarcheById(Long id) {
        log.info("Récupération du marché - id: {}", id);
        Marche marche = marcheRepository.findByIdAvecBanque(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marché non trouvé avec l'ID: " + id));
        return marcheMapper.toResponseDTO(marche);
    }

    /**
     * Met à jour un marché existant. {@code banque} est {@code null} si le marché ne change pas
     * de banque (champ optionnel à la mise à jour) — voir javadoc de la classe pour la raison de
     * cette résolution en amont.
     */
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Marche.class)
    public MarcheResponseDTO updateMarche(Long id, MarcheRequestDTO requestDTO, Banque banque) {
        log.info("Mise à jour du marché - id: {}", id);

        Marche existingMarche = findOrThrow(id);

        if (marcheRepository.existsByNumeroAndIdNot(requestDTO.numero(), id)) {
            log.warn("Tentative de mise à jour du marché {} avec un numéro déjà existant: {}", id, requestDTO.numero());
            throw new DuplicateEntryException("Un marché avec ce numéro existe déjà.");
        }

        validerDates(requestDTO);

        marcheMapper.applyUpdate(existingMarche, requestDTO);
        if (banque != null) {
            existingMarche.setBanque(banque);
        }

        Marche updatedMarche = marcheRepository.save(existingMarche);
        log.info("Marché mis à jour avec succès - id: {}", updatedMarche.getId());
        return marcheMapper.toResponseDTO(updatedMarche);
    }

    /**
     * Supprime définitivement un marché.
     * Suppression interdite si le marché a des données liées (décomptes,
     * préfinancements, droits constatés, factorings, lignes fonctionnelles spécifiques) — ces
     * enfants seraient sinon supprimés en cascade ({@code cascade = ALL, orphanRemoval = true}
     * sur {@code Marche}), détruisant un historique financier réel. Dans ce cas, utiliser
     * {@link #archiverMarche(Long)} à la place.
     */
    @AuditableAction(operation = AuditOperation.DELETE, entity = Marche.class)
    public void deleteMarche(Long id) {
        log.info("Suppression du marché - id: {}", id);

        Marche marche = findOrThrow(id);

        boolean aDesDonneesLiees =
                !marche.getDecomptes().isEmpty()
                        || !marche.getPrefinancements().isEmpty()
                        || !marche.getDroitConstates().isEmpty()
                        || !marche.getFactorings().isEmpty()
                        || !marche.getLignesFonctionnellesSpecifiques().isEmpty();

        if (aDesDonneesLiees) {
            log.warn("Suppression du marché {} refusée: des données financières y sont rattachées", id);
            throw new MarcheBusinessException(
                    "Suppression interdite: ce marché a des décomptes, préfinancements, droits constatés, "
                            + "factorings ou lignes spécifiques rattachés. Utilisez l'archivage à la place.");
        }

        marcheRepository.deleteById(id);
        log.info("Marché supprimé avec succès - id: {}", id);
    }

    /**
     * Archive un marché : il devient invisible pour les calculs futurs (agios, encours des
     * lignes fonctionnelles) mais reste consultable et son historique n'est pas modifié.
     */
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Marche.class)
    public MarcheResponseDTO archiverMarche(Long id) {
        log.info("Archivage du marché - id: {}", id);

        Marche marche = findOrThrow(id);
        marche.setActif(false);
        Marche saved = marcheRepository.save(marche);
        log.info("Marché archivé avec succès - id: {}", id);
        return marcheMapper.toResponseDTO(saved);
    }

    /**
     * Désarchive un marché : il redevient pris en compte dans les calculs futurs (agios,
     * encours des lignes fonctionnelles).
     */
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Marche.class)
    public MarcheResponseDTO desarchiverMarche(Long id) {
        log.info("Désarchivage du marché - id: {}", id);

        Marche marche = findOrThrow(id);
        marche.setActif(true);
        Marche saved = marcheRepository.save(marche);
        log.info("Marché désarchivé avec succès - id: {}", id);
        return marcheMapper.toResponseDTO(saved);
    }

    /**
     * Recherche paginée, triée et filtrable des marchés.
     */
    @Transactional(readOnly = true)
    public Page<MarcheResponseDTO> findAllMarches(MarcheSearchDTO searchDTO, Pageable pageable) {
        log.info("Recherche de marchés paginée - page: {}, taille: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Specification<Marche> spec = MarcheSpecification.searchByCriteria(searchDTO);

        Page<Marche> marchePage = marcheRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        return marchePage.map(marcheMapper::toResponseDTO);
    }

    /**
     * Vérifie que la date d'achèvement des travaux n'est pas antérieure à la date de début.
     * Centralisée ici pour être appliquée identiquement à la création et à la mise à jour.
     */
    private void validerDates(MarcheRequestDTO requestDTO) {
        if (requestDTO.dateDebutDesTravaux() != null && requestDTO.dateDAchievementDesTravaux() != null
                && requestDTO.dateDAchievementDesTravaux().isBefore(requestDTO.dateDebutDesTravaux())) {
            log.warn("Dates invalides pour le marché {}: date de fin avant la date de début", requestDTO.numero());
            throw new MarcheBusinessException("La date d'achèvement des travaux ne peut pas être avant la date de début.");
        }
    }
}
