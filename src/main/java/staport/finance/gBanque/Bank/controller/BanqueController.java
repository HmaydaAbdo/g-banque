package staport.finance.gBanque.Bank.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import staport.finance.gBanque.Bank.dtos.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.dtos.BanqueSearchCriteria;
import staport.finance.gBanque.Bank.exceptions.BanqueExceptionHandler;
import staport.finance.gBanque.Bank.services.BankAccueilAggregatorService;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.common.PageableUtils;
import java.util.List;

/**
 * REST controller for the Banque module.

 * Injects {@link IBanqueService} (DIP — never the concrete implementation).
 * Exception handling is delegated to {@link BanqueExceptionHandler}.
 * No business logic lives here.
 *
 * <p>Lecture ouverte à tout utilisateur authentifié. Mutation (create/update/delete)
 * réservée à {@code SYSTEM_ADMIN} ou au département Finance (MEMBER ou DIRECTOR) —
 * {@code @PreAuthorize} posé par méthode plutôt qu'au niveau classe, car ce module
 * n'est pas uniformément restreint comme User/Department.
 */
@RestController
@RequestMapping("/banques")
public class BanqueController {

    private static final Logger logger = LoggerFactory.getLogger(BanqueController.class);

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";

    private final IBanqueService banqueService;
    private final BankAccueilAggregatorService accueilAggregatorService;

    public BanqueController(IBanqueService banqueService, BankAccueilAggregatorService accueilAggregatorService) {
        this.banqueService = banqueService;
        this.accueilAggregatorService = accueilAggregatorService;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<BanqueResponse> createBanque(
            @Valid @RequestPart("banque") BanqueRequest request,
            @RequestPart("logoFile") MultipartFile logoFile) {

        logger.info("POST /banques — création banque : {}", request.nom());
        BanqueResponse created = banqueService.createBanque(request, logoFile);
        logger.info("Banque créée — ID: {}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<BanqueResponse> getBanqueById(@PathVariable Long id) {
        logger.debug("GET /banques/{}", id);
        return ResponseEntity.ok(banqueService.getBanqueById(id));
    }

    @GetMapping
    public ResponseEntity<Page<BanqueResponse>> getAllBanques(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "id")   String[] sort,
            @ModelAttribute BanqueSearchCriteria searchCriteria) {

        // Guard: sort[1] may be absent when the caller passes only a field name.
        String         sortField = sort[0];
        Sort.Direction direction = (sort.length > 1)
                ? Sort.Direction.fromOptionalString(sort[1]).orElse(Sort.Direction.ASC)
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), Sort.by(direction, sortField));
        logger.debug("GET /banques — page={}, size={}, sort={} {}", page, size, sortField, direction);
        return ResponseEntity.ok(banqueService.getAllBanques(searchCriteria, pageable));
    }

    @GetMapping("/accueil-info")
    public ResponseEntity<List<BankAcceuilResponse>> getAccueilBankInformation() {
        logger.debug("GET /banques/accueil-info");
        return ResponseEntity.ok(accueilAggregatorService.getAccueilBankInfo());
    }

    @GetMapping("/accueil-info/{banqueId}")
    public ResponseEntity<BankAcceuilResponse> getAccueilBankInformationById(
            @PathVariable Long banqueId) {
        logger.debug("GET /banques/accueil-info/{}", banqueId);
        return ResponseEntity.ok(accueilAggregatorService.getAccueilBankInfoById(banqueId));
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<BanqueResponse> updateBanque(
            @PathVariable Long id,
            @Valid @RequestPart("banque") BanqueRequest request,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile) {

        logger.info("PUT /banques/{}", id);
        return ResponseEntity.ok(banqueService.updateBanque(id, request, logoFile));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Void> deleteBanque(@PathVariable Long id) {
        logger.info("DELETE /banques/{}", id);
        banqueService.deleteBanque(id);
        return ResponseEntity.noContent().build();
    }
}
