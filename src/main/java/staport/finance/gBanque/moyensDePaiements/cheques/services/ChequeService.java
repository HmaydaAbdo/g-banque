package staport.finance.gBanque.moyensDePaiements.cheques.services;

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
import staport.finance.gBanque.moyensDePaiements.cheques.dtos.*;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.mappers.ChequeMapper;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.cheques.specs.ChequeSpec;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.NoSuchElementException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

@Service
public class ChequeService {

    private static final Logger log = LoggerFactory.getLogger(ChequeService.class);
    private final ChequeRepository chequeRepository;
    private final BanqueRepository banqueRepository;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public ChequeService(ChequeRepository chequeRepository, BanqueRepository banqueRepository) {
        this.chequeRepository = chequeRepository;
        this.banqueRepository = banqueRepository;
    }

    @Transactional
    public ChequeResponseDto createEncaissementCheque(ChequeRequestDto dto) {
        log.info("Création d'un nouveau chèque d'encaissement: {}", dto);

        Banque banque = banqueRepository.findById(dto.getBanqueId()).orElseThrow(() -> {
            log.error("Banque avec l'ID {} introuvable", dto.getBanqueId());
            return new ResourceNotFoundException("Bank with id " + dto.getBanqueId() + " not found.");
        });

        Cheque cheque = ChequeMapper.toEntity(dto, banque);

        log.debug("Enregistrement de l'entité chèque: {}", cheque);
        cheque = chequeRepository.save(cheque);

        log.info("Chèque créé avec succès avec l'ID: {}", cheque.getId());
        return ChequeMapper.toDto(cheque);
    }

    @Transactional
    public ChequeResponseDto editEncaissementCheque(Long id, ChequeRequestDto dto) {
        log.info("Modification du chèque d'encaissement ID={} avec les données={}", id, dto);

        Cheque existingCheque = chequeRepository.findById(id).orElseThrow(() -> {
            log.error("Chèque avec l'ID {} introuvable", id);
            return new ResourceNotFoundException("Cheque with id " + id + " not found.");
        });

        if (existingCheque.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'un chèque qui n'est pas de type ENCAISSEMENT, ID={}", id);
            throw new IllegalArgumentException("Modification interdite: le cheque with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (existingCheque.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'un chèque déjà réglé, ID={}", id);
            throw new IllegalArgumentException("Modification interdite: le cheque with id " + id + " est déjà réglé.");
        }

        if (dto.getNumero() != null) {
            log.debug("Mise à jour du numéro pour le chèque ID={}", id);
            existingCheque.setNumero(dto.getNumero());
        }

        if (dto.getDateEmission() != null) {
            log.debug("Mise à jour de la date d'émission pour le chèque ID={}", id);
            existingCheque.setDateEmission(dto.getDateEmission());
        }

        if (dto.getDateEcheance() != null) {
            log.debug("Mise à jour de la date d'échéance pour le chèque ID={}", id);
            existingCheque.setDateEcheance(dto.getDateEcheance());
        }

        if (dto.getBanqueId() != null) {
            log.debug("Mise à jour de la banque pour le chèque ID={}", id);
            Banque banque = banqueRepository.findById(dto.getBanqueId())
                    .orElseThrow(() -> {
                        log.error("Banque avec l'ID {} introuvable", dto.getBanqueId());
                        return new ResourceNotFoundException("Bank with id " + dto.getBanqueId() + " not found.");
                    });
            existingCheque.setBanque(banque);
        }

        if (dto.getMontant() != null) {
            log.debug("Mise à jour du montant pour le chèque ID={}", id);
            existingCheque.setMontant(dto.getMontant());
        }

        if (dto.getEmetteur() != null) {
            log.debug("Mise à jour de l'émetteur pour le chèque ID={}", id);
            existingCheque.setEmetteur(dto.getEmetteur());
        }

        existingCheque = chequeRepository.save(existingCheque);
        log.info("Chèque ID={} mis à jour avec succès", id);
        return ChequeMapper.toDto(existingCheque);
    }

    @Transactional(readOnly = true)
    public ChequeResponseDto getChequeById(Long id) {
        log.info("Récupération du chèque avec l'ID={}", id);
        Cheque cheque = chequeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Chèque avec l'ID {} introuvable", id);
                    return new ResourceNotFoundException("Cheque with id " + id + " not found.");
                });
        log.debug("Chèque trouvé: {}", cheque);
        return ChequeMapper.toDto(cheque);
    }

    @Transactional
    public void deleteCheque(Long id) {
        log.info("Suppression du chèque avec l'ID={}", id);

        Cheque cheque = chequeRepository.findById(id).orElseThrow(() -> {
            log.error("Chèque avec l'ID {} introuvable", id);
            return new ResourceNotFoundException("Cheque with id " + id + " not found.");
        });


        if (cheque.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'un chèque déjà réglé, ID={}", id);
            throw new IllegalArgumentException("Suppression interdite: le cheque with id " + id + " est déjà réglé.");
        }

        chequeRepository.deleteById(id);
        log.info("Chèque ID={} supprimé avec succès", id);
    }

    @Transactional(readOnly = true)
    public Page<ChequeResponseDto> getAllDecaissement(ChequeSearchDto searchDto, Pageable pageable) {
        log.info("Recherche des chèques de décaissement avec les filtres={} et pagination={}", searchDto, pageable);

        // Combine specifications based on non-null search criteria from the DTO
        Specification<Cheque> spec = Specification.where(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasNomBanque(searchDto.getNomBanque()))
                .and(ChequeSpec.hasStatut(searchDto.getStatut()))
                .and(ChequeSpec.hasType(Type.DECAISSEMENT));

        // Execute the search with the combined specification and pagination
        Page<Cheque> chequePage = chequeRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("{} chèque(s) de décaissement trouvé(s)", chequePage.getTotalElements());

        // Map the result Page<Cheque> to Page<ChequeResponseDto>
        return chequePage.map(ChequeMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ChequeResponseDto> getAllEncaissement(ChequeSearchDto searchDto, Pageable pageable) {
        log.info("Recherche des chèques d'encaissement avec les filtres={} et pagination={}", searchDto, pageable);

        // Combine specifications based on non-null search criteria from the DTO
        Specification<Cheque> spec = Specification.where(ChequeSpec.hasType(Type.ENCAISSEMENT))
                .and(ChequeSpec.hasNomBanque(searchDto.getNomBanque()))
                .and(ChequeSpec.hasStatut(searchDto.getStatut()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()))
                .and(ChequeSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(ChequeSpec.hasEmetteur(searchDto.getEmetteur()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()));

        // Execute the search with the combined specification and pagination
        Page<Cheque> chequePage = chequeRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("{} chèque(s) d'encaissement trouvé(s)", chequePage.getTotalElements());

        // Map the result Page<Cheque> to Page<ChequeResponseDto>
        return chequePage.map(ChequeMapper::toDto);
    }

    @Transactional
    public ChequeResponseDto toggleChequeStatus(Long id) {
        log.info("Basculement du statut pour le chèque ID={}", id);

        Cheque cheque = chequeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Chèque avec l'ID {} introuvable", id);
                    return new NoSuchElementException("Cheque with id " + id + " not found.");
                });

        if (cheque.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'un chèque annulé, ID={}", id);
            throw new IllegalStateException("Impossible de basculer le statut d'un chèque annulé (ID=" + id + ").");
        }
        // Toggle the 'active' status
        cheque.setStatut(cheque.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        log.debug("Nouveau statut pour le chèque ID={}: actif={}", id, cheque.getStatut() == StatutMoyenPaiement.ENCOURS);

        // Save the updated cheque
        cheque = chequeRepository.save(cheque);

        log.info("Statut basculé avec succès pour le chèque ID={}", id);

        // Map and return the updated DTO
        return ChequeMapper.toDto(cheque);
    }

    @Transactional(readOnly = true)
    public EncoursChequeResponseDto getEncoursDecaissementForCheques(EncoursChequeSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de décaissement pour les chèques de la banque ID={} avec les filtres={}", banqueId, searchDto);

        // Build the specification for active cheques for the specified bank in the current month
        Specification<Cheque> spec = ChequeSpec.encoursDecaissements(banqueId,endOfMonth)
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()));

        // Execute the search with the specification and pagination
        Page<Cheque> page = chequeRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        Page<ChequeResponseDto> chequePage = page.map(ChequeMapper::toDto);
        BigDecimal total = chequeRepository.findAll(spec).stream().map(Cheque::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours de décaissement pour la banque ID={}: {}", banqueId, total);

        // Map the result to DTOs and return
        return new EncoursChequeResponseDto(total, chequePage);
    }

    @Transactional(readOnly = true)
    public EncoursChequeResponseDto getEncoursEncaissementForCheques(EncoursChequeSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours d'encaissement pour les chèques de la banque ID={} avec les filtres={}", banqueId, searchDto);

        // Build the specification for active cheques for the specified bank in the current month
        Specification<Cheque> spec = ChequeSpec.encoursEncaissements(banqueId,endOfMonth)
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasEmetteur(searchDto.getEmetteur()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasDateEmission(searchDto.getDateEmission()));

        // Execute the search with the specification and pagination
        Page<Cheque> page = chequeRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        Page<ChequeResponseDto> chequePage = page.map(ChequeMapper::toDto);
        BigDecimal total = chequeRepository.findAll(spec).stream().map(Cheque::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours d'encaissement pour la banque ID={}: {}", banqueId, total);

        // Map the result to DTOs and return
        return new EncoursChequeResponseDto(total, chequePage);
    }
}