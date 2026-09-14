package staport.finance.gBanque.leasing.config.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.leasing.config.dtos.LeasingConfigRequestDTO;
import staport.finance.gBanque.leasing.config.dtos.LeasingConfigResponseDTO;
import staport.finance.gBanque.leasing.config.entities.LeasingConfig;
import staport.finance.gBanque.leasing.config.mappers.LeasingConfigMapper;
import staport.finance.gBanque.leasing.config.repositories.LeasingConfigRepository;
import staport.finance.gBanque.leasing.config.services.interfaces.ILeasingConfigService;

import java.math.BigDecimal;

@Service
public class LeasingConfigService implements ILeasingConfigService {

    private static final Logger log = LoggerFactory.getLogger(LeasingConfigService.class);

    /** Ligne unique de configuration. */
    private static final Long SINGLETON_ID = 1L;

    /**
     * Valeurs historiques appliquées avant la migration de ces paramètres depuis
     * {@code application.yml} vers cette configuration administrable. Utilisées uniquement
     * pour amorcer la ligne de configuration au premier démarrage — modifiables ensuite
     * uniquement via l'interface d'administration.
     */
    private static final BigDecimal VALEUR_RESIDUELLE_INITIALE = new BigDecimal("0.0100");
    private static final BigDecimal TAUX_TVA_INITIAL = new BigDecimal("0.2000");
    private static final int ALERTE_COURT_TERME_INITIALE = 7;
    private static final int ALERTE_LONG_TERME_INITIALE = 30;

    private final LeasingConfigRepository repository;
    private final LeasingConfigMapper mapper;

    public LeasingConfigService(LeasingConfigRepository repository, LeasingConfigMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @PostConstruct
    public void ensureDefaultConfig() {
        if (!repository.existsById(SINGLETON_ID)) {
            LeasingConfig config = new LeasingConfig();
            config.setId(SINGLETON_ID);
            config.setValeurResiduelle(VALEUR_RESIDUELLE_INITIALE);
            config.setTauxTva(TAUX_TVA_INITIAL);
            config.setAlerteEcheanceJoursCourtTerme(ALERTE_COURT_TERME_INITIALE);
            config.setAlerteEcheanceJoursLongTerme(ALERTE_LONG_TERME_INITIALE);
            repository.save(config);
            log.info("Configuration leasing initialisée avec les valeurs historiques");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LeasingConfigResponseDTO getConfig() {
        return mapper.toDto(findSingleton());
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = LeasingConfig.class)
    public LeasingConfigResponseDTO update(LeasingConfigRequestDTO dto) {
        LeasingConfig entity = findSingleton();
        mapper.updateEntity(dto, entity);
        LeasingConfig saved = repository.save(entity);
        log.info("Configuration leasing mise à jour");
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getValeurResiduelle() {
        return findSingleton().getValeurResiduelle();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTauxTva() {
        return findSingleton().getTauxTva();
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

    private LeasingConfig findSingleton() {
        return repository.findById(SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Configuration leasing non initialisée — vérifier LeasingConfigService.ensureDefaultConfig()"));
    }
}
