package staport.finance.gBanque.Bank.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import staport.finance.gBanque.Bank.dtos.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.dtos.BanqueSearchCriteria;
import staport.finance.gBanque.Bank.entities.Banque;
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
import java.util.stream.Collectors;

import static staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService.FC_CODE;


@Service
@Transactional(readOnly = true)
public class BanqueServiceImpl implements IBanqueService {

    private static final Logger logger = LoggerFactory.getLogger(BanqueServiceImpl.class);

    private final BanqueRepository banqueRepository;
    private final BanqueMapper banqueMapper;

    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;
    private final ITransactionService transactionService;
    private final ILigneFonctionnelleService ligneFonctionnelleService;


    public BanqueServiceImpl(BanqueRepository banqueRepository,
                             BanqueMapper banqueMapper, INatureLigneFonctionnelleService natureLigneFonctionnelleService,
                             ITransactionService transactionService, ILigneFonctionnelleService ligneFonctionnelleService) {
        this.banqueRepository = banqueRepository;
        this.banqueMapper = banqueMapper;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.ligneFonctionnelleService = ligneFonctionnelleService;
        this.transactionService = transactionService;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
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
        Page<Banque> banquePage = banqueRepository.findAll(spec, pageable);

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
    public List<BankAcceuilResponse> getAccueilBankInfo() {
        // Query 1: Fetch Banks
        List<Banque> banques = banqueRepository.findAll();
        List<Long> ids = banques.stream().map(Banque::getId).toList();

        // Query 2: Batch fetch  balances
        Map<Long, BigDecimal> balanceMap = transactionService.computeBalancesForBanks(ids);

        // Query 3: Batch fetch active FC
        Map<Long, BigDecimal> plafondMap = ligneFonctionnelleService.getActivePlafondsForBanks(ids, FC_CODE);

        // Assembly: Logic stays in Java, DB stays fast
        return banques.stream()
                .map(b -> {
                    BigDecimal balance = balanceMap.get(b.getId());
                    BigDecimal plafond = plafondMap.get(b.getId());

                    return banqueMapper.toAcceuilDto(b, balance, plafond);
                })
                .collect(Collectors.toList());
    }

    @Override
    public BankAcceuilResponse getAccueilBankInfoById(Long banqueId) {
        Banque banque = findOrThrow(banqueId);
        Long fcNatureId = natureLigneFonctionnelleService.findFaciliteDeCaisseNatureId();
        return mapToAccueilResponse(banque, fcNatureId);
    }

    @Override
    public BigDecimal getPlafondFC(Long banqueId) {
        Long fcNatureId = natureLigneFonctionnelleService.findFaciliteDeCaisseNatureId();
        return this.ligneFonctionnelleService.getPlafondFCInternal(banqueId, fcNatureId);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
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
    @Transactional
    public void setSoldeDepart(Long banqueId,BigDecimal newSoldeDepart) {
        Banque banque = findOrThrow(banqueId);
        banque.setSoldeDepart(newSoldeDepart);
        banqueRepository.save(banque);
    }



    // ── Private Senior Helpers ────────────────────────────────────────────────

    private BankAcceuilResponse mapToAccueilResponse(Banque banque, Long fcNatureId) {
        Long banqueId=banque.getId();
        BigDecimal balance = computeLiveBalance(banqueId);
        BigDecimal plafond =this.ligneFonctionnelleService.getPlafondFCInternal(banqueId, fcNatureId);
        return banqueMapper.toAcceuilDto(banque, balance, plafond);
    }


    private BigDecimal computeLiveBalance(Long banqueId) {
        return transactionService.computeBalance(banqueId);
    }

    @Override
    public Banque findOrThrow(Long id) {
        return banqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée ID: " + id));
    }
}