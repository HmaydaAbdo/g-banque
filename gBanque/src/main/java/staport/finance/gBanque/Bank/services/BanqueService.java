package staport.finance.gBanque.Bank.services;

import staport.finance.gBanque.Bank.dtos.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.dtos.BanqueSearchCriteria;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.mappers.BanqueMapper;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.Bank.specifications.BanqueSpecification;;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.FileStorageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BanqueService {

    private static final Logger logger = LoggerFactory.getLogger(BanqueService.class);

    private final BanqueRepository banqueRepository;
    private final BanqueMapper banqueMapper;
    private final NatureLigneFonctionnelleService natureLigneFonctionnelleService;
    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;

    public BanqueService(BanqueRepository banqueRepository,
                         BanqueMapper banqueMapper,
                         NatureLigneFonctionnelleService natureLigneFonctionnelleService, LigneFonctionnelleRepository ligneFonctionnelleRepository) {
        this.banqueRepository = banqueRepository;
        this.banqueMapper = banqueMapper;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
    }



    @Transactional
    public BanqueResponse createBanque(BanqueRequest request, MultipartFile logoFile) {
        logger.info("Creating banque with : {}", request);

        if (banqueRepository.findByNumeroDeCompte(request.getNumeroDeCompte()).isPresent()) {
            logger.error("Duplicate account number detected: {}", request.getNumeroDeCompte());
            throw new DuplicateEntryException("Le numéro de compte existe déjà: " + request.getNumeroDeCompte());
        }

        Banque banque = banqueMapper.toEntity(request);

        if (logoFile != null && !logoFile.isEmpty()) {
            logger.debug("Processing logo file: {} (size: {} bytes)",
                    logoFile.getOriginalFilename(), logoFile.getSize());
            try {
                banque.setLogoData(logoFile.getBytes());
                logger.debug("Logo file successfully processed and attached to banque");
            } catch (IOException e) {
                logger.error("Failed to read logo file data for banque: {}", request.getNom(), e);
                throw new FileStorageException("Impossible de lire les données du fichier logo.", e);
            }
        } else {
            logger.error("Logo file is required but was not provided for banque: {}", request.getNom());
            throw new FileStorageException("Le logo est obligatoire pour la creation d'une banque");
        }

        Banque savedBanque = banqueRepository.save(banque);
        logger.info("Successfully created banque with ID: {}, Name: {}", savedBanque.getId(), savedBanque.getNom());
        return banqueMapper.toDto(savedBanque);
    }


    public BanqueResponse getBanqueById(Long id) {
        logger.info("Fetching banque by ID: {}", id);

        Banque banque = banqueRepository.findById(id)
                .orElseThrow(() ->{
                    logger.error("Banque not found with ID: {}", id);
                    return new ResourceNotFoundException("Banque non trouvée avec l'ID: " + id);
                }) ;
        logger.debug("Successfully retrieved banque: {} (ID: {})", banque.getNom(), id);

        return banqueMapper.toDto(banque);
    }


    public Page<BanqueResponse> getAllBanques(BanqueSearchCriteria criteria, Pageable pageable) {
        logger.info("Fetching all banques with criteria: {}, Page: {}, Size: {}", criteria, pageable.getPageNumber(), pageable.getPageSize());
        Specification<Banque> spec = BanqueSpecification.searchByCriteria(criteria);
        Page<Banque> banquesPage = banqueRepository.findAll(spec, pageable);
        logger.info("Retrieved {} banque(s) out of {} total", banquesPage.getNumberOfElements(), banquesPage.getTotalElements());
        logger.debug("Banques page details - Current page: {}/{}, Elements: {}", banquesPage.getNumber() + 1, banquesPage.getTotalPages(), banquesPage.getNumberOfElements());
        return banquesPage.map(banqueMapper::toDto);
    }


    @Transactional
    public BanqueResponse updateBanque(Long id, BanqueRequest request, MultipartFile logoFile) {
        logger.info("Updating banque with ID: {}", id);
        logger.debug("Update request details : {}", request.getNom());

        Banque existingBanque = banqueRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Banque not found for update with ID: {}", id);
                    return new ResourceNotFoundException("Banque non trouvée avec l'ID: " + id);
                });
        logger.debug("Found existing banque: {} (ID: {})", existingBanque.getNom(), id);


        banqueMapper.updateBanqueFromDto(request, existingBanque);
        if (logoFile != null && !logoFile.isEmpty()) {
            logger.debug("Updating logo file: {} (size: {} bytes)", logoFile.getOriginalFilename(), logoFile.getSize());
            try {
                existingBanque.setLogoData(logoFile.getBytes());
                logger.debug("Logo file successfully updated for banque ID: {}", id);
            } catch (IOException e) {
                logger.error("Failed to read logo file data for banque update ID: {}", id, e);
                throw new FileStorageException("Impossible de lire les données du fichier logo pour la mise à jour.", e);
            }
        } else {
            logger.debug("No new logo file provided for update, keeping existing logo");
        }

        Banque updatedBanque = banqueRepository.save(existingBanque);
        banqueRepository.flush();
        logger.info("Successfully updated banque ID: {}, Name: {}", id, updatedBanque.getNom());
        return banqueMapper.toDto(updatedBanque);
        }


    @Transactional(readOnly = true)
    public List<BankAcceuilResponse> getAccueilBankInfo() {
        logger.info("Fetching accueil information for all banques");

        List<Banque> allBanques = banqueRepository.findAll();
        logger.debug("Found {} banque(s) in database", allBanques.size());

        Long faciliteDeCaisseId = findFaciliteDeCaisseNatureId();
        logger.debug("Facilité de Caisse nature ID: {}", faciliteDeCaisseId);

        List<BankAcceuilResponse> responses = allBanques.stream()
                .map(banque -> {
                    logger.debug("Processing accueil info for banque ID: {}, Name: {}",
                            banque.getId(), banque.getNom());

                    BankAcceuilResponse response = banqueMapper.toAcceuilDto(banque);

                    Optional<LigneFonctionnelle> fcOptional =
                            ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(
                                    banque.getId(), faciliteDeCaisseId
                            );

                    if (fcOptional.isPresent()) {
                        BigDecimal plafond = fcOptional.get().getPlafond();
                        response.setPlafondFC(plafond);
                        logger.debug("Set plafond FC for banque ID {}: {}", banque.getId(), plafond);
                    } else {
                        logger.debug("No active Facilité de Caisse found for banque ID: {}", banque.getId());
                    }

                    return response;
                })
                .collect(Collectors.toList());

        logger.info("Successfully retrieved accueil info for {} banque(s)", responses.size());
        return responses;

    }

    @Transactional(readOnly = true)
    public BankAcceuilResponse getAccueilBankInfoById(Long banqueId) {
        logger.info("Fetching accueil information for banque ID: {}", banqueId);

        Banque banque = banqueRepository.findById(banqueId)
                .orElseThrow(() -> {
                    logger.error("Banque not found for accueil info with ID: {}", banqueId);
                    return new ResourceNotFoundException("Banque not found with id: " + banqueId);
                });

        logger.debug("Found banque: {} for accueil info", banque.getNom());

        Long faciliteDeCaisseId = findFaciliteDeCaisseNatureId();
        logger.debug("Facilité de Caisse nature ID: {}", faciliteDeCaisseId);

        BankAcceuilResponse response = banqueMapper.toAcceuilDto(banque);

        Optional<LigneFonctionnelle> fc=ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banqueId,faciliteDeCaisseId);

        if (fc.isPresent()) {
            BigDecimal plafond = fc.get().getPlafond();
            response.setPlafondFC(plafond);
            logger.debug("Set plafond FC for banque ID {}: {}", banqueId, plafond);
        } else {
            logger.debug("No active Facilité de Caisse found for banque ID: {}", banqueId);
        }

        logger.info("Successfully retrieved accueil info for banque ID: {}", banqueId);

        return response;
    }

    @Transactional(readOnly = true)
    public BigDecimal getPlafoncFC(Long id) {
        logger.info("Fetching Plafond FC for banque ID: {}", id);

        Long faciliteDeCaisseId = findFaciliteDeCaisseNatureId();
        logger.debug("Facilité de Caisse nature ID: {}", faciliteDeCaisseId);

        Optional<LigneFonctionnelle> fc = ligneFonctionnelleRepository
                .findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(id, faciliteDeCaisseId);

        if (fc.isPresent()) {
            BigDecimal plafond = fc.get().getPlafond();
            logger.info("Plafond FC found for banque ID {}: {}", id, plafond);
            return plafond;
        } else {
            logger.warn("No active Facilité de Caisse found for banque ID: {}, returning ZERO", id);
            return BigDecimal.ZERO;
        }
    }

    @Transactional
    public void deleteBanque(Long id) throws Exception {
        logger.info("Attempting to delete banque with ID: {}", id);

        Banque banque = banqueRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Banque not found for deletion with ID: {}", id);
                    return new ResourceNotFoundException("Banque non trouvée avec l'ID: " + id);
                });

        logger.debug("Found banque to delete: {} (ID: {})", banque.getNom(), id);

        boolean hasActiveLignes = banque.getLigneFonctionnelles().stream()
                .anyMatch(LigneFonctionnelle::isActif);

        if (hasActiveLignes) {
            logger.error("Cannot delete banque ID: {} - has {} active lignes fonctionnelles",
                    id, banque.getLigneFonctionnelles().stream()
                            .filter(LigneFonctionnelle::isActif).count());
            throw new Exception("Impossible de supprimer cette banque car elle possède des lignes fonctionnelles actives.");
        }

        logger.debug("No active lignes fonctionnelles found, proceeding with deletion");
        banqueRepository.delete(banque);
        logger.info("Successfully deleted banque ID: {}, Name: {}", id, banque.getNom());
    }


    private Long findFaciliteDeCaisseNatureId() {
        logger.debug("Fetching 'Facilité de Caisse' nature ID");

        Long natureId = natureLigneFonctionnelleService.getNatureByLabel("Facilité de Caisse").getId();

        logger.debug("Found 'Facilité de Caisse' nature ID: {}", natureId);
        return natureId;
    }

    @Transactional
    public void debiter(Long banqueId, BigDecimal montant) {
        logger.info("Debiting banque ID: {} with amount: {}", banqueId, montant);

        Banque banque = banqueRepository.findById(banqueId)
                .orElseThrow(() -> {
                    logger.error("Banque not found for debit operation with ID: {}", banqueId);
                    return new RuntimeException("banque non trouvée avec l id " + banqueId);
                });

        BigDecimal soldeBefore = banque.getSoldeActuel();
        BigDecimal soldeAfter = soldeBefore.subtract(montant);

        banque.setSoldeActuel(soldeAfter);
        banqueRepository.save(banque);

        logger.info("Successfully debited banque ID: {} - Before: {}, After: {}, Amount: {}",
                banqueId, soldeBefore, soldeAfter, montant);
        logger.debug("Banque {} new balance: {}", banque.getNom(), soldeAfter);
    }

    @Transactional
    public void crediter(Long banqueId, BigDecimal montant) {
        logger.info("Crediting banque ID: {} with amount: {}", banqueId, montant);

        Banque banque = banqueRepository.findById(banqueId)
                .orElseThrow(() -> {
                    logger.error("Banque not found for credit operation with ID: {}", banqueId);
                    return new RuntimeException("banque non trouvée avec l id " + banqueId);
                });

        BigDecimal soldeBefore = banque.getSoldeActuel();
        BigDecimal soldeAfter = soldeBefore.add(montant);

        banque.setSoldeActuel(soldeAfter);
        banqueRepository.save(banque);

        logger.info("Successfully credited banque ID: {} - Before: {}, After: {}, Amount: {}",
                banqueId, soldeBefore, soldeAfter, montant);
        logger.debug("Banque {} new balance: {}", banque.getNom(), soldeAfter);
    }
}
