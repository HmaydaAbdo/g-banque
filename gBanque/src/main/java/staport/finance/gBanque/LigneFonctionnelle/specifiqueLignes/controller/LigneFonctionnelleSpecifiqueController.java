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
import staport.finance.gBanque.droitConstate.dtos.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.factoring.dtos.AgregatedSuivieFactoringLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.AgregatedSuiviePrefiLigneResponseDto;


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

    @GetMapping("/banque/{banqueId}")
    public ResponseEntity<Page<LigneFonctionnelleSpecifiqueResponse>> getLignesFonctionnellesSpecifiquesByBanqueId(
            @PathVariable Long banqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSpecifiqueSearchCriteria searchCriteria) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<LigneFonctionnelleSpecifiqueResponse> lignes = ligneFonctionnelleSpecifiqueService.getLignesFonctionnellesSpecifiquesByBanqueId(banqueId, searchCriteria, pageable);
        return ResponseEntity.ok(lignes);
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
        ligneFonctionnelleSpecifiqueService.deactivateLigneFonctionnelleById(ligneId);
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

    @GetMapping("/factoring-lines")
    public ResponseEntity<Page<SpecificLigneResposneDto>> getFactoringLines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute SpecifiqueLineSearchDto searchCriteria
    ){
        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<SpecificLigneResposneDto> resposne =ligneFonctionnelleSpecifiqueService.getFactoringLines(searchCriteria,pageable);
        return ResponseEntity.ok(resposne);
    }


    @GetMapping("/{id}/prefinancements")
    public ResponseEntity<AgregatedSuiviePrefiLigneResponseDto> getPrefinancementsByLineId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        AgregatedSuiviePrefiLigneResponseDto res = ligneFonctionnelleSpecifiqueService.getSuiviePrefiLigneResponse(id, pageable);
        return ResponseEntity.ok(res);
    }


    @GetMapping("/{id}/dc")
    public ResponseEntity<AgregatedSuivieDcLigneResponseDto> getDroitConstateByLineId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        AgregatedSuivieDcLigneResponseDto res = ligneFonctionnelleSpecifiqueService.getSuivieDcLigneResponse(id, pageable);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}/factoring")
    public ResponseEntity<AgregatedSuivieFactoringLigneResponseDto> getFactoringByLineId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        AgregatedSuivieFactoringLigneResponseDto res = ligneFonctionnelleSpecifiqueService.getSuivieFactoringLigneResponse(id, pageable);
        return ResponseEntity.ok(res);
    }
}