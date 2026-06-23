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
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.dtos.BanqueSearchCriteria;
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

    private static final Logger logger = LoggerFactory.getLogger(BanqueServiceImpl.class);

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
        logger.info("Creating banque: {}", request.nom());

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
        // 1. Execute the search and pagination (1 Query with Join/Where/Limit)
        Specification<Banque> spec = BanqueSpecification.searchByCriteria(criteria);
        Page<Banque> banquePage = banqueRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        if (banquePage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2. Extract IDs from the CURRENT page
        List<Long> ids = banquePage.getContent().stream()
                .map(Banque::getId)
                .toList();

        // 3. Batch fetch balances for these specific IDs (1 Query)
        Map<Long, BigDecimal> balanceMap = transactionService.computeBalancesForBanks(ids);

        // 4. Map to DTOs using the pre-calculated Map
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
        logger.info("Updating banque ID: {}", id);

        // 1. Fetch the existing entity
        Banque banque = findOrThrow(id);

        // 2. Manual Mapping / Business Logic

        banque.setNom(request.nom());
        banque.setDevise(request.devise());
        banque.setAddress(request.address());
        banque.setAgence(request.agence());


        // 3. Handle File Upload
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                banque.setLogoData(logoFile.getBytes());
            } catch (IOException e) {
                throw new FileStorageException("Erreur lors de la lecture du nouveau logo.", e);
            }
        }

        // 4. Save (triggers @PreUpdate and Audit)
        Banque updated = banqueRepository.save(banque);

        // 5. Atomic Response construction
        BigDecimal balance = computeLiveBalance(id);

        logger.info("Banque ID: {} successfully updated.", id);
        return banqueMapper.toDto(updated, balance);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Banque.class)
    public void deleteBanque(Long id) {
        Banque banque = findOrThrow(id);


        if (transactionService.countPendingTransactions(id) > 0) {
            throw new BanqueDeletionException("Transactions en attente détectées.");
        }

        BigDecimal balance = computeLiveBalance(id);
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new BanqueDeletionException("Le solde doit être nul avant suppression.");
        }

        banqueRepository.delete(banque);
    }

    @Override
    public List<Long> getAllBanqueIds() {
        logger.debug("Fetching all banque IDs");
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

    // ── Private Senior Helpers ────────────────────────────────────────────────

    private BigDecimal computeLiveBalance(Long banqueId) {
        return transactionService.computeBalance(banqueId);
    }

}