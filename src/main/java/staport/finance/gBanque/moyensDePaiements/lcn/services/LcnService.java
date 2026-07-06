package staport.finance.gBanque.moyensDePaiements.lcn.services;

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
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.*;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.mapper.LcnMapper;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.specs.LcnSpecification;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.NoSuchElementException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

import static staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService.ESC_COM_CODE;

@Service
public class LcnService {

    private static final Logger log = LoggerFactory.getLogger(LcnService.class);
    private final LcnRepository lcnRepository;
    private final BanqueRepository banqueRepository;
    private final LigneFonctionnelleReader ligneFonctionnelleReader;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public LcnService(LcnRepository lcnRepository, BanqueRepository banqueRepository, LigneFonctionnelleReader ligneFonctionnelleReader) {
        this.lcnRepository = lcnRepository;
        this.banqueRepository = banqueRepository;
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
    }


    @Transactional
    public LcnResponseDTO createLcn(LcnRequestDto requestDto) {
        log.info("Création d'un nouveau LCN d'encaissement: {}", requestDto);

        validateEscompteBusinessRule(requestDto.getType(), requestDto.getNatureLcn());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque avec l'ID {} introuvable", requestDto.getBanqueId());
                    return new ResourceNotFoundException(
                            "Bank with id " + requestDto.getBanqueId() + " not found.");
                });

        LigneFonctionnelle ligneFonctionnelle = null;

        //  Association automatique pour les LCN de type ESCOMPTE
        if (requestDto.getNatureLcn() == NatureLcn.ESCOMPTE) {
            log.debug("LCN de type ESCOMPTE détecté - recherche de la ligne fonctionnelle escompte commerciale pour la banque ID: {}",
                    banque.getId());

            ligneFonctionnelle = ligneFonctionnelleReader
                    .findByBanqueIdAndNatureLigneFonctionnelleCodeAndActifTrue(banque.getId(), ESC_COM_CODE)
                    .orElseThrow(() -> {
                        log.error("Ligne fonctionnelle escompte commerciale active introuvable pour la banque ID: {}",
                                banque.getId());
                        return new ResourceNotFoundException(
                                "Aucune ligne fonctionnelle escompte commerciale active n'a été trouvée pour la banque '" +
                                        banque.getNom() + "' (ID: " + banque.getId() + "). " +
                                        "Veuillez activer une ligne fonctionnelle escompte commerciale avant de créer un LCN de type ESCOMPTE.");
                    });

            log.info("Ligne fonctionnelle escompte commerciale trouvée: ID={}, Plafond={}",
                    ligneFonctionnelle.getId(),
                    ligneFonctionnelle.getPlafond());


        }

        // Création de l'entité LCN
        Lcn lcn = LcnMapper.toEntity(requestDto, banque);

        //  Association de la ligne fonctionnelle si type ESCOMPTE
        if (ligneFonctionnelle != null) {
            lcn.setLigneFonctionnelle(ligneFonctionnelle);
            log.debug("LCN associé à la ligne fonctionnelle ID={}", ligneFonctionnelle.getId());
        }

        log.debug("Enregistrement de l'entité LCN: {}", lcn);
        lcn = lcnRepository.save(lcn);

        log.info("LCN créé avec succès - ID: {}, Type: {}, Nature: {}, Ligne fonctionnelle: {}",
                lcn.getId(),
                lcn.getType(),
                lcn.getNatureLcn(),
                ligneFonctionnelle != null ? ligneFonctionnelle.getId() : "N/A");

        return LcnMapper.toDto(lcn);
    }

    @Transactional
    public LcnResponseDTO editEncaissementLcn(Long id, LcnRequestDto dto) {
        log.info("Modification du LCN d'encaissement ID={} avec les données={}", id, dto);

        Lcn existingLcn = lcnRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("LCN avec l'ID {} introuvable", id);
                    return new ResourceNotFoundException("Lcn with id " + id + " not found.");
                });

        if (existingLcn.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'un LCN qui n'est pas de type ENCAISSEMENT, ID={}", id);
            throw new IllegalArgumentException(
                    "Modification interdite: le lcn with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (existingLcn.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'un LCN déjà réglé, ID={}", id);
            throw new IllegalArgumentException(
                    "Modification interdite: le lcn with id " + id + " est déjà réglé.");
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

            //  Si on change la nature vers ESCOMPTE, il faut associer la ligne fonctionnelle
            if (dto.getNatureLcn() == NatureLcn.ESCOMPTE &&
                    existingLcn.getNatureLcn() != NatureLcn.ESCOMPTE) {

                log.debug("Changement de nature vers ESCOMPTE - recherche de la ligne fonctionnelle");
                LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleReader
                        .findByBanqueIdAndNatureLigneFonctionnelleCodeAndActifTrue(
                                existingLcn.getBanque().getId(), ESC_COM_CODE)
                        .orElseThrow(() -> {
                            log.error("Ligne fonctionnelle escompte commerciale active introuvable pour la banque ID: {}",
                                    dto.getBanqueId());
                            return new ResourceNotFoundException(
                                    "Aucune ligne fonctionnelle escompte commerciale active n'a été trouvée pour cette banque.");
                        });

                existingLcn.setLigneFonctionnelle(ligneFonctionnelle);
                log.debug("Ligne fonctionnelle ID={} associée au LCN", ligneFonctionnelle.getId());
            }

            // Si on change la nature depuis ESCOMPTE vers autre chose, on dissocie la ligne
            if (dto.getNatureLcn() != NatureLcn.ESCOMPTE &&
                    existingLcn.getNatureLcn() == NatureLcn.ESCOMPTE) {

                log.debug("Changement de nature depuis ESCOMPTE - dissociation de la ligne fonctionnelle");
                existingLcn.setLigneFonctionnelle(null);
            }

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
                        return new ResourceNotFoundException(
                                "Bank with id " + dto.getBanqueId() + " not found.");
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

        if (lcn.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'un LCN déjà réglé, ID={}", id);
            throw new IllegalArgumentException("Suppression interdite: le lcn with id " + id + " est déjà réglé.");
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
                .and(LcnSpecification.hasStatut(searchDto.getStatut()))
                .and(LcnSpecification.hasType(Type.DECAISSEMENT));

        // Execute the search with the combined specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

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
                .and(LcnSpecification.hasStatut(searchDto.getStatut()))
                .and(LcnSpecification.hasNumero(searchDto.getNumero()));

        // Execute the search with the combined specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

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

        if (lcn.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'un LCN annulé, ID={}", id);
            throw new IllegalStateException("Impossible de basculer le statut d'un LCN annulé (ID=" + id + ").");
        }
        // Toggle the 'active' status
        lcn.setStatut(lcn.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        log.debug("Nouveau statut pour le LCN ID={}: actif={}", id, lcn.getStatut() == StatutMoyenPaiement.ENCOURS);

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
        Specification<Lcn> spec = LcnSpecification.encoursDecaissement(banqueId,endOfMonth)
                .and(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()));

        // Execute the search with the specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
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
        Specification<Lcn> spec = LcnSpecification.encoursEncaissement(banqueId,endOfMonth)
                .and(LcnSpecification.hasEmetteur(searchDto.getEmetteur()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()))
                .and(LcnSpecification.hasDateEcheance(searchDto.getDateEcheance()))
                .and(LcnSpecification.hasDateEmission(searchDto.getDateEmission()))
                .and(LcnSpecification.hasNumero(searchDto.getNumero()));

        // Execute the search with the specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        BigDecimal totalDesEncours = lcnRepository.findAll(spec).stream().map(Lcn::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<LcnResponseDTO> result = lcnPage.map(LcnMapper::toDto);

        log.info("Total des encours d'encaissement pour la banque ID={}: {}", banqueId, totalDesEncours);

        // Map the result to DTOs and return
        return new EncoursLcnResponse(totalDesEncours, result);
    }

    /**
     * Nombre de LCN échus à la date donnée, groupés par banque — façade publique sur
     * {@link LcnRepository#countEchusGroupedByBanque}. Permet à d'autres domaines (ex.
     * l'agrégateur accueil de Bank) de lire cette information sans jamais injecter
     * {@code LcnRepository} directement (Backend/CLAUDE.md, "Encapsulation des domaines").
     */
    public List<Object[]> countEchusGroupedByBanque(LocalDate today) {
        return lcnRepository.countEchusGroupedByBanque(today);
    }

    /**
     * Nombre de LCN échus pour une banque donnée — façade publique sur
     * {@link LcnRepository#countEchusByBanqueId}. Utilisé par l'agrégateur accueil (requête
     * ciblée sur une seule banque, plus efficace que {@link #countEchusGroupedByBanque}).
     */
    public long countEchusByBanqueId(Long banqueId, LocalDate today) {
        return lcnRepository.countEchusByBanqueId(banqueId, today);
    }

    private void validateEscompteBusinessRule(Type type, NatureLcn nature) {
        if (nature == NatureLcn.ESCOMPTE && type != Type.ENCAISSEMENT) {
            log.error("Règle métier violée: tentative de création d'un LCN ESCOMPTE avec le type {}", type);
            throw new IllegalArgumentException(
                    "Règle métier violée: Un LCN de nature ESCOMPTE ne peut être créé que pour des opérations d'ENCAISSEMENT. " +
                            "Type fourni: " + type + ". Veuillez corriger le type ou la nature du LCN.");
        }

        if (nature == NatureLcn.ESCOMPTE) {
            log.debug("Validation réussie: LCN ESCOMPTE avec type ENCAISSEMENT");
        }
    }


}