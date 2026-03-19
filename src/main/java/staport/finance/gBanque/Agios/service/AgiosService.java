package staport.finance.gBanque.Agios.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosSearchDto;
import staport.finance.gBanque.Agios.dtos.EncoursAgiosDto;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Agios.mapper.AgiosMapper;
import staport.finance.gBanque.Agios.repository.AgiosRepository;
import staport.finance.gBanque.Agios.spec.AgiosSpec;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

@Service
public class AgiosService {

    private static final Logger log = LoggerFactory.getLogger(AgiosService.class);
    private final AgiosRepository agiosRepository;
    private final BanqueRepository banqueRepository;
    private final AgiosMapper agiosMapper;
    private final RapprochementRepository rapprochementRepository;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public AgiosService(AgiosRepository agiosRepository, BanqueRepository banqueRepository, AgiosMapper agiosMapper, RapprochementRepository rapprochementRepository) {
        this.agiosRepository = agiosRepository;
        this.banqueRepository = banqueRepository;
        this.agiosMapper = agiosMapper;
        this.rapprochementRepository = rapprochementRepository;
        log.info("Initialisation du service Agios");
    }

    /**
     * Crée un nouvel agios
     *
     * @param requestDto Données de l'agios à créer
     * @return L'agios créé
     */
    @Transactional
    public AgiosResponseDto createAgios(AgiosRequestDto requestDto) {
        log.info("Création d'un agios avec les données suivantes {}", requestDto);

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID: {}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque non trouvée avec l'ID: " + requestDto.getBanqueId());
                });

        log.debug("Banque trouvée: {} (ID: {})", banque.getNom(), banque.getId());

        Agios agios = agiosMapper.toEntity(requestDto, banque);
        Agios savedAgios = agiosRepository.save(agios);

        log.info("Agios créé avec succès - ID: {}, Statut: {}", savedAgios.getId(), savedAgios.getStatus());
        return agiosMapper.toResponseDto(savedAgios);
    }

    /**
     * Récupère un agios par son ID
     *
     * @param id Identifiant de l'agios
     * @return L'agios demandé
     */
    @Transactional(readOnly = true)
    public AgiosResponseDto getById(Long id) {
        log.info("Récupération de l'agios avec l'ID: {}", id);

        Agios agios = agiosRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Agios non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Agios non trouvé avec l'ID: " + id);
                });

        log.debug("Agios trouvé - Libellé: '{}', Montant: {}, Statut: {}",
                agios.getLibelle(), agios.getMontant(), agios.getStatus());
        return agiosMapper.toResponseDto(agios);
    }

    /**
     * Recherche des agios selon des critères avec pagination
     *
     * @param searchDto Critères de recherche
     * @param pageable Informations de pagination
     * @return Page d'agios correspondant aux critères
     */
    @Transactional(readOnly = true)
    public Page<AgiosResponseDto> searchAgios(AgiosSearchDto searchDto, Pageable pageable) {
        log.info("Recherche d'agios avec critères: {}, Page: {}, Taille: {}",
                searchDto, pageable.getPageNumber(), pageable.getPageSize());

        Page<AgiosResponseDto> result = agiosRepository.findAll(
                        AgiosSpec.buildSpecification(searchDto),
                        pageable
                )
                .map(agiosMapper::toResponseDto);

        log.info("Recherche terminée - {} agios trouvé(s) sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * Met à jour un agios existant
     *
     * @param id Identifiant de l'agios à modifier
     * @param requestDto Nouvelles données de l'agios
     * @return L'agios mis à jour
     */
    @Transactional
    public AgiosResponseDto updateAgios(Long id, AgiosRequestDto requestDto) {
        log.info("Mise à jour de l'agios ID: {} - Nouveau libellé: '{}', Nouveau montant: {}",
                id, requestDto.getLibelle(), requestDto.getMontant());

        Agios agios = agiosRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Agios non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Agios non trouvé avec l'ID: " + id);
                });

        log.debug("Agios actuel avant modification - Libellé: '{}', Montant: {}, Statut: {}",
                agios.getLibelle(), agios.getMontant(), agios.getStatus());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID: {}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque non trouvée avec l'ID: " + requestDto.getBanqueId());
                });

        agios.setLibelle(requestDto.getLibelle());
        agios.setMontant(requestDto.getMontant());
        agios.setBanque(banque);
        agios.setDateEmission(requestDto.getDateEmission());

        Agios updatedAgios = agiosRepository.save(agios);
        log.info("Agios ID: {} mis à jour avec succès", id);
        return agiosMapper.toResponseDto(updatedAgios);
    }

    /**
     * Supprime un agios
     *
     * @param id Identifiant de l'agios à supprimer
     */
    @Transactional
    public void deleteAgios(Long id) {
        log.info("Suppression de l'agios avec l'ID: {}", id);

        if (!agiosRepository.existsById(id)) {
            log.error("Agios non trouvé avec l'ID: {}", id);
            throw new IllegalArgumentException("Agios non trouvé avec l'ID: " + id);
        }

        agiosRepository.deleteById(id);
        log.info("Agios ID: {} supprimé avec succès", id);
    }

    /**
     * Récupère les agios en cours avec leur montant total
     * Les agios en cours sont ceux avec un statut 'PREVU' et une date d'émission dans le passé
     *
     * @param searchDto Critères de recherche optionnels
     * @param pageable Informations de pagination et de tri
     * @return Un EncoursAgiosDto contenant la page d'agios et le montant total en cours
     */
    @Transactional(readOnly = true)
    public EncoursAgiosDto getOutstandingAgios(AgiosSearchDto searchDto, Pageable pageable) {

        // Déterminer la période : rapprochement EN_COURS si disponible, sinon mois courant
        YearMonth periode = rapprochementRepository
                .findByBanqueIdAndStatut(searchDto.getBanqueId(), StatutRapprochement.EN_COURS)
                .map(r -> r.getDateRapprochement())
                .orElse(YearMonth.now()); // fallback si pas encore de rapprochement EN_COURS

        Specification<Agios> spec = Specification
                .where(AgiosSpec.outstandingUntil(endOfMonth))
                .and(AgiosSpec.hasBanque(searchDto.getBanqueId()))
                .and(AgiosSpec.hasLibelle(searchDto.getLibelle()))
                .and(AgiosSpec.hasMontant(searchDto.getMontant()));

        Page<Agios> agiosPage = agiosRepository.findAll(spec, pageable);

        BigDecimal totalDesEncours = agiosRepository.findAll(spec).stream()
                .map(Agios::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Page<AgiosResponseDto> pageDto = agiosPage.map(agiosMapper::toResponseDto);

        EncoursAgiosDto result = new EncoursAgiosDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }

    @Transactional
    public AgiosResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'agios ID: {}", id);

        Agios agios = agiosRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Agios non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Agios non trouvé avec l'ID: " + id);
                });

        Status oldStatus = agios.getStatus();
        Status newStatus=oldStatus==Status.PAYE ? Status.PREVU : Status.PAYE;

        agios.setStatus(newStatus);
        Agios updatedAgios = agiosRepository.save(agios);

        log.info("Statut de l'agios ID: {} basculé avec succès de {} vers {}",
                id, oldStatus, newStatus);

        return agiosMapper.toResponseDto(updatedAgios);
    }
}