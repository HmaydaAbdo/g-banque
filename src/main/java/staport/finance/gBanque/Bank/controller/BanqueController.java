package staport.finance.gBanque.Bank.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import staport.finance.gBanque.Bank.dtos.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.dtos.BanqueSearchCriteria;
import staport.finance.gBanque.Bank.services.BanqueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/banques")
public class BanqueController {

    private static final Logger logger = LoggerFactory.getLogger(BanqueController.class);
    private final BanqueService banqueService;

    public BanqueController(BanqueService banqueService) {
        this.banqueService = banqueService;
        logger.info("BanqueController initialized successfully");
    }

    @PostMapping
    public ResponseEntity<BanqueResponse> createBanque(
            @Valid @RequestPart("banque") BanqueRequest request,
            @RequestPart(value = "logoFile") MultipartFile logoFile) {

        logger.info("Creating new banque with name: {}", request.getNom());
        logger.debug("Banque creation request details: {}, Logo file: {} (size: {} bytes)",
                request, logoFile.getOriginalFilename(), logoFile.getSize());

        BanqueResponse createdBanque = banqueService.createBanque(request, logoFile);

        logger.info("Successfully created banque with ID: {} and name: {}",
                createdBanque.getId(), createdBanque.getNom());
        return new ResponseEntity<>(createdBanque, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BanqueResponse> getBanqueById(@PathVariable Long id) {
        logger.info("Fetching banque by ID: {}", id);

        BanqueResponse banque = banqueService.getBanqueById(id);

        logger.debug("Successfully retrieved banque: {} (ID: {})", banque.getNom(), id);
        return ResponseEntity.ok(banque);
    }

    @GetMapping("/accueil-info")
    public ResponseEntity<List<BankAcceuilResponse>> getAccueilBankInformation() {
        logger.info("Fetching all bank accueil information");

        List<BankAcceuilResponse> bankInfo = banqueService.getAccueilBankInfo();

        logger.info("Successfully retrieved {} bank accueil record(s)", bankInfo.size());
        return ResponseEntity.ok(bankInfo);
    }

    @GetMapping("/accueil-info/{banqueId}")
    public ResponseEntity<BankAcceuilResponse> getAccueilBankInformationById(@PathVariable Long banqueId) {
        logger.info("Fetching bank accueil info for banque ID: {}", banqueId);

        BankAcceuilResponse bankInfo = banqueService.getAccueilBankInfoById(banqueId);

        logger.debug("Successfully retrieved accueil info for banque ID: {}", banqueId);
        return ResponseEntity.ok(bankInfo);
    }

    @GetMapping
    public ResponseEntity<Page<BanqueResponse>> getAllBanques(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute BanqueSearchCriteria searchCriteria) {

        logger.info("Fetching banques - Page: {}, Size: {}, Sort: {} {}",
                page, size, sort[0], sort[1]);
        logger.debug("Search criteria applied: {}", searchCriteria);

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<BanqueResponse> banques = banqueService.getAllBanques(searchCriteria, pageable);

        logger.info("Successfully retrieved {} banque(s) out of {} total (Page {}/{})",
                banques.getNumberOfElements(),
                banques.getTotalElements(),
                banques.getNumber() + 1,
                banques.getTotalPages());

        return ResponseEntity.ok(banques);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BanqueResponse> updateBanque(
            @PathVariable Long id,
            @Valid @RequestPart("banque") BanqueRequest request,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile) {

        logger.info("Updating banque with ID: {}", id);
        logger.debug("Update request details: {}, New logo provided: {}",
                request, logoFile != null);

        BanqueResponse updatedBanque = banqueService.updateBanque(id, request, logoFile);
        logger.info("Successfully updated banque ID: {} with name: {}", id, updatedBanque.getNom());
        return ResponseEntity.ok(updatedBanque);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanque(@PathVariable Long id) throws Exception {
        logger.info("Attempting to delete banque with ID: {}", id);
        banqueService.deleteBanque(id);
        logger.info("Successfully deleted banque with ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}