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
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.FileStorageException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Senior Implementation of BanqueService.
 *
 * Architecture: Unidirectional JPA relationships, Immutable DTOs (Records),
 * and Atomic mapping (Data calculated before DTO construction).
 */
@Service
@Transactional(readOnly = true)
public class BanqueServiceImpl implements IBanqueService {

    private static final Logger logger = LoggerFactory.getLogger(BanqueServiceImpl.class);
    private static final String FC_CODE = "FC";

    private final BanqueRepository banqueRepository;
    private final BanqueMapper banqueMapper;
    private final NatureLigneFonctionnelleRepository natureLFRepository;
    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    private final TransactionRepository transactionRepository;

    public BanqueServiceImpl(BanqueRepository banqueRepository,
                             BanqueMapper banqueMapper,
                             NatureLigneFonctionnelleRepository natureLFRepository,
                             LigneFonctionnelleRepository ligneFonctionnelleRepository,
                             TransactionRepository transactionRepository) {
        this.banqueRepository = banqueRepository;
        this.banqueMapper = banqueMapper;
        this.natureLFRepository = natureLFRepository;
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        this.transactionRepository = transactionRepository;
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

        // At creation, balance is usually soldeDepart as there are no transactions yet
        BigDecimal balance = computeLiveBalance(saved.getId(), saved.getSoldeDepart());
        return banqueMapper.toDto(saved, balance);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Override
    public BanqueResponse getBanqueById(Long id) {
        Banque banque = findOrThrow(id);
        BigDecimal balance = computeLiveBalance(id, banque.getSoldeDepart());
        return banqueMapper.toDto(banque, balance);
    }

    @Override
    public Page<BanqueResponse> getAllBanques(BanqueSearchCriteria criteria, Pageable pageable) {
        Specification<Banque> spec = BanqueSpecification.searchByCriteria(criteria);
        return banqueRepository.findAll(spec, pageable)
                .map(banque -> {
                    BigDecimal balance = computeLiveBalance(banque.getId(), banque.getSoldeDepart());
                    return banqueMapper.toDto(banque, balance);
                });
    }

    @Override
    public List<BankAcceuilResponse> getAccueilBankInfo() {
        List<Banque> banques = banqueRepository.findAll();
        Long fcNatureId = findFaciliteDeCaisseNatureId();

        return banques.stream()
                .map(banque -> mapToAccueilResponse(banque, fcNatureId))
                .collect(Collectors.toList());
    }

    @Override
    public BankAcceuilResponse getAccueilBankInfoById(Long banqueId) {
        Banque banque = findOrThrow(banqueId);
        Long fcNatureId = findFaciliteDeCaisseNatureId();
        return mapToAccueilResponse(banque, fcNatureId);
    }

    @Override
    public BigDecimal getPlafondFC(Long banqueId) {
        Long fcNatureId = findFaciliteDeCaisseNatureId();
        return getPlafondFCInternal(banqueId, fcNatureId);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BanqueResponse updateBanque(Long id, BanqueRequest request, MultipartFile logoFile) {
        logger.info("Updating banque ID: {}", id);

        // 1. Fetch the existing entity
        Banque banque = findOrThrow(id);

        // 2. Manual Mapping / Business Logic
        // This gives you the power to add logic (e.g., "Don't allow account number changes")
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
        BigDecimal balance = computeLiveBalance(id, updated.getSoldeDepart());

        logger.info("Banque ID: {} successfully updated.", id);
        return banqueMapper.toDto(updated, balance);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteBanque(Long id) {
        Banque banque = findOrThrow(id);

        if (ligneFonctionnelleRepository.existsByBanqueIdAndActifTrue(id)) {
            throw new BanqueDeletionException("La banque possède des lignes fonctionnelles actives.");
        }

        if (transactionRepository.countPendingByBanque(id) > 0) {
            throw new BanqueDeletionException("Transactions en attente détectées.");
        }

        BigDecimal balance = computeLiveBalance(id, banque.getSoldeDepart());
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new BanqueDeletionException("Le solde doit être nul avant suppression.");
        }

        banqueRepository.delete(banque);
    }

    // ── Private Senior Helpers ────────────────────────────────────────────────

    private BankAcceuilResponse mapToAccueilResponse(Banque banque, Long fcNatureId) {
        BigDecimal balance = computeLiveBalance(banque.getId(), banque.getSoldeDepart());
        BigDecimal plafond = getPlafondFCInternal(banque.getId(), fcNatureId);
        return banqueMapper.toAcceuilDto(banque, balance, plafond);
    }

    private BigDecimal getPlafondFCInternal(Long banqueId, Long fcNatureId) {
        return ligneFonctionnelleRepository
                .findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banqueId, fcNatureId)
                .map(LigneFonctionnelle::getPlafond)
                .orElse(BigDecimal.ZERO);
    }

    private Long findFaciliteDeCaisseNatureId() {
        return natureLFRepository.findByCode(FC_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Code système FC introuvable."))
                .getId();
    }

    private BigDecimal computeLiveBalance(Long banqueId, BigDecimal soldeDepart) {
        return transactionRepository.computeBalance(banqueId).orElse(soldeDepart);
    }

    private Banque findOrThrow(Long id) {
        return banqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée ID: " + id));
    }
}