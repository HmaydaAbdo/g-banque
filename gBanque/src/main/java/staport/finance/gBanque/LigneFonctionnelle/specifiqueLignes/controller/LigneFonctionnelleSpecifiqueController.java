package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.controller;


import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.*;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueService;

@RestController
@RequestMapping("/lignes-fonctionnelles-specifiques")
public class LigneFonctionnelleSpecifiqueController {

    private final LigneFonctionnelleSpecifiqueService ligneFonctionnelleSpecifiqueService;


    public LigneFonctionnelleSpecifiqueController(LigneFonctionnelleSpecifiqueService ligneFonctionnelleSpecifiqueService) {
        this.ligneFonctionnelleSpecifiqueService = ligneFonctionnelleSpecifiqueService;
    }

    @PostMapping
    public ResponseEntity<LigneFonctionnelleSpecifiqueResponse> createLigneFonctionnelleSpecifique(@Valid @RequestBody LigneFonctionnelleSpecifiqueRequest request) {
        LigneFonctionnelleSpecifiqueResponse createdLigne = ligneFonctionnelleSpecifiqueService.createLigneFonctionnelleSpecifique(request);
        return new ResponseEntity<>(createdLigne, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LigneFonctionnelleSpecifiqueResponse> getLigneFonctionnelleSpecifiqueById(@PathVariable Long id) {
        LigneFonctionnelleSpecifiqueResponse ligne = ligneFonctionnelleSpecifiqueService.getLigneFonctionnelleSpecifiqueById(id);
        return ResponseEntity.ok(ligne);
    }

    @GetMapping
    public ResponseEntity<Page<LigneFonctionnelleSpecifiqueResponse>> getLignesFonctionnellesSpecifiques(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSpecifiqueSearchCriteria searchCriteria) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<LigneFonctionnelleSpecifiqueResponse> lignes = ligneFonctionnelleSpecifiqueService.getLignesFonctionnellesSpecifiques(searchCriteria, pageable);
        return ResponseEntity.ok(lignes);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LigneFonctionnelleSpecifiqueResponse> updateLigneFonctionnelleSpecifique(
            @PathVariable Long id,
            @Valid @RequestBody LigneFonctionnelleSpecifiqueRequest request) {
        LigneFonctionnelleSpecifiqueResponse updatedLigne = ligneFonctionnelleSpecifiqueService.updateLigneFonctionnelleSpecifique(id, request);
        return ResponseEntity.ok(updatedLigne);
    }

    /**
     * Deactivates a specific  functional line by id
     * PUT /lignes-fonctionnelles-specifique/deactivate/{ligneId}
     * @param ligneId The ID of the functional line.
     * @return HTTP status 204 No Content on success.
     */
    @PutMapping("/deactivate/{ligneId}")
    public ResponseEntity<Void> deactivateLigneFonctionnelle(
            @PathVariable Long ligneId)
    {
        ligneFonctionnelleSpecifiqueService.deactivateLigneFonctionnelleByNatureAndBank(ligneId);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLigneFonctionnelleSpecifique(@PathVariable Long id) {
        ligneFonctionnelleSpecifiqueService.deleteLigneFonctionnelleSpecifique(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/prefi-lines")
    public ResponseEntity<Page<SpecificLigneResposneDto>> getPrefiLines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute SpecifiqueLineSearchDto searchCriteria
    ){
        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<SpecificLigneResposneDto> prefinancementResponseDtoPage =ligneFonctionnelleSpecifiqueService.getPrefiLines(searchCriteria,pageable);
        return ResponseEntity.ok(prefinancementResponseDtoPage);
    }

    @GetMapping("/dc-lines")
    public ResponseEntity<Page<SpecificLigneResposneDto>> getDcLines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute SpecifiqueLineSearchDto searchCriteria
    ){
        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<SpecificLigneResposneDto> resposne =ligneFonctionnelleSpecifiqueService.getDCLines(searchCriteria,pageable);
        return ResponseEntity.ok(resposne);
    }
}