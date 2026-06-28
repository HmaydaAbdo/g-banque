package staport.finance.gBanque.commision.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.commision.dtos.CommissionConfigRequestDTO;
import staport.finance.gBanque.commision.dtos.CommissionConfigResponseDTO;
import staport.finance.gBanque.commision.entities.CommissionConfig;
import staport.finance.gBanque.commision.enums.CommissionCondition;
import staport.finance.gBanque.commision.mapper.CommissionConfigMapper;
import staport.finance.gBanque.commision.repository.CommissionConfigRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CommissionConfigService implements ICommissionConfigService {

    private static final Logger log = LoggerFactory.getLogger(CommissionConfigService.class);

    private final CommissionConfigRepository repository;
    private final CommissionConfigMapper mapper;
    private final IBanqueService banqueService;

    public CommissionConfigService(CommissionConfigRepository repository,
                                   CommissionConfigMapper mapper,
                                   IBanqueService banqueService) {
        this.repository    = repository;
        this.mapper        = mapper;
        this.banqueService = banqueService;
    }

    // ── Résolution ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> resolveCommission(String banqueNom, String organismeNom, TransactionSource type) {
        if (banqueNom == null) {
            log.warn("resolveCommission appelé avec banqueNom null — type={}", type);
            return Optional.empty();
        }

        List<CommissionConfig> rules = repository.findByBanque_NomIgnoreCaseAndType(banqueNom, type);

        if (rules.isEmpty()) {
            log.debug("Aucune règle de commission pour banque='{}' type={}", banqueNom, type);
            return Optional.empty();
        }

        // 1. SI_MATCH : banque + organisme correspondent
        Optional<BigDecimal> siMatch = rules.stream()
                .filter(r -> r.getCondition() == CommissionCondition.SI_MATCH)
                .filter(r -> r.getOrganismeNom() != null
                        && r.getOrganismeNom().equalsIgnoreCase(organismeNom))
                .map(CommissionConfig::getMontant)
                .findFirst();

        if (siMatch.isPresent()) {
            log.debug("Règle SI_MATCH trouvée — banque='{}', organisme='{}', montant={}",
                    banqueNom, organismeNom, siMatch.get());
            return siMatch;
        }

        // 2. SI_NON_MATCH : banque a une règle conditionnelle mais organisme ne correspond pas
        boolean hasConditionalRule = rules.stream()
                .anyMatch(r -> r.getCondition() == CommissionCondition.SI_MATCH);

        if (hasConditionalRule) {
            Optional<BigDecimal> siNonMatch = rules.stream()
                    .filter(r -> r.getCondition() == CommissionCondition.SI_NON_MATCH)
                    .map(CommissionConfig::getMontant)
                    .findFirst();

            if (siNonMatch.isPresent()) {
                log.debug("Règle SI_NON_MATCH appliquée — banque='{}', organisme='{}', montant={}",
                        banqueNom, organismeNom, siNonMatch.get());
            } else {
                log.warn("Règle SI_MATCH existe pour banque='{}' mais aucune SI_NON_MATCH — pas de commission",
                        banqueNom);
            }
            return siNonMatch;
        }

        // 3. DEFAULT
        Optional<BigDecimal> defaultRule = rules.stream()
                .filter(r -> r.getCondition() == CommissionCondition.DEFAULT)
                .map(CommissionConfig::getMontant)
                .findFirst();

        log.debug("Règle DEFAULT — banque='{}', montant={}", banqueNom, defaultRule.orElse(null));
        return defaultRule;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCommission(String banqueNom, TransactionSource type) {
        return repository.existsByBanque_NomIgnoreCaseAndType(banqueNom, type);
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CommissionConfigResponseDTO> findAll() {
        return repository.findAll().stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public CommissionConfigResponseDTO create(CommissionConfigRequestDTO dto) {
        Banque banque = banqueService.findOrThrow(dto.banqueId());
        log.info("Création règle commission : banque='{}' (id={}), type={}, condition={}",
                banque.getNom(), dto.banqueId(), dto.type(), dto.condition());
        CommissionConfig entity = mapper.toEntity(dto);
        entity.setBanque(banque);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public CommissionConfigResponseDTO update(Long id, CommissionConfigRequestDTO dto) {
        CommissionConfig entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CommissionConfig introuvable — id=" + id));
        Banque banque = banqueService.findOrThrow(dto.banqueId());
        log.info("Mise à jour règle commission id={}, banque='{}' (id={})",
                id, banque.getNom(), dto.banqueId());
        mapper.updateEntity(dto, entity);
        entity.setBanque(banque);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CommissionConfig entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CommissionConfig introuvable — id=" + id));
        log.info("Suppression règle commission id={}", id);
        repository.delete(entity);
    }
}
