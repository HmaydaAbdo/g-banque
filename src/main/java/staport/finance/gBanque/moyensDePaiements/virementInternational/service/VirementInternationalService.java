package staport.finance.gBanque.moyensDePaiements.virementInternational.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.virementInternational.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virementInternational.mapper.VirementInternationalMapper;
import staport.finance.gBanque.moyensDePaiements.virementInternational.repositories.VirementInternationalRepository;
import staport.finance.gBanque.moyensDePaiements.virementInternational.specs.VirementInternationalSpec;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.NoSuchElementException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

@Service
public class VirementInternationalService {

    private static final Logger log = LoggerFactory.getLogger(VirementInternationalService.class);
    private final VirementInternationalRepository virementInternationalRepository;
    private final BanqueRepository banqueRepository;
    private final VirementInternationalMapper virementInternationalMapper;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public VirementInternationalService(
            VirementInternationalRepository virementInternationalRepository,
            BanqueRepository banqueRepository,
            VirementInternationalMapper virementInternationalMapper) {
        this.virementInternationalRepository = virementInternationalRepository;
        this.banqueRepository = banqueRepository;
        this.virementInternationalMapper = virementInternationalMapper;
    }

    /**
     * Crée un nouveau virement international (ENCAISSEMENT ou DECAISSEMENT).
     *
     * @param dto Les données du virement à créer.
     * @return Le virement créé sous forme de DTO.
     */
    @Transactional
    public VirementInternationalResponseDto createVirementInternational(VirementInternationalRequestDto dto) {
        log.info("Création d'un nouveau virement international de type {}: {}", dto.getType(), dto);

        VirementInternational virement = this.virementInternationalMapper.toEntity(dto);

        log.debug("Enregistrement de l'entité virement international: {}", virement);
        virement = virementInternationalRepository.save(virement);

        log.info("Virement international créé avec succès avec l'ID: {} (Type: {})", virement.getId(), virement.getType());
        return this.virementInternationalMapper.toDto(virement);
    }

    /**
     * Modifie un virement international existant.
     * La modification n'est autorisée que si le virement est actif.
     *
     * @param id L'ID du virement à modifier.
     * @param dto Les nouvelles données du virement.
     * @return Le virement modifié sous forme de DTO.
     */
    @Transactional
    public VirementInternationalResponseDto editVirementInternational(Long id, VirementInternationalRequestDto dto) {
        log.info("Modification du virement international ID={} avec les données={}", id, dto);

        VirementInternational existingVirement = virementInternationalRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement international avec l'ID {} introuvable", id);
                    return new NoSuchElementException("VirementInternational with id " + id + " not found.");
                });

        if (existingVirement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'un virement international inactif (déjà réglé), ID={}", id);
            throw new IllegalArgumentException("Modification interdite: le virement international with id " + id + " est déjà réglé.");
        }

        log.debug("Mise à jour du virement international ID={} via mapper", id);
        VirementInternational updatedVirement = this.virementInternationalMapper.updateEntity(existingVirement, dto);

        updatedVirement = virementInternationalRepository.save(updatedVirement);
        log.info("Virement international ID={} mis à jour avec succès", id);
        return this.virementInternationalMapper.toDto(updatedVirement);
    }

    /**
     * Récupère un virement international par son ID.
     *
     * @param id L'ID du virement à récupérer.
     * @return Le virement sous forme de DTO.
     */
    @Transactional(readOnly = true)
    public VirementInternationalResponseDto getVirementInternationalById(Long id) {
        log.info("Récupération du virement international avec l'ID={}", id);

        VirementInternational virement = virementInternationalRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement international avec l'ID {} introuvable", id);
                    return new ResourceNotFoundException("VirementInternational with id " + id + " not found.");
                });

        log.debug("Virement international trouvé: {}", virement);
        return this.virementInternationalMapper.toDto(virement);
    }

    /**
     * Récupère une page de virements internationaux selon les critères de recherche.
     *
     * @param searchDto Les critères de recherche.
     * @param pageable Les paramètres de pagination.
     * @return Une page de virements internationaux.
     */
    @Transactional(readOnly = true)
    public Page<VirementInternationalResponseDto> getVirementsInternationaux(
            VirementInternationalSearchDto searchDto, Pageable pageable) {
        log.info("Recherche des virements internationaux avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<VirementInternational> spec = VirementInternationalSpec.bySearchDto(searchDto);

        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, pageable);

        log.info("{} virement(s) international(aux) trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(this.virementInternationalMapper::toDto);
    }

    /**
     * Récupère les virements internationaux de type ENCAISSEMENT avec pagination.
     *
     * @param searchDto Les critères de recherche.
     * @param pageable Les paramètres de pagination.
     * @return Une page de virements internationaux d'encaissement.
     */
    @Transactional(readOnly = true)
    public Page<VirementInternationalResponseDto> getEncaissementVirementsInternationaux(
            VirementInternationalSearchDto searchDto, Pageable pageable) {
        log.info("Recherche des virements internationaux d'encaissement avec les filtres={} et pagination={}",
                searchDto, pageable);

        Specification<VirementInternational> spec = VirementInternationalSpec.bySearchDto(searchDto)
                .and(VirementInternationalSpec.hasType(Type.ENCAISSEMENT));

        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, pageable);

        log.info("{} virement(s) international(aux) d'encaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(this.virementInternationalMapper::toDto);
    }

    /**
     * Récupère les virements internationaux de type DECAISSEMENT avec pagination.
     *
     * @param searchDto Les critères de recherche.
     * @param pageable Les paramètres de pagination.
     * @return Une page de virements internationaux de décaissement.
     */
    @Transactional(readOnly = true)
    public Page<VirementInternationalResponseDto> getDecaissementVirementsInternationaux(
            VirementInternationalSearchDto searchDto, Pageable pageable) {
        log.info("Recherche des virements internationaux de décaissement avec les filtres={} et pagination={}",
                searchDto, pageable);

        Specification<VirementInternational> spec = VirementInternationalSpec.bySearchDto(searchDto)
                .and(VirementInternationalSpec.hasType(Type.DECAISSEMENT));

        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, pageable);

        log.info("{} virement(s) international(aux) de décaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(this.virementInternationalMapper::toDto);
    }

    /**
     * Bascule le statut actif/inactif d'un virement international.
     *
     * @param id L'ID du virement dont on veut basculer le statut.
     * @return Le virement avec son nouveau statut.
     */
    @Transactional
    public VirementInternationalResponseDto toggleVirementStatus(Long id) {
        log.info("Basculement du statut pour le virement international ID={}", id);

        VirementInternational virement = virementInternationalRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement international avec l'ID {} introuvable", id);
                    return new NoSuchElementException("VirementInternational with id " + id + " not found.");
                });

        virement.setStatut(virement.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        log.debug("Nouveau statut pour le virement international ID={}: actif={}", id, virement.getStatut() == StatutMoyenPaiement.ENCOURS);

        virement = virementInternationalRepository.save(virement);

        log.info("Statut basculé avec succès pour le virement international ID={}", id);
        return this.virementInternationalMapper.toDto(virement);
    }

    /**
     * Récupère les encours de décaissement pour une banque donnée.
     * Les encours sont les virements actifs de type DECAISSEMENT avec une date d'émission
     * égale ou antérieure à la fin du mois en cours.
     *
     * @param searchDto Les critères de recherche supplémentaires.
     * @param banqueId L'ID de la banque.
     * @param pageable Les paramètres de pagination.
     * @return Les encours de décaissement avec le total.
     */
    @Transactional(readOnly = true)
    public EncoursVirementInternationalResponseDto getEncoursDecaissement(
            EncoursVirementsInternationalSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de décaissement pour la banque ID={} avec les filtres={}",
                banqueId, searchDto);

        // Vérifier que la banque existe
        banqueRepository.findById(banqueId)
                .orElseThrow(() -> {
                    log.error("Banque avec l'ID {} introuvable", banqueId);
                    return new ResourceNotFoundException("Banque with id " + banqueId + " not found.");
                });

        Specification<VirementInternational> spec = VirementInternationalSpec.encoursDecaissement(banqueId,endOfMonth);

        // Ajout des filtres supplémentaires si présents
        if (searchDto.getDateEmission() != null) {
            spec = spec.and(VirementInternationalSpec.hasDateEmission(searchDto.getDateEmission()));
        }
        if (searchDto.getBeneficiaire() != null) {
            spec = spec.and(VirementInternationalSpec.hasBeneficiaire(searchDto.getBeneficiaire()));
        }
        if (searchDto.getMontant() != null) {
            spec = spec.and(VirementInternationalSpec.hasMontant(searchDto.getMontant()));
        }
        if (searchDto.getEmetteur() != null) {
            spec = spec.and(VirementInternationalSpec.hasEmetteur(searchDto.getEmetteur()));
        }

        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, pageable);

        // Calcul du total des encours
        BigDecimal totalDesEncours = virementInternationalRepository.findAll(spec).stream()
                .map(VirementInternational::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours de décaissement pour la banque ID={}: {}", banqueId, totalDesEncours);

        return new EncoursVirementInternationalResponseDto(
                totalDesEncours,
                virementPage.map(this.virementInternationalMapper::toDto)
        );
    }

    /**
     * Récupère les encours d'encaissement pour une banque donnée.
     * Les encours sont les virements actifs de type ENCAISSEMENT avec une date d'émission
     * égale ou antérieure à la fin du mois en cours.
     *
     * @param searchDto Les critères de recherche supplémentaires.
     * @param banqueId L'ID de la banque.
     * @param pageable Les paramètres de pagination.
     * @return Les encours d'encaissement avec le total.
     */
    @Transactional(readOnly = true)
    public EncoursVirementInternationalResponseDto getEncoursEncaissement(
            EncoursVirementsInternationalSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours d'encaissement pour la banque ID={} avec les filtres={}",
                banqueId, searchDto);

        // Vérifier que la banque existe
        banqueRepository.findById(banqueId)
                .orElseThrow(() -> {
                    log.error("Banque avec l'ID {} introuvable", banqueId);
                    return new ResourceNotFoundException("Banque with id " + banqueId + " not found.");
                });

        Specification<VirementInternational> spec = VirementInternationalSpec.encoursEncaissement(banqueId,endOfMonth);

        // Ajout des filtres supplémentaires si présents
        if (searchDto.getDateEmission() != null) {
            spec = spec.and(VirementInternationalSpec.hasDateEmission(searchDto.getDateEmission()));
        }
        if (searchDto.getBeneficiaire() != null) {
            spec = spec.and(VirementInternationalSpec.hasBeneficiaire(searchDto.getBeneficiaire()));
        }
        if (searchDto.getMontant() != null) {
            spec = spec.and(VirementInternationalSpec.hasMontant(searchDto.getMontant()));
        }
        if (searchDto.getEmetteur() != null) {
            spec = spec.and(VirementInternationalSpec.hasEmetteur(searchDto.getEmetteur()));
        }

        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, pageable);

        // Calcul du total des encours
        BigDecimal totalDesEncours = virementInternationalRepository.findAll(spec).stream()
                .map(VirementInternational::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours d'encaissement pour la banque ID={}: {}", banqueId, totalDesEncours);

        return new EncoursVirementInternationalResponseDto(
                totalDesEncours,
                virementPage.map(this.virementInternationalMapper::toDto)
        );
    }

    /**
     * Supprime un virement international.
     * La suppression n'est autorisée que si le virement est actif.
     *
     * @param id L'ID du virement à supprimer.
     */
    @Transactional
    public void deleteVirementInternational(Long id) {
        log.info("Suppression du virement international ID={}", id);

        VirementInternational virement = virementInternationalRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement international avec l'ID {} introuvable", id);
                    return new NoSuchElementException("VirementInternational with id " + id + " not found.");
                });

        if (virement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'un virement international inactif (déjà réglé), ID={}", id);
            throw new IllegalArgumentException("Suppression interdite: le virement international with id " + id + " est déjà réglé.");
        }

        virementInternationalRepository.delete(virement);
        log.info("Virement international ID={} supprimé avec succès", id);
    }
}