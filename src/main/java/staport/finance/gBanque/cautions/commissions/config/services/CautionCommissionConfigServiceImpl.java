package staport.finance.gBanque.cautions.commissions.config.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.cautions.commissions.config.dtos.request.CautionCommissionConfigRequestDTO;
import staport.finance.gBanque.cautions.commissions.config.dtos.request.CautionCommissionConfigSearchCriteria;
import staport.finance.gBanque.cautions.commissions.config.dtos.response.CautionCommissionConfigResponseDTO;
import staport.finance.gBanque.cautions.commissions.config.entities.CautionCommissionConfig;
import staport.finance.gBanque.cautions.commissions.config.mappers.CautionCommissionConfigMapper;
import staport.finance.gBanque.cautions.commissions.config.repositories.CautionCommissionConfigRepository;
import staport.finance.gBanque.cautions.commissions.config.services.interfaces.ICautionCommissionConfigService;
import staport.finance.gBanque.cautions.commissions.config.specifications.CautionCommissionConfigSpecification;
import staport.finance.gBanque.cautions.exceptions.CautionBusinessException;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class CautionCommissionConfigServiceImpl implements ICautionCommissionConfigService {

    private static final Logger log = LoggerFactory.getLogger(CautionCommissionConfigServiceImpl.class);

    private final CautionCommissionConfigRepository repository;
    private final CautionCommissionConfigMapper mapper;
    private final IBanqueService banqueService;

    public CautionCommissionConfigServiceImpl(CautionCommissionConfigRepository repository,
                                               CautionCommissionConfigMapper mapper,
                                               IBanqueService banqueService) {
        this.repository = repository;
        this.mapper = mapper;
        this.banqueService = banqueService;
    }

    @Override
    public Page<CautionCommissionConfigResponseDTO> findAll(CautionCommissionConfigSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(CautionCommissionConfigSpecification.searchByCriteria(criteria), pageable)
                .map(mapper::toResponseDTO);
    }

    @Override
    @Transactional
    public CautionCommissionConfigResponseDTO create(CautionCommissionConfigRequestDTO dto) {
        if (repository.existsByBanque_Id(dto.banqueId())) {
            throw new DuplicateEntryException(
                    "Une configuration de commission caution existe déjà pour cette banque.");
        }

        Banque banque = banqueService.findOrThrow(dto.banqueId());
        log.info("Création config commission caution — banque='{}' (id={}), montantMinimum={}, diviseur={}",
                banque.getNom(), dto.banqueId(), dto.montantMinimum(), dto.diviseur());

        CautionCommissionConfig entity = mapper.toEntity(dto);
        entity.setBanque(banque);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public CautionCommissionConfigResponseDTO update(Long id, CautionCommissionConfigRequestDTO dto) {
        CautionCommissionConfig entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Configuration de commission caution introuvable — id=" + id));

        if (repository.existsByBanque_IdAndIdNot(dto.banqueId(), id)) {
            throw new DuplicateEntryException(
                    "Une configuration de commission caution existe déjà pour cette banque.");
        }

        Banque banque = banqueService.findOrThrow(dto.banqueId());
        log.info("Mise à jour config commission caution id={}, banque='{}' (id={})",
                id, banque.getNom(), dto.banqueId());

        mapper.updateEntity(dto, entity);
        entity.setBanque(banque);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CautionCommissionConfig entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Configuration de commission caution introuvable — id=" + id));
        log.info("Suppression config commission caution id={}, banque='{}'", id, entity.getBanque().getNom());
        repository.delete(entity);
    }

    @Override
    public CautionCommissionConfigResponseDTO resolveOuLeverException(String nomBanque) {
        return repository.findByBanque_NomIgnoreCase(nomBanque)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new CautionBusinessException(
                        "Aucune configuration de commission caution (seuil minimum, diviseur) "
                                + "n'est définie pour la banque '" + nomBanque + "'. "
                                + "Contactez un administrateur pour la créer avant de générer des commissions."));
    }
}
