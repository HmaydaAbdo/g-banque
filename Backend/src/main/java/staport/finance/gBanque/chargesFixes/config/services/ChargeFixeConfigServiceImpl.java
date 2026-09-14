package staport.finance.gBanque.chargesFixes.config.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.chargesFixes.TypeChargeFixe;
import staport.finance.gBanque.chargesFixes.config.dtos.request.ChargeFixeConfigRequestDTO;
import staport.finance.gBanque.chargesFixes.config.dtos.request.ChargeFixeConfigSearchCriteria;
import staport.finance.gBanque.chargesFixes.config.dtos.response.ChargeFixeConfigResponseDTO;
import staport.finance.gBanque.chargesFixes.config.entities.ChargeFixeConfig;
import staport.finance.gBanque.chargesFixes.config.mappers.ChargeFixeConfigMapper;
import staport.finance.gBanque.chargesFixes.config.repositories.ChargeFixeConfigRepository;
import staport.finance.gBanque.chargesFixes.config.services.interfaces.IChargeFixeConfigService;
import staport.finance.gBanque.chargesFixes.config.specifications.ChargeFixeConfigSpecification;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import java.util.List;

@Service
public class ChargeFixeConfigServiceImpl implements IChargeFixeConfigService {

    private static final Logger log = LoggerFactory.getLogger(ChargeFixeConfigServiceImpl.class);

    private final ChargeFixeConfigRepository repository;
    private final ChargeFixeConfigMapper mapper;
    private final IBanqueService banqueService;

    public ChargeFixeConfigServiceImpl(ChargeFixeConfigRepository repository,
                                        ChargeFixeConfigMapper mapper,
                                        IBanqueService banqueService) {
        this.repository = repository;
        this.mapper = mapper;
        this.banqueService = banqueService;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ChargeFixeConfigResponseDTO> findAll(ChargeFixeConfigSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(ChargeFixeConfigSpecification.searchByCriteria(criteria), pageable)
                .map(mapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargeFixeConfig> findAllEntities() {
        return repository.findAll();
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = ChargeFixeConfig.class)
    public ChargeFixeConfigResponseDTO create(ChargeFixeConfigRequestDTO dto) {
        validate(dto);
        Banque banque = banqueService.findOrThrow(dto.banqueId());
        ChargeFixeConfig entity = mapper.toEntity(dto, banque);
        ChargeFixeConfig saved = repository.save(entity);
        log.info("Création charge fixe '{}' pour banque '{}'", saved.getLibelle(), banque.getNom());
        return mapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = ChargeFixeConfig.class)
    public ChargeFixeConfigResponseDTO update(Long id, ChargeFixeConfigRequestDTO dto) {
        validate(dto);
        ChargeFixeConfig entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charge fixe introuvable — id=" + id));
        Banque banque = banqueService.findOrThrow(dto.banqueId());
        mapper.updateEntity(dto, entity);
        entity.setBanque(banque);
        ChargeFixeConfig saved = repository.save(entity);
        log.info("Mise à jour charge fixe id={}", id);
        return mapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = ChargeFixeConfig.class)
    public void delete(Long id) {
        ChargeFixeConfig entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charge fixe introuvable — id=" + id));
        log.info("Suppression charge fixe id={}, libellé='{}'", id, entity.getLibelle());
        repository.delete(entity);
    }

    private void validate(ChargeFixeConfigRequestDTO dto) {
        if (dto.type() == TypeChargeFixe.IMPOT_TAXE && dto.motif() == null) {
            throw new ValidationException("Le motif est obligatoire pour une charge de type IMPOT_TAXE.");
        }
        if (dto.type() != TypeChargeFixe.IMPOT_TAXE && dto.motif() != null) {
            throw new ValidationException("Le motif ne s'applique qu'aux charges de type IMPOT_TAXE.");
        }
        if (dto.jourEmission() == 0 || dto.jourEmission() < -2 || dto.jourEmission() > 31) {
            throw new ValidationException(
                    "jourEmission invalide : doit être > 0 (jour du mois), -1 (dernier jour du mois) "
                            + "ou -2 (dernier jour du trimestre).");
        }
        if (dto.moisPrecedent() && !dto.appendPeriode()) {
            throw new ValidationException("moisPrecedent ne peut être vrai que si appendPeriode est également vrai.");
        }
    }


}
