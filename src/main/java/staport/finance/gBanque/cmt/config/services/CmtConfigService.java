package staport.finance.gBanque.cmt.config.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.cmt.config.dtos.CmtConfigRequestDTO;
import staport.finance.gBanque.cmt.config.dtos.CmtConfigResponseDTO;
import staport.finance.gBanque.cmt.config.entities.CmtConfig;
import staport.finance.gBanque.cmt.config.mappers.CmtConfigMapper;
import staport.finance.gBanque.cmt.config.repositories.CmtConfigRepository;
import staport.finance.gBanque.cmt.config.services.interfaces.ICmtConfigService;

@Service
public class CmtConfigService implements ICmtConfigService {

    private static final Logger log = LoggerFactory.getLogger(CmtConfigService.class);

    /** Ligne unique de configuration. */
    private static final Long SINGLETON_ID = 1L;

    /**
     * Valeurs historiques appliquées avant la migration de ces paramètres depuis
     * des constantes codées en dur dans {@code CmtMetrics} vers cette configuration
     * administrable. Utilisées uniquement pour amorcer la ligne de configuration au
     * premier démarrage — modifiables ensuite uniquement via l'interface d'administration.
     */
    private static final int ALERTE_COURT_TERME_INITIALE = 7;
    private static final int ALERTE_LONG_TERME_INITIALE = 30;

    private final CmtConfigRepository repository;
    private final CmtConfigMapper mapper;

    public CmtConfigService(CmtConfigRepository repository, CmtConfigMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @PostConstruct
    public void ensureDefaultConfig() {
        if (!repository.existsById(SINGLETON_ID)) {
            CmtConfig config = new CmtConfig();
            config.setId(SINGLETON_ID);
            config.setAlerteEcheanceJoursCourtTerme(ALERTE_COURT_TERME_INITIALE);
            config.setAlerteEcheanceJoursLongTerme(ALERTE_LONG_TERME_INITIALE);
            repository.save(config);
            log.info("Configuration CMT initialisée avec les valeurs historiques");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CmtConfigResponseDTO getConfig() {
        return mapper.toDto(findSingleton());
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = CmtConfig.class)
    public CmtConfigResponseDTO update(CmtConfigRequestDTO dto) {
        CmtConfig entity = findSingleton();
        mapper.updateEntity(dto, entity);
        CmtConfig saved = repository.save(entity);
        log.info("Configuration CMT mise à jour");
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public int getAlerteEcheanceJoursCourtTerme() {
        return findSingleton().getAlerteEcheanceJoursCourtTerme();
    }

    @Override
    @Transactional(readOnly = true)
    public int getAlerteEcheanceJoursLongTerme() {
        return findSingleton().getAlerteEcheanceJoursLongTerme();
    }

    private CmtConfig findSingleton() {
        return repository.findById(SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Configuration CMT non initialisée — vérifier CmtConfigService.ensureDefaultConfig()"));
    }
}
