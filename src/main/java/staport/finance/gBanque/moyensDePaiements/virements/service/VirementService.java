package staport.finance.gBanque.moyensDePaiements.virements.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.virements.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.virements.mapper.VirementMapper;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.moyensDePaiements.virements.specs.VirementSpec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.NoSuchElementException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

@Service
public class VirementService {

    private static final Logger log = LoggerFactory.getLogger(VirementService.class);
    private final VirementRepository virementRepository;
    private final BanqueRepository banqueRepository;
    private final VirementMapper virementMapper;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());


    public VirementService(VirementRepository virementRepository, BanqueRepository banqueRepository, VirementMapper virementMapper) {
        this.virementRepository = virementRepository;
        this.banqueRepository = banqueRepository;
        this.virementMapper = virementMapper;
    }

    @Transactional
    public VirementResponseDto createEncaissementVirement(VirementRequestDto dto) {
        log.info("Création d'un nouveau virement d'encaissement: {}", dto);

        Virement virement = this.virementMapper.toEntity(dto);
        virement.setStatut(StatutMoyenPaiement.ENCOURS);

        log.debug("Enregistrement de l'entité virement: {}", virement);
        virement = virementRepository.save(virement);

        log.info("Virement créé avec succès avec l'ID: {}", virement.getId());
        return this.virementMapper.toDto(virement);
    }

    @Transactional
    public VirementResponseDto editEncaissementVirement(Long id, VirementRequestDto dto) {
        log.info("Modification du virement d'encaissement ID={} avec les données={}", id, dto);

        Virement existingVirement = virementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement avec l'ID {} introuvable", id);
                    return new NoSuchElementException("Virement with id " + id + " not found.");
                });

        if (existingVirement.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'un virement qui n'est pas de type ENCAISSEMENT, ID={}", id);
            throw new IllegalArgumentException("Modification interdite: le virement with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (existingVirement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'un virement déjà réglé, ID={}", id);
            throw new IllegalArgumentException("Modification interdite: le virement with id " + id + " est déjà réglé.");
        }

        if (dto.getMontant() != null) {
            log.debug("Mise à jour du montant pour le virement ID={}", id);
            existingVirement.setMontant(dto.getMontant());
        }
        if (dto.getDateReception() != null) {
            log.debug("Mise à jour de la date de réception pour le virement ID={}", id);
            existingVirement.setDateEmission(dto.getDateReception());
        }
        if (dto.getEmetteur() != null) {
            log.debug("Mise à jour de l'émetteur pour le virement ID={}", id);
            existingVirement.setEmetteur(dto.getEmetteur());
        }
        if (dto.getBanqueBeneficiaireId() != null) {
            log.debug("Mise à jour de la banque bénéficiaire pour le virement ID={}", id);
            Banque banque = banqueRepository.findById(dto.getBanqueBeneficiaireId())
                    .orElseThrow(() -> {
                        log.error("Banque avec l'ID {} introuvable", dto.getBanqueBeneficiaireId());
                        return new ResourceNotFoundException("Banque with id: " + dto.getBanqueBeneficiaireId() + " not found");
                    });
            existingVirement.setBanqueBeneficiare(banque);
        }

        existingVirement = virementRepository.save(existingVirement);
        log.info("Virement ID={} mis à jour avec succès", id);
        return this.virementMapper.toDto(existingVirement);
    }

    @Transactional
    public VirementResponseDto getVirementById(Long id) {
        log.info("Récupération du virement avec l'ID={}", id);

        Virement virement = virementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement avec l'ID {} introuvable", id);
                    return new ResourceNotFoundException("Virement with id " + id + " not found.");
                });

        log.debug("Virement trouvé: {}", virement);
        return this.virementMapper.toDto(virement);
    }

    @Transactional(readOnly = true)
    public Page<VirementResponseDto> getEncaissementVirements(VirementSearchDto searchDto, Pageable pageable) {
        log.info("Recherche des virements d'encaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto)
                .and(VirementSpec.hasType(Type.ENCAISSEMENT));

        Page<Virement> virementPage = virementRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("{} virement(s) d'encaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(this.virementMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<VirementResponseDto> getDecaissementVirements(VirementSearchDto searchDto, Pageable pageable) {
        log.info("Recherche des virements de décaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto)
                .and(VirementSpec.hasType(Type.DECAISSEMENT));

        Page<Virement> virementPage = virementRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("{} virement(s) de décaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(this.virementMapper::toDto);
    }

    @Transactional
    public VirementResponseDto toggleVirementStatus(Long id) {
        log.info("Basculement du statut pour le virement ID={}", id);

        Virement virement = virementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement avec l'ID {} introuvable", id);
                    return new NoSuchElementException("Virement with id " + id + " not found.");
                });

        if (virement.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'un virement annulé, ID={}", id);
            throw new IllegalStateException("Impossible de basculer le statut d'un virement annulé (ID=" + id + ").");
        }
        virement.setStatut(virement.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        log.debug("Nouveau statut pour le virement ID={}: actif={}", id, virement.getStatut() == StatutMoyenPaiement.ENCOURS);

        virement = virementRepository.save(virement);

        log.info("Statut basculé avec succès pour le virement ID={}", id);
        return this.virementMapper.toDto(virement);
    }

    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursDecaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de décaissement pour la banque ID={} avec les filtres={}", banqueId, searchDto);

        Specification<Virement> spec = VirementSpec.encoursDecaissement(banqueId,endOfMonth)
                .and(VirementSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()));

        Page<Virement> virementPage = virementRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream()
                .map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours de décaissement pour la banque ID={}: {}", banqueId, totalDesEncours);

        return new EncoursVirementResponseDto(totalDesEncours, virementPage.map(this.virementMapper::toDto));
    }

    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursEncaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours d'encaissement pour la banque ID={} avec les filtres={}", banqueId, searchDto);

        Banque banqueBeneficiaire = banqueRepository.findById(banqueId)
                .orElseThrow(() -> {
                    log.error("Banque avec l'ID {} introuvable", banqueId);
                    return new NoSuchElementException("Bank with id " + banqueId + " not found.");
                });

        Specification<Virement> spec = VirementSpec.encoursEncaissement(banqueBeneficiaire.getNom(),endOfMonth)
                .and(VirementSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()))
                .and(VirementSpec.hasEmetteur(searchDto.getEmetteur()));

        Page<Virement> virementPage = virementRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream()
                .map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours d'encaissement pour la banque ID={}: {}", banqueId, totalDesEncours);

        return new EncoursVirementResponseDto(totalDesEncours, virementPage.map(this.virementMapper::toDto));
    }

    @Transactional
    public void deleteVirement(Long id) {
        log.info("Suppression du virement  ID={}", id);

        Virement virement = virementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement avec l'ID {} introuvable", id);
                    return new NoSuchElementException("Virement with id " + id + " not found.");
                });


        if (virement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'un virement déjà réglé, ID={}", id);
            throw new IllegalArgumentException("Suppression interdite: le virement with id " + id + " est déjà réglé.");
        }

        virementRepository.delete(virement);
        log.info("Virement ID={} supprimé avec succès", id);
    }

    /**
     * Nombre de virements échus à la date donnée, groupés par banque — façade publique sur
     * {@link VirementRepository#countEchusGroupedByBanque}. Permet à d'autres domaines (ex.
     * l'agrégateur accueil de Bank) de lire cette information sans jamais injecter
     * {@code VirementRepository} directement (Backend/CLAUDE.md, "Encapsulation des domaines").
     */
    public List<Object[]> countEchusGroupedByBanque(LocalDate today) {
        return virementRepository.countEchusGroupedByBanque(today);
    }
}