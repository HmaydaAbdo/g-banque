package staport.finance.gBanque.Bank.controller;

import staport.finance.gBanque.Bank.dtos.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.dtos.BanqueSearchCriteria;
import staport.finance.gBanque.Bank.services.BanqueService;
import org.springframework.beans.factory.annotation.Autowired;
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

        private final BanqueService banqueService;

        @Autowired
        public BanqueController(BanqueService banqueService) {
            this.banqueService = banqueService;
        }

        /**
         * Create a new bank account with an optional logo file.
         * POST /banques
         * The request should be 'multipart/form-data'.
         * 'banque' part for JSON data, 'logoFile' part for the image file.
         *
         * @param request The DTO containing bank details (JSON part).
         * @param logoFile The MultipartFile representing the logo image (file part).
         * @return The created bank as a DTO with HTTP status 201 CREATED.
         */
        @PostMapping
        public ResponseEntity<BanqueResponse> createBanque(
                @Valid @RequestPart("banque") BanqueRequest request,
                @RequestPart(value = "logoFile") MultipartFile logoFile) {
            BanqueResponse createdBanque = banqueService.createBanque(request, logoFile);
            return new ResponseEntity<>(createdBanque, HttpStatus.CREATED);
        }

        /**
         * Get a bank account by its ID.
         * GET /banques/{id}
         * @param id The ID of the bank account.
         * @return The bank as a DTO with HTTP status 200 OK.
         */
        @GetMapping("/{id}")
        public ResponseEntity<BanqueResponse> getBanqueById(@PathVariable Long id) {
            BanqueResponse banque = banqueService.getBanqueById(id);
            return ResponseEntity.ok(banque);
        }

        /**
         * Get essential bank information for the Accueil component.
         * GET /banques/accueil-info
         * @return A list of BankAcceuilResponse DTOs with HTTP status 200 OK.
         */
        @GetMapping("/accueil-info")
        public ResponseEntity<List<BankAcceuilResponse>> getAccueilBankInformation() {
            List<BankAcceuilResponse> bankInfo = banqueService.getAccueilBankInfo();
            return ResponseEntity.ok(bankInfo);
        }

        @GetMapping("/accueil-info/{banqueId}")
        public ResponseEntity<BankAcceuilResponse> getAccueilBankInformationById(@PathVariable Long banqueId) {
            BankAcceuilResponse bankInfo = banqueService.getAccueilBankInfoById(banqueId);
            return ResponseEntity.ok(bankInfo);
        }

        /**
         * Get all bank accounts with advanced search, pagination, and sorting.
         * GET /banques?page={page}&size={size}&sort={field,direction}&nom={nom}&numeroDeCompte={num}&minSolde={min}&maxSolde={max}
         *
         * @param page The page number (0-indexed).
         * @param size The number of items per page.
         * @param sort The sorting criteria (e.g., "nom,asc" or "dateCreation,desc").
         * @param searchCriteria DTO for specific search fields (nom, numeroDeCompte, minSolde, maxSolde).
         * @return A paginated list of bank accounts as DTOs with HTTP status 200 OK.
         */
        @GetMapping
        public ResponseEntity<Page<BanqueResponse>> getAllBanques(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size,
                @RequestParam(defaultValue = "id,asc") String[] sort,
                @ModelAttribute BanqueSearchCriteria searchCriteria) {

            Sort.Direction direction = Sort.Direction.fromString(sort[1]);
            Sort sortOrder = Sort.by(direction, sort[0]);
            Pageable pageable = PageRequest.of(page, size, sortOrder);

            Page<BanqueResponse> banques = banqueService.getAllBanques(searchCriteria, pageable);
            return ResponseEntity.ok(banques);
        }

        /**
         * Update an existing bank account with optional new logo file.
         * PUT /banques/{id}
         * The request should be 'multipart/form-data'.
         * 'banque' part for JSON data, 'logoFile' part for the image file.
         *
         * @param id The ID of the bank account to update.
         * @param request The DTO containing updated bank details (JSON part).
         * @param logoFile The MultipartFile representing the new logo image (file part, optional).
         * @return The updated bank as a DTO with HTTP status 200 OK.
         */
        @PutMapping("/{id}")
        public ResponseEntity<BanqueResponse> updateBanque(
                @PathVariable Long id,
                @Valid @RequestPart("banque") BanqueRequest request,
                @RequestPart(value = "logoFile", required = false) MultipartFile logoFile) {
            BanqueResponse updatedBanque = banqueService.updateBanque(id, request, logoFile);
            return ResponseEntity.ok(updatedBanque);
        }

        /**
         * Delete a bank account by its ID.
         * DELETE /banques/{id}
         * @param id The ID of the bank account to delete.
         * @return HTTP status 204 NO CONTENT.
         */
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteBanque(@PathVariable Long id) throws Exception {
            banqueService.deleteBanque(id);
            return ResponseEntity.noContent().build();
        }
    }