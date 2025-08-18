package staport.finance.gBanque.leasing.lesOrganismsLeasing.controllers;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingRequestDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingResponseDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingSearchDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.services.interfaces.OrganismeLeasingService;


@RestController
@RequestMapping("/0rganismesLeasing")
public class OrganismeLeasingController {

    private final OrganismeLeasingService organismeLeasingService;

    public OrganismeLeasingController(OrganismeLeasingService organismeLeasingService) {
        this.organismeLeasingService = organismeLeasingService;
    }

    @PostMapping
    public ResponseEntity<OrganismeLeasingResponseDTO> createOrganismeLeasing(@Valid @RequestBody OrganismeLeasingRequestDTO requestDTO) {
        // requestDTO no longer contains banqueId, so no changes needed here.
        OrganismeLeasingResponseDTO createdOrganisme = organismeLeasingService.create(requestDTO);
        return new ResponseEntity<>(createdOrganisme, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganismeLeasingResponseDTO> updateOrganismeLeasing(
            @PathVariable Long id,
            @Valid @RequestBody OrganismeLeasingRequestDTO requestDTO) {
        // requestDTO no longer contains banqueId, so no changes needed here.
        OrganismeLeasingResponseDTO updatedOrganisme = organismeLeasingService.update(id, requestDTO);
        return ResponseEntity.ok(updatedOrganisme);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganismeLeasingResponseDTO> getOrganismeLeasingById(@PathVariable Long id) {
        OrganismeLeasingResponseDTO organisme = organismeLeasingService.getById(id);
        return ResponseEntity.ok(organisme);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganismeLeasing(@PathVariable Long id) {
        organismeLeasingService.delete(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping
    public ResponseEntity<Page<OrganismeLeasingResponseDTO>> searchOrganismeLeasings(
            // OrganismeLeasingSearchDTO no longer contains banqueId,
            // so this will automatically bind only 'nom' and other fields if added.
            OrganismeLeasingSearchDTO criteria,
            Pageable pageable) {
        Page<OrganismeLeasingResponseDTO> organismes = organismeLeasingService.search(criteria, pageable);
        return ResponseEntity.ok(organismes);
    }
}