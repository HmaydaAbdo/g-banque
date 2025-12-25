package staport.finance.gBanque.moyensDePaiements.lcn.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.*;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.mapper.LcnMapper;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.specs.LcnSpecification;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
public class LcnService {

    private static final Logger log = LoggerFactory.getLogger(LcnService.class);
    private final LcnRepository lcnRepository;
    private final BanqueRepository banqueRepository;
    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;

    public LcnService(LcnRepository lcnRepository, BanqueRepository banqueRepository, LigneFonctionnelleRepository ligneFonctionnelleRepository) {
        this.lcnRepository = lcnRepository;
        this.banqueRepository = banqueRepository;
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
    }

    @Transactional
    public LcnResponseDTO createLcn(LcnRequestDto requestDto) {
        log.info("Création d'un nouveau LCN d'encaissement: {}", requestDto);

        Banque banque = banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(() -> {
            log.error("Banque avec l'ID {} introuvable", requestDto.getBanqueId());
            return new ResourceNotFoundException("Bank with id " + requestDto.getBanqueId() + " not found.");
        });

        if (requestDto.getNatureLcn() == NatureLcn.ESCOMPTE) {
            log.debug("Vérification de la ligne fonctionnelle escompte commerciale pour la banque ID: {}", banque.getId());
            ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banque.getId(), 2L)
                    .orElseThrow(() -> {
                        log.error("Ligne fonctionnelle escompte commerciale inactive ou introuvable pour la banque ID: {}", banque.getId());
                        return new ResourceNotFoundException("ligne fonctionnelle escompte commerciale inactif ou introuvable  pour la banque " + banque.getId());
                    });
        }

        Lcn lcn = LcnMapper.toEntity(requestDto, banque);
        log.debug("Enregistrement de l'entité LCN: {}", lcn);
        lcn = lcnRepository.save(lcn);
        log.info("LCN créé avec succès avec l'ID: {}", lcn.getId());
        return LcnMapper.toDto(lcn);
    }

    @Transactional
    public LcnResponseDTO editEncaissementLcn(Long id, LcnRequestDto dto) {
        log.info("Modification du LCN d'encaissement ID={} avec les données={}", id, dto);

        Lcn existingLcn = lcnRepository.findById(id).orElseThrow(() -> {
            log.error("LCN avec l'ID {} introuvable", id);
            return new ResourceNotFoundException("Lcn with id " + id + " not found.");
        });

        if (existingLcn.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'un LCN qui n'est pas de type ENCAISSEMENT, ID={}", id);
            throw new IllegalArgumentException("Modification interdite: le lcn with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (!existingLcn.isActive()) {
            log.warn("Tentative de modification d'un LCN inactif (déjà réglé), ID={}", id);
            throw new IllegalArgumentException("Modification interdite: le lcn with id " + id + " est déja réglé.");
        }

        if (dto.getDateEmission() != null) {
            log.debug("Mise à jour de la date d'émission pour le LCN ID={}", id);
            existingLcn.setDateEmission(dto.getDateEmission());
        }

        if (dto.getNumero() != null) {
            log.debug("Mise à jour du numéro pour le LCN ID={}", id);
            existingLcn.setNumero(dto.getNumero());
        }

        if (dto.getNatureLcn() != null) {
            log.debug("Mise à jour de la nature du LCN pour l'ID={}", id);
            existingLcn.setNatureLcn(dto.getNatureLcn());
        }

        if (dto.getDateEcheance() != null) {
            log.debug("Mise à jour de la date d'échéance pour le LCN ID={}", id);
            existingLcn.setDateEcheance(dto.getDateEcheance());
        }

        if (dto.getBanqueId() != null) {
            log.debug("Mise à jour de la banque pour le LCN ID={}", id);
            Banque banque = banqueRepository.findById(dto.getBanqueId())
                    .orElseThrow(() -> {
                        log.error("Banque avec l'ID {} introuvable", dto.getBanqueId());
                        return new ResourceNotFoundException("Bank with id " + dto.getBanqueId() + " not found.");
                    });
            existingLcn.setBanque(banque);
        }

        if (dto.getMontant() != null) {
            log.debug("Mise à jour du montant pour le LCN ID={}", id);
            existingLcn.setMontant(dto.getMontant());
        }

        if (dto.getEmetteur() != null) {
            log.debug("Mise à jour de l'émetteur pour le LCN ID={}", id);
            existingLcn.setEmetteur(dto.getEmetteur());
        }

        existingLcn = lcnRepository.save(existingLcn);
        log.info("LCN ID={} mis à jour avec succès", id);
        return LcnMapper.toDto(existingLcn);
    }

    @Transactional(readOnly = true)
    public LcnResponseDTO getLcnById(Long id) {
        log.info("Récupération du LCN avec l'ID={}", id);
        Lcn lcn = lcnRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("LCN avec l'ID {} introuvable", id);
                    return new ResourceNotFoundException("lcn with id " + id + " not found.");
                });
        log.debug("LCN trouvé: {}", lcn);
        return LcnMapper.toDto(lcn);
    }

    @Transactional
    public void deleteLcn(Long id) {
        log.info("Suppression du LCN avec l'ID={}", id);

        Lcn lcn = lcnRepository.findById(id).orElseThrow(() -> {
            log.error("LCN avec l'ID {} introuvable", id);
            return new ResourceNotFoundException("lcn with id " + id + " not found.");
        });

        if (lcn.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de suppression d'un LCN qui n'est pas de type ENCAISSEMENT, ID={}", id);
            throw new IllegalArgumentException("Suppression interdite: le lcn with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (!lcn.isActive()) {
            log.warn("Tentative de suppression d'un LCN inactif (déjà réglé), ID={}", id);
            throw new IllegalArgumentException("Suppression interdite: le lcn with id " + id + " est déja réglé.");
        }

        lcnRepository.deleteById(id);
        log.info("LCN ID={} supprimé avec succès", id);
    }

    @Transactional(readOnly = true)
    public Page<LcnResponseDTO> getDecaissements(LcnSearchDTO searchDto, Pageable pageable) {
        log.info("Recherche des LCN de décaissement avec les filtres={} et pagination={}", searchDto, pageable);

        // Combine specifications based on non-null search criteria from the DTO
        Specification<Lcn> spec = Specification.where(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasBanqueNom(searchDto.getNomBanque()))
                .and(LcnSpecification.isActive(searchDto.getActive()))
                .and(LcnSpecification.hasType(Type.DECAISSEMENT));

        // Execute the search with the combined specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);

        log.info("{} LCN de décaissement trouvé(s)", lcnPage.getTotalElements());

        // Map the result Page<Lcn> to Page<LcnResponseDTO>
        return lcnPage.map(LcnMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<LcnResponseDTO> getEncaissements(LcnSearchDTO searchDto, Pageable pageable) {
        log.info("Recherche des LCN d'encaissement avec les filtres={} et pagination={}", searchDto, pageable);

        // Combine specifications based on non-null search criteria from the DTO
        Specification<Lcn> spec = Specification.where(LcnSpecification.hasType(Type.ENCAISSEMENT))
                .and(LcnSpecification.hasDateEmission(searchDto.getDateEmission()))
                .and(LcnSpecification.hasDateEcheance(searchDto.getDateEcheance()))
                .and(LcnSpecification.hasEmetteur(searchDto.getEmetteur()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasBanqueNom(searchDto.getNomBanque()))
                .and(LcnSpecification.isActive(searchDto.getActive()))
                .and(LcnSpecification.hasNumero(searchDto.getNumero()));

        // Execute the search with the combined specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);

        log.info("{} LCN d'encaissement trouvé(s)", lcnPage.getTotalElements());

        // Map the result Page<Lcn> to Page<LcnResponseDTO>
        return lcnPage.map(LcnMapper::toDto);
    }

    @Transactional
    public LcnResponseDTO toggleLcnStatus(Long id) {
        log.info("Basculement du statut pour le LCN ID={}", id);

        Lcn lcn = lcnRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("LCN avec l'ID {} introuvable", id);
                    return new NoSuchElementException("Lcn with id " + id + " not found.");
                });

        // Toggle the 'active' status
        lcn.setActive(!lcn.isActive());
        log.debug("Nouveau statut pour le LCN ID={}: actif={}", id, lcn.isActive());

        // Save the updated lcn
        lcn = lcnRepository.save(lcn);

        log.info("Statut basculé avec succès pour le LCN ID={}", id);

        // Map and return the updated DTO
        return LcnMapper.toDto(lcn);
    }

    @Transactional(readOnly = true)
    public EncoursLcnResponse getEncoursDecaissementsForLcn(EncoursLcnSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de décaissement pour les LCN de la banque ID={} avec les filtres={}", banqueId, searchDto);

        // Build the specification for active LCN for the specified bank in the current month
        Specification<Lcn> spec = LcnSpecification.encoursDecaissement(banqueId)
                .and(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()));

        // Execute the search with the specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = lcnRepository.findAll(spec).stream().map(Lcn::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<LcnResponseDTO> result = lcnPage.map(LcnMapper::toDto);

        log.info("Total des encours de décaissement pour la banque ID={}: {}", banqueId, totalDesEncours);

        // Map the result to DTOs and return
        return new EncoursLcnResponse(totalDesEncours, result);
    }

    @Transactional(readOnly = true)
    public EncoursLcnResponse getEncoursEncaissementsForLcn(EncoursLcnSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours d'encaissement pour les LCN de la banque ID={} avec les filtres={}", banqueId, searchDto);

        // Build the specification for active LCN for the specified bank in the current month
        Specification<Lcn> spec = LcnSpecification.encoursEncaissement(banqueId)
                .and(LcnSpecification.hasEmetteur(searchDto.getEmetteur()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()))
                .and(LcnSpecification.hasDateEcheance(searchDto.getDateEcheance()))
                .and(LcnSpecification.hasDateEmission(searchDto.getDateEmission()))
                .and(LcnSpecification.hasNumero(searchDto.getNumero()));

        // Execute the search with the specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = lcnRepository.findAll(spec).stream().map(Lcn::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<LcnResponseDTO> result = lcnPage.map(LcnMapper::toDto);

        log.info("Total des encours d'encaissement pour la banque ID={}: {}", banqueId, totalDesEncours);

        // Map the result to DTOs and return
        return new EncoursLcnResponse(totalDesEncours, result);
    }
}