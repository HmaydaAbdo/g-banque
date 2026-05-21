package staport.finance.gBanque.moyensDePaiements.miseADisposition.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos.*;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.mapper.MiseADispositionMapper;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.specs.MiseADispositionSpec;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.NoSuchElementException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

@Service
public class MiseADispositionService {

    private static final Logger log = LoggerFactory.getLogger(MiseADispositionService.class);
    private final MiseADispositionRepository miseADispositionRepository;
    private final BanqueRepository banqueRepository;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());


    public MiseADispositionService(MiseADispositionRepository miseADispositionRepository, BanqueRepository banqueRepository) {
        this.miseADispositionRepository = miseADispositionRepository;
        this.banqueRepository = banqueRepository;
    }

    @Transactional()
    public MiseADispositionResponseDto createEncaissementMiseAdisposition(MiseAdispositionRequestDto dto) {
        log.info("Création d'une nouvelle mise à disposition d'encaissement: {}", dto);

        Banque banque = banqueRepository.findById(dto.getBanqueId()).orElseThrow(() -> {
            log.error("Banque avec l'ID {} introuvable", dto.getBanqueId());
            return new ResourceNotFoundException("Bank with id " + dto.getBanqueId() + " not found.");
        });

        MiseADisposition miseADisposition = MiseADispositionMapper.toEntity(dto, banque);

        log.debug("Enregistrement de l'entité mise à disposition: {}", miseADisposition);
        miseADisposition = miseADispositionRepository.save(miseADisposition);

        log.info("Mise à disposition créée avec succès avec l'ID: {}", miseADisposition.getId());
        return MiseADispositionMapper.toDto(miseADisposition);
    }

    @Transactional
    public MiseADispositionResponseDto editEncaissementMiseAdisposition(Long id, MiseAdispositionRequestDto dto) {
        log.info("Modification de la mise à disposition d'encaissement ID={} avec les données={}", id, dto);

        MiseADisposition existingMiseAdisposition = miseADispositionRepository.findById(id).orElseThrow(() -> {
            log.error("Mise à disposition avec l'ID {} introuvable", id);
            return new ResourceNotFoundException("MiseAdisposition with id " + id + " not found.");
        });

        if (existingMiseAdisposition.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'une mise à disposition qui n'est pas de type ENCAISSEMENT, ID={}", id);
            throw new IllegalArgumentException("Modification interdite: la MiseADisposition with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (existingMiseAdisposition.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'une mise à disposition déjà réglée, ID={}", id);
            throw new IllegalArgumentException("Modification interdite: la MiseADisposition with id " + id + " est déjà réglée.");
        }

        if (dto.getDateEmission() != null) {
            log.debug("Mise à jour de la date d'émission pour la mise à disposition ID={}", id);
            existingMiseAdisposition.setDateEmission(dto.getDateEmission());
        }

        if (dto.getBanqueId() != null) {
            log.debug("Mise à jour de la banque pour la mise à disposition ID={}", id);
            Banque banque = banqueRepository.findById(dto.getBanqueId())
                    .orElseThrow(() -> {
                        log.error("Banque avec l'ID {} introuvable", dto.getBanqueId());
                        return new ResourceNotFoundException("Bank with id " + dto.getBanqueId() + " not found.");
                    });
            existingMiseAdisposition.setBanque(banque);
        }

        if (dto.getMontant() != null) {
            log.debug("Mise à jour du montant pour la mise à disposition ID={}", id);
            existingMiseAdisposition.setMontant(dto.getMontant());
        }

        if (dto.getEmetteur() != null) {
            log.debug("Mise à jour de l'émetteur pour la mise à disposition ID={}", id);
            existingMiseAdisposition.setEmetteur(dto.getEmetteur());
        }

        existingMiseAdisposition = miseADispositionRepository.save(existingMiseAdisposition);
        log.info("Mise à disposition ID={} mise à jour avec succès", id);
        return MiseADispositionMapper.toDto(existingMiseAdisposition);
    }

    @Transactional(readOnly = true)
    public MiseADispositionResponseDto getMiseAdispositionById(Long id) {
        log.info("Récupération de la mise à disposition avec l'ID={}", id);
        MiseADisposition miseADisposition = miseADispositionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Mise à disposition avec l'ID {} introuvable", id);
                    return new ResourceNotFoundException("Mise à disposition with id " + id + " not found.");
                });
        log.debug("Mise à disposition trouvée: {}", miseADisposition);
        return MiseADispositionMapper.toDto(miseADisposition);
    }

    @Transactional
    public void deleteMiseAdispositionById(Long id) {
        log.info("Suppression de la mise à disposition avec l'ID={}", id);

        MiseADisposition miseADisposition = miseADispositionRepository.findById(id).orElseThrow(() -> {
            log.error("Mise à disposition avec l'ID {} introuvable", id);
            return new ResourceNotFoundException("Mise à disposition  with id " + id + " not found.");
        });



        if (miseADisposition.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'une mise à disposition déjà réglée, ID={}", id);
            throw new IllegalArgumentException("Suppression interdite: la mise à dispo with id " + id + " est déjà réglée.");
        }

        miseADispositionRepository.deleteById(id);
        log.info("Mise à disposition ID={} supprimée avec succès", id);
    }

    @Transactional(readOnly = true)
    public Page<MiseADispositionResponseDto> getDecaissementForMiseAdisposition(MiseADispositionSearchDto searchDto, Pageable pageable) {
        log.info("Recherche des mises à disposition de décaissement avec les filtres={} et pagination={}", searchDto, pageable);

        // Build the combined specification using the static methods from MiseADispositionSpec.
        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto)
                .and(MiseADispositionSpec.hasType(Type.DECAISSEMENT));

        // Execute the search with the combined specification and pagination.
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);

        log.info("{} mise(s) à disposition de décaissement trouvée(s)", miseADispositionPage.getTotalElements());

        // Map the resulting Page<MiseADisposition> to Page<MiseADispositionResponseDto>.
        return miseADispositionPage.map(MiseADispositionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<MiseADispositionResponseDto> getEncaissementForMiseAdisposition(MiseADispositionSearchDto searchDto, Pageable pageable) {
        log.info("Recherche des mises à disposition d'encaissement avec les filtres={} et pagination={}", searchDto, pageable);

        // Build the combined specification using the static methods from MiseADispositionSpec.
        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto)
                .and(MiseADispositionSpec.hasType(Type.ENCAISSEMENT));

        // Execute the search with the combined specification and pagination.
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);

        log.info("{} mise(s) à disposition d'encaissement trouvée(s)", miseADispositionPage.getTotalElements());

        // Map the resulting Page<MiseADisposition> to Page<MiseADispositionResponseDto>.
        return miseADispositionPage.map(MiseADispositionMapper::toDto);
    }

    @Transactional
    public MiseADispositionResponseDto toggleMiseADispositionStatus(Long id) {
        log.info("Basculement du statut pour la mise à disposition ID={}", id);

        MiseADisposition miseADisposition = miseADispositionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Mise à disposition avec l'ID {} introuvable", id);
                    return new NoSuchElementException("MiseADisposition with id " + id + " not found.");
                });

        if (miseADisposition.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'une mise à disposition annulée, ID={}", id);
            throw new IllegalStateException("Impossible de basculer le statut d'une mise à disposition annulée (ID=" + id + ").");
        }
        // Toggle the 'active' status.
        miseADisposition.setStatut(miseADisposition.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        log.debug("Nouveau statut pour la mise à disposition ID={}: actif={}", id, miseADisposition.getStatut() == StatutMoyenPaiement.ENCOURS);

        // Save the updated entity.
        miseADisposition = miseADispositionRepository.save(miseADisposition);

        log.info("Statut basculé avec succès pour la mise à disposition ID={}", id);

        // Map and return the updated DTO.
        return MiseADispositionMapper.toDto(miseADisposition);
    }

    @Transactional(readOnly = true)
    public EncoursMiseAdisposition getEncoursDecaissementsForMiseADispositionByBank(EncoursMiseAdispositionSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de décaissement pour les mises à disposition de la banque ID={} avec les filtres={}", banqueId, searchDto);

        // Build the specification for active mise à disposition for the specified bank in the current month
        Specification<MiseADisposition> spec = MiseADispositionSpec.encoursDecaissement(banqueId,endOfMonth)
                .and(MiseADispositionSpec.hasNumero(searchDto.getNumero()))
                .and(MiseADispositionSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(MiseADispositionSpec.hasMontant(searchDto.getMontant()))
                .and(MiseADispositionSpec.hasType(Type.DECAISSEMENT));

        // Execute the search with the specification and pagination
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = miseADispositionRepository.findAll(spec).stream().map(MiseADisposition::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<MiseADispositionResponseDto> result = miseADispositionPage.map(MiseADispositionMapper::toDto);

        log.info("Total des encours de décaissement pour la banque ID={}: {}", banqueId, totalDesEncours);

        // Map the result to DTOs and return
        return new EncoursMiseAdisposition(totalDesEncours, result);
    }

    @Transactional(readOnly = true)
    public EncoursMiseAdisposition getEncoursEncaissementsForMiseADispositionByBank(EncoursMiseAdispositionSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours d'encaissement pour les mises à disposition de la banque ID={} avec les filtres={}", banqueId, searchDto);

        // Build the specification for active mise à disposition for the specified bank in the current month
        Specification<MiseADisposition> spec = MiseADispositionSpec.encoursEncaissement(banqueId,endOfMonth)
                .and(MiseADispositionSpec.hasNumero(searchDto.getNumero()))
                .and(MiseADispositionSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(MiseADispositionSpec.hasMontant(searchDto.getMontant()))
                .and(MiseADispositionSpec.hasType(Type.ENCAISSEMENT))
                .and(MiseADispositionSpec.hasEmetteur(searchDto.getEmetteur()))
                .and(MiseADispositionSpec.hasDateEmission(searchDto.getDateEmission()));

        // Execute the search with the specification and pagination
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = miseADispositionRepository.findAll(spec).stream().map(MiseADisposition::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<MiseADispositionResponseDto> result = miseADispositionPage.map(MiseADispositionMapper::toDto);

        log.info("Total des encours d'encaissement pour la banque ID={}: {}", banqueId, totalDesEncours);

        // Map the result to DTOs and return
        return new EncoursMiseAdisposition(totalDesEncours, result);
    }
}