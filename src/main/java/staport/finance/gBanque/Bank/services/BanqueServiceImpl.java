package staport.finance.gBanque.Bank.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.Bank.dtos.request.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.request.BanqueSearchCriteria;
import staport.finance.gBanque.Bank.dtos.response.BanqueResponse;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.events.BanqueCreatedEvent;
import staport.finance.gBanque.Bank.exceptions.BanqueDeletionException;
import staport.finance.gBanque.Bank.mappers.BanqueMapper;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.Bank.specifications.BanqueSpecification;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.ILigneFonctionnelleService;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.FileStorageException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.transactions.service.ITransactionService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Service
@Transactional(readOnly = true)
public class BanqueServiceImpl implements IBanqueService {

    private static final Logger log = LoggerFactory.getLogger(BanqueServiceImpl.class);

    private final BanqueRepository banqueRepository;
    private final BanqueMapper banqueMapper;

    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;
    private final ITransactionService transactionService;
    private final ILigneFonctionnelleService ligneFonctionnelleService;
    private final ApplicationEventPublisher eventPublisher;


    public BanqueServiceImpl(BanqueRepository banqueRepository,
                             BanqueMapper banqueMapper,
                             INatureLigneFonctionnelleService natureLigneFonctionnelleService,
                             ITransactionService transactionService,
                             ILigneFonctionnelleService ligneFonctionnelleService,
                             ApplicationEventPublisher eventPublisher) {
        this.banqueRepository = banqueRepository;
        this.banqueMapper = banqueMapper;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.ligneFonctionnelleService = ligneFonctionnelleService;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Banque.class)
    public BanqueResponse createBanque(BanqueRequest request, MultipartFile logoFile) {
        log.info("Création banque: {}", request.nom());

        if (banqueRepository.findByNumeroDeCompte(request.numeroDeCompte()).isPresent()) {
            throw new DuplicateEntryException("Le numéro de compte existe déjà.");
        }

        if (logoFile == null || logoFile.isEmpty()) {
            throw new FileStorageException("Le logo est obligatoire.");
        }

        Banque banque = banqueMapper.toEntity(request);
        try {
            banque.setLogoData(logoFile.getBytes());
        } catch (IOException e) {
            throw new FileStorageException("Erreur lecture logo.", e);
        }

        Banque saved = banqueRepository.save(banque);

        // Le domaine Rapprochement crée son premier rapprochement EN_COURS en réaction à
        // cet event — Bank ne dépend jamais de RapprochementService directement (celui-ci
        // dépend déjà de IBanqueService, une dépendance dans l'autre sens créerait un cycle).
        eventPublisher.publishEvent(new BanqueCreatedEvent(saved.getId()));

        BigDecimal balance = computeLiveBalance(saved.getId());
        return banqueMapper.toDto(saved, balance);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Override
    public BanqueResponse getBanqueById(Long id) {
        Banque banque = findOrThrow(id);
        BigDecimal balance = computeLiveBalance(id);
        return banqueMapper.toDto(banque, balance);
    }

    @Override
    public Page<BanqueResponse> getAllBanques(BanqueSearchCriteria criteria, Pageable pageable) {
        // 1. Recherche paginée (1 requête avec Join/Where/Limit)
        Specification<Banque> spec = BanqueSpecification.searchByCriteria(criteria);
        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
        Page<Banque> banquePage = banqueRepository.findAll(spec, effective);

        // 2. Extraction des IDs de la page courante
        List<Long> ids = banquePage.getContent().stream()
                .map(Banque::getId)
                .toList();

        // 3. Chargement batch des soldes (1 requête)
        Map<Long, BigDecimal> balanceMap = transactionService.computeBalancesForBanks(ids);

        // 4. Mapping vers les DTOs
        return banquePage.map(banque -> {
            BigDecimal balance = balanceMap.get(banque.getId());
            return banqueMapper.toDto(banque, balance);
        });
    }

    @Override
    public BigDecimal getPlafondFC(Long banqueId) {
        Long fcNatureId = natureLigneFonctionnelleService.findFaciliteDeCaisseNatureId();
        return this.ligneFonctionnelleService.getPlafondFCInternal(banqueId, fcNatureId);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Banque.class)
    public BanqueResponse updateBanque(Long id, BanqueRequest request, MultipartFile logoFile) {
        log.info("Mise à jour banque ID: {}", id);

        Banque banque = findOrThrow(id);

        // Le numéro de compte (RIB) est immuable — toute tentative de modification est rejetée.
        if (!banque.getNumeroDeCompte().equals(request.numeroDeCompte())) {
            throw new DuplicateEntryException("Le numéro de compte ne peut pas être modifié.");
        }

        banque.setNom(request.nom());
        banque.setDevise(request.devise());
        banque.setAdresse(request.adresse());
        banque.setAgence(request.agence());

        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                banque.setLogoData(logoFile.getBytes());
            } catch (IOException e) {
                throw new FileStorageException("Erreur lors de la lecture du nouveau logo.", e);
            }
        }

        Banque updated = banqueRepository.save(banque);
        BigDecimal balance = computeLiveBalance(id);

        log.info("Banque ID: {} mise à jour avec succès.", id);
        return banqueMapper.toDto(updated, balance);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Banque.class)
    public void deleteBanque(Long id) {
        log.info("Suppression banque ID: {}", id);
        Banque banque = findOrThrow(id);

        // Guard 1 — Lignes fonctionnelles actives
        if (ligneFonctionnelleService.hasActiveLignesForBanque(id)) {
            log.warn("Suppression banque ID: {} bloquée — lignes fonctionnelles actives", id);
            throw new BanqueDeletionException("Des lignes fonctionnelles actives sont associées à cette banque.");
        }

        // Guard 2 — Transactions en attente
        if (transactionService.countPendingTransactions(id) > 0) {
            log.warn("Suppression banque ID: {} bloquée — transactions en attente", id);
            throw new BanqueDeletionException("Transactions en attente détectées.");
        }

        // Guard 3 — Solde non nul
        BigDecimal balance = computeLiveBalance(id);
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            log.warn("Suppression banque ID: {} bloquée — solde non nul", id);
            throw new BanqueDeletionException("Le solde doit être nul avant suppression.");
        }

        banqueRepository.delete(banque);
        log.info("Banque ID: {} supprimée.", id);
    }

    @Override
    public List<Long> getAllBanqueIds() {
        log.debug("Récupération de tous les IDs de banques");
        return banqueRepository.findAllIds();
    }

    @Override
    public Banque findOrThrow(Long id) {
        return banqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée ID: " + id));
    }

    @Override
    public List<Banque> getAllBanqueEntities() {
        return banqueRepository.findAll();
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    private BigDecimal computeLiveBalance(Long banqueId) {
        return transactionService.computeBalance(banqueId);
    }

}
