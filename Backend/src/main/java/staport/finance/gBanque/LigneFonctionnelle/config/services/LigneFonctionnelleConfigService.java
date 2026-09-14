package staport.finance.gBanque.LigneFonctionnelle.config.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.LigneFonctionnelle.config.dtos.LigneFonctionnelleConfigRequestDTO;
import staport.finance.gBanque.LigneFonctionnelle.config.dtos.LigneFonctionnelleConfigResponseDTO;
import staport.finance.gBanque.LigneFonctionnelle.config.entities.LigneFonctionnelleConfig;
import staport.finance.gBanque.LigneFonctionnelle.config.mappers.LigneFonctionnelleConfigMapper;
import staport.finance.gBanque.LigneFonctionnelle.config.repositories.LigneFonctionnelleConfigRepository;
import staport.finance.gBanque.LigneFonctionnelle.config.services.interfaces.ILigneFonctionnelleConfigService;

@Service
public class LigneFonctionnelleConfigService implements ILigneFonctionnelleConfigService {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleConfigService.class);

    /** Ligne unique de configuration. */
    private static final Long SINGLETON_ID = 1L;

    /**
     * Valeurs historiques appliquées avant la migration de ces paramètres depuis des
     * propriétés {@code @Value} (non définies dans application.yml — trou de configuration
     * comblé par cette entité) vers cette configuration administrable. Utilisées uniquement
     * pour amorcer la ligne de configuration au premier démarrage — modifiables ensuite
     * uniquement via l'interface d'administration.
     */
    private static final int ALERT_THRESHOLD_MONTHS_INITIAL = 1;
    private static final int WARNING_THRESHOLD_MONTHS_INITIAL = 3;

    private final LigneFonctionnelleConfigRepository repository;
    private final LigneFonctionnelleConfigMapper mapper;

    public LigneFonctionnelleConfigService(LigneFonctionnelleConfigRepository repository, LigneFonctionnelleConfigMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @PostConstruct
    public void ensureDefaultConfig() {
        if (!repository.existsById(SINGLETON_ID)) {
            LigneFonctionnelleConfig config = new LigneFonctionnelleConfig();
            config.setId(SINGLETON_ID);
            config.setAlertThresholdMonths(ALERT_THRESHOLD_MONTHS_INITIAL);
            config.setWarningThresholdMonths(WARNING_THRESHOLD_MONTHS_INITIAL);
            repository.save(config);
            log.info("Configuration LigneFonctionnelle initialisée avec les valeurs historiques");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LigneFonctionnelleConfigResponseDTO getConfig() {
        return mapper.toDto(findSingleton());
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = LigneFonctionnelleConfig.class)
    public LigneFonctionnelleConfigResponseDTO update(LigneFonctionnelleConfigRequestDTO dto) {
        LigneFonctionnelleConfig entity = findSingleton();
        mapper.updateEntity(dto, entity);
        LigneFonctionnelleConfig saved = repository.save(entity);
        log.info("Configuration LigneFonctionnelle mise à jour");
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public int getAlertThresholdMonths() {
        return findSingleton().getAlertThresholdMonths();
    }

    @Override
    @Transactional(readOnly = true)
    public int getWarningThresholdMonths() {
        return findSingleton().getWarningThresholdMonths();
    }

    private LigneFonctionnelleConfig findSingleton() {
        return repository.findById(SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Configuration LigneFonctionnelle non initialisée — vérifier LigneFonctionnelleConfigService.ensureDefaultConfig()"));
    }
}
