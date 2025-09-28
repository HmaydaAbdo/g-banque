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



    /**
     * Creates a new bank account from a DTO and a logo file.
     * @param request The BanqueRequest DTO containing bank details.
     * @param logoFile The MultipartFile representing the logo image.
     * @return The created BanqueResponse DTO.
     * @throws FileStorageException if there's an issue processing the logo file.
     */

    @Transactional
    public BanqueResponse createBanque(BanqueRequest request, MultipartFile logoFile) {

        if (banqueRepository.findByNumeroDeCompte(request.getNumeroDeCompte()).isPresent()) {
            throw new DuplicateEntryException("Le numéro de compte existe déjà: " + request.getNumeroDeCompte());
        }

        Banque banque = banqueMapper.toEntity(request);

        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                banque.setLogoData(logoFile.getBytes());
            } catch (IOException e) {
                throw new FileStorageException("Impossible de lire les données du fichier logo.", e);
            }
        } else {
            throw new FileStorageException("Le logo est obligatoire pour la creation d'une banque");
        }

        Banque savedBanque = banqueRepository.save(banque);
        return banqueMapper.toDto(savedBanque);
    }

    /**
     * Retrieves a bank account by ID and maps it to a DTO.
     * @param id The ID of the bank account.
     * @return The BanqueResponse DTO.
     * @throws ResourceNotFoundException if the bank is not found.
     */
    public BanqueResponse getBanqueById(Long id) {
        Banque banque = banqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée avec l'ID: " + id));
        return banqueMapper.toDto(banque);
    }

    /**
     * Retrieves all bank accounts with pagination, sorting, and advanced filtering using JpaSpecification.
     *
     * @param criteria BanqueSearchCriteria for specific field filters (nom, numeroDeCompte, minSolde, maxSolde).
     * @param pageable Pagination and sorting information.
     * @return A Page of BanqueResponse DTOs.
     */
    public Page<BanqueResponse> getAllBanques(BanqueSearchCriteria criteria, Pageable pageable) {
        Specification<Banque> spec = BanqueSpecification.searchByCriteria(criteria);
        Page<Banque> banquesPage = banqueRepository.findAll(spec, pageable);
        return banquesPage.map(banqueMapper::toDto);
    }

    /**
     * Updates an existing bank account from a DTO and an optional logo file.
     * @param id The ID of the bank account to update.
     * @param request The BanqueRequest DTO with updated details.
     * @param logoFile The MultipartFile representing the new logo image (optional).
     * @return The updated BanqueResponse DTO.
     * @throws ResourceNotFoundException if the bank is not found.
     * @throws FileStorageException if there's an issue processing the logo file.
     */
    @Transactional
    public BanqueResponse updateBanque(Long id, BanqueRequest request, MultipartFile logoFile) {
        Banque existingBanque = banqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée avec l'ID: " + id));

        banqueMapper.updateBanqueFromDto(request, existingBanque);
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                existingBanque.setLogoData(logoFile.getBytes());
            } catch (IOException e) {
                throw new FileStorageException("Impossible de lire les données du fichier logo pour la mise à jour.", e);
            }
        }
        Banque updatedBanque = banqueRepository.save(existingBanque);
        banqueRepository.flush();
        return banqueMapper.toDto(updatedBanque);
    }


    @Transactional(readOnly = true)
    public List<BankAcceuilResponse> getAccueilBankInfo() {

        List<Banque> allBanques = banqueRepository.findAll();

        Long faciliteDeCaisseId = findFaciliteDeCaisseNatureId();

        return allBanques.stream()
                .map(banque -> {
                    BankAcceuilResponse response = banqueMapper.toAcceuilDto(banque);
                    // Only attempt to get functional line if the nature ID is present
                    // If not found, plafondFC will remain null (its default value after mapping toDto)
                    Optional<LigneFonctionnelle> fcOptional =
                            ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(
                                    banque.getId(),faciliteDeCaisseId
                            );

                    fcOptional.ifPresent(fc -> response.setPlafondFC(fc.getPlafond()));


                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BankAcceuilResponse getAccueilBankInfoById(Long banqueId) {

        Banque banque = banqueRepository.findById(banqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Banque not found with id: " + banqueId));

        Long faciliteDeCaisseId = findFaciliteDeCaisseNatureId();

        BankAcceuilResponse response = banqueMapper.toAcceuilDto(banque);

        Optional<LigneFonctionnelle> fc=ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banqueId,faciliteDeCaisseId);

        fc.ifPresent(f -> response.setPlafondFC(f.getPlafond()));

        return response;
    }

    /**
     * Deletes a bank account.
     * @param id The ID of the bank account to delete.
     * @throws ResourceNotFoundException if the bank is not found.
     */
    @Transactional
    public void deleteBanque(Long id) throws Exception {
        Banque banque = banqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée avec l'ID: " + id));

        boolean hasActiveLignes = banque.getLigneFonctionnelles().stream()
                .anyMatch(LigneFonctionnelle::isActif);

        if (hasActiveLignes) {
            throw new Exception("Impossible de supprimer cette banque car elle possède des lignes fonctionnelles actives.");
        }

        banqueRepository.delete(banque);
    }

    /**
     * Tries to find the ID for the "Facilité de Caisse" nature of functional line.
     * Logs a warning if not found, but does not throw an exception.
     * @return An Optional containing the ID if found, or an empty Optional otherwise.
     */
    private Long findFaciliteDeCaisseNatureId() {

        return natureLigneFonctionnelleService.getNatureByLabel("Facilité de Caisse").getId();

    }


    @Transactional
    private void debiter(Long banqueId, BigDecimal montant) {
        Banque banque=banqueRepository.findById(banqueId).orElseThrow(()->new RuntimeException("banque non trouvée avec l id "+banqueId));
        banque.setSoldeActuel( banque.getSoldeActuel().subtract(montant));
        banqueRepository.save(banque);
    }


    @Transactional
    private void crediter(Long banqueId,BigDecimal montant) {
        Banque banque=banqueRepository.findById(banqueId).orElseThrow(()->new RuntimeException("banque non trouvée avec l id "+banqueId));
        banque.setSoldeActuel( banque.getSoldeActuel().add(montant));
        banqueRepository.save(banque);
    }
}
