package staport.finance.gBanque.cautions.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.cautions.commissions.factory.CommissionCautionStrategieFactory;
import staport.finance.gBanque.cautions.commissions.strategy.CommissionCautionStrategie;
import staport.finance.gBanque.cautions.dtos.CautionCreateDto;
import staport.finance.gBanque.cautions.dtos.CautionResponseDto;
import staport.finance.gBanque.cautions.dtos.CautionSearchCriteria;
import staport.finance.gBanque.cautions.dtos.CautionUpdateDto;
import staport.finance.gBanque.cautions.dtos.CloturerCautionRequest;
import staport.finance.gBanque.cautions.dtos.CloturerCautionResponse;
import staport.finance.gBanque.cautions.entities.Caution;
import staport.finance.gBanque.cautions.exceptions.CautionBusinessException;
import staport.finance.gBanque.cautions.mappers.CautionMapper;
import staport.finance.gBanque.cautions.repository.CautionRepository;
import staport.finance.gBanque.cautions.specifications.CautionSpecification;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.mapper.CommissionMapper;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.enums.Status;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implémentation du service Caution.
 *
 * <p>Stratégie transactionnelle :
 * <ul>
 *   <li>Classe annotée {@code @Transactional(readOnly = true)} — toutes les lectures
 *       bénéficient du mode lecture seule (optimisation Hibernate + connexion pool).</li>
 *   <li>Méthodes d'écriture annotées {@code @Transactional(isolation = READ_COMMITTED)}
 *       — standard bancaire : évite les dirty reads, tolère les non-repeatable reads
 *       sur des données non critiques.</li>
 * </ul>
 *
 * <p>Anti-N+1 :
 * <ul>
 *   <li>{@code getById} → {@code findByIdAvecAssociations} avec EntityGraph (1 requête JOIN)</li>
 *   <li>{@code rechercher} → {@code findAll(spec, pageable)} avec EntityGraph sur la page courante</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CautionServiceImpl implements ICautionService {

    private static final Logger logger = LoggerFactory.getLogger(CautionServiceImpl.class);

    private final CautionRepository                  cautionRepository;
    private final CautionMapper                      cautionMapper;
    private final BanqueRepository                   banqueRepository;
    private final NatureLigneFonctionnelleRepository natureRepository;
    private final MarcheRepository                   marcheRepository;
    private final CommisionRepository                commissionRepository;
    private final CommissionMapper                   commissionMapper;
    private final ImpotEtTaxeRepository              impotEtTaxeRepository;
    private final CommissionCautionStrategieFactory  strategieFactory;

    private static final BigDecimal TAUX_TVA = new BigDecimal("0.10");

    public CautionServiceImpl(
            CautionRepository cautionRepository,
            CautionMapper cautionMapper,
            BanqueRepository banqueRepository,
            NatureLigneFonctionnelleRepository natureRepository,
            MarcheRepository marcheRepository,
            CommisionRepository commissionRepository,
            CommissionMapper commissionMapper,
            ImpotEtTaxeRepository impotEtTaxeRepository,
            CommissionCautionStrategieFactory strategieFactory) {
        this.cautionRepository    = cautionRepository;
        this.cautionMapper        = cautionMapper;
        this.banqueRepository     = banqueRepository;
        this.natureRepository     = natureRepository;
        this.marcheRepository     = marcheRepository;
        this.commissionRepository = commissionRepository;
        this.commissionMapper     = commissionMapper;
        this.impotEtTaxeRepository = impotEtTaxeRepository;
        this.strategieFactory     = strategieFactory;
    }

    // ── Création ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CautionResponseDto creer(CautionCreateDto dto) {
        logger.info("Création caution — numeroCaution: {}", dto.numeroCaution());

        // 1. Unicité du numéro de caution
        if (cautionRepository.existsByNumeroCaution(dto.numeroCaution())) {
            throw new DuplicateEntryException(
                "Une caution avec le numéro '" + dto.numeroCaution() + "' existe déjà."
            );
        }

        // 2. Résolution des associations
        Banque banque = trouverBanqueOuLeverException(dto.banqueId());
        NatureLigneFonctionnelle nature = trouverNatureOuLeverException(dto.natureId());
        Marche marche = dto.marcheId() != null ? trouverMarcheOuLeverException(dto.marcheId()) : null;

        // 3. Mapping + injection des associations
        Caution caution = cautionMapper.toEntity(dto);
        caution.setBanque(banque);
        caution.setNature(nature);
        caution.setMarche(marche);

        // 4. Persistance
        Caution sauvegardee = cautionRepository.save(caution);
        logger.info("Caution créée — id: {}, numeroCaution: {}", sauvegardee.getId(), sauvegardee.getNumeroCaution());

        // 5. Rechargement avec associations pour la réponse (garantit la cohérence du DTO)
        return toDto(sauvegardee.getId());
    }

    // ── Modification ──────────────────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CautionResponseDto modifier(Long id, CautionUpdateDto dto) {
        logger.info("Modification caution — id: {}", id);

        Caution caution = trouverOuLeverException(id);

        // Règle métier : une caution inactive ne peut pas être modifiée
        if (Boolean.FALSE.equals(caution.getActif())) {
            throw new CautionBusinessException(
                "La caution id: " + id + " est archivée et ne peut pas être modifiée."
            );
        }

        // Vérification unicité numeroCaution si modification de ce champ
        if (!caution.getNumeroCaution().equals(dto.numeroCaution()) &&
                cautionRepository.existsByNumeroCautionAndIdNot(dto.numeroCaution(), id)) {
            throw new DuplicateEntryException(
                "Une caution avec le numéro '" + dto.numeroCaution() + "' existe déjà."
            );
        }

        // Résolution de toutes les associations (banque, nature, marche sont tous modifiables)
        Banque banque = trouverBanqueOuLeverException(dto.banqueId());
        NatureLigneFonctionnelle nature = trouverNatureOuLeverException(dto.natureId());
        Marche marche = dto.marcheId() != null ? trouverMarcheOuLeverException(dto.marcheId()) : null;

        // Application de tous les champs modifiables
        cautionMapper.applyUpdate(dto, caution);
        caution.setBanque(banque);
        caution.setNature(nature);
        caution.setMarche(marche);

        Caution modifiee = cautionRepository.save(caution);
        logger.info("Caution modifiée — id: {}", modifiee.getId());

        return toDto(modifiee.getId());
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Override
    public CautionResponseDto getById(Long id) {
        logger.debug("Récupération caution — id: {}", id);
        return toDto(id);
    }

    @Override
    public Page<CautionResponseDto> rechercher(CautionSearchCriteria criteres, Pageable pageable) {
        logger.debug("Recherche cautions — critères: {}", criteres);

        Page<Caution> page = cautionRepository.findAll(
            CautionSpecification.parCriteres(criteres),
            pageable
        );

        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        // Mapping vers DTOs — les associations sont déjà chargées par l'EntityGraph
        return page.map(cautionMapper::toDto);
    }

    // ── Suppression (hard delete) ─────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void supprimer(Long id) {
        logger.info("Suppression caution — id: {}", id);

        Caution caution = trouverOuLeverException(id);

        // Règle métier : la suppression est interdite lorsque la caution est archivée
        if (Boolean.FALSE.equals(caution.getActif())) {
            throw new CautionBusinessException(
                "La suppression de la caution id: " + id + " est interdite — la caution est archivée."
            );
        }

        cautionRepository.delete(caution);
        logger.info("Caution id: {} supprimée définitivement", id);
    }

    // ── Clôture (main levée) ──────────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CloturerCautionResponse cloturer(Long id, CloturerCautionRequest request) {
        logger.info("Clôture caution — id: {}, dateDeMainLevee: {}", id, request.dateDeMainLevee());

        // 1. Chargement avec associations (banque + nature nécessaires pour la stratégie)
        Caution caution = cautionRepository.findByIdAvecAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caution introuvable — id: " + id));

        // 2. Validations métier
        if (Boolean.FALSE.equals(caution.getActif())) {
            throw new CautionBusinessException("La caution id: " + id + " est déjà clôturée.");
        }


        // 3. Résolution de la stratégie
        String nomBanque = caution.getBanque().getNom();
        CommissionCautionStrategie strategie = strategieFactory.trouver(nomBanque)
                .orElseThrow(() -> new CautionBusinessException(
                        "Aucune stratégie de commission enregistrée pour la banque '" + nomBanque + "'."));

        // 4. Calcul et persistance de la commission de clôture
        Commission commission = strategie.calculerMainLevee(caution, request.dateDeMainLevee());
        commission.setCaution(caution);
        commission = commissionRepository.save(commission);
        logger.info("Commission de clôture générée — id: {}, montant: {}", commission.getId(), commission.getMontant());

        // 5. TVA si la stratégie l'exige (ex. BMCI)
        if (strategie.genererTva()) {
            BigDecimal montantTva = commission.getMontant()
                    .multiply(TAUX_TVA)
                    .setScale(2, RoundingMode.HALF_UP);

            ImpotEtTaxe tva = new ImpotEtTaxe();
            tva.setBanque(caution.getBanque());
            tva.setMontant(montantTva);
            tva.setMotif(Motif.TVA_COMMISSION_CAUTION);
            tva.setDateEmission(commission.getDateEmission());
            tva.setLibelle("TVA " + commission.getLibelle());
            tva.setObservation("TVA/commission/caution/cloture");
            tva.setCommission(commission);
            tva.setCaution(caution);
            impotEtTaxeRepository.save(tva);
            logger.info("TVA de clôture générée — montant: {}", montantTva);
        }

        // 6. Clôture de la caution
        caution.setDateDeMainLevee(request.dateDeMainLevee());
        caution.setActif(false);
        cautionRepository.save(caution);
        logger.info("Caution id: {} clôturée avec succès", id);

        return new CloturerCautionResponse(
                cautionMapper.toDto(caution),
                commissionMapper.toResponseDto(commission)
        );
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    @Override
    public Caution trouverOuLeverException(Long id) {
        return cautionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Caution introuvable — id: " + id
            ));
    }

    /**
     * Rechargement avec EntityGraph pour construire le DTO de réponse complet.
     * Utilisé en fin de chaque opération d'écriture pour retourner un état cohérent.
     */
    private CautionResponseDto toDto(Long id) {
        Caution caution = cautionRepository.findByIdAvecAssociations(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Caution introuvable après persistance — id: " + id
            ));
        return cautionMapper.toDto(caution);
    }

    private Banque trouverBanqueOuLeverException(Long banqueId) {
        return banqueRepository.findById(banqueId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Banque introuvable — id: " + banqueId
            ));
    }

    private NatureLigneFonctionnelle trouverNatureOuLeverException(Long natureId) {
        return natureRepository.findById(natureId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Nature de ligne fonctionnelle introuvable — id: " + natureId
            ));
    }

    private Marche trouverMarcheOuLeverException(Long marcheId) {
        return marcheRepository.findById(marcheId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Marché introuvable — id: " + marcheId
            ));
    }
}
