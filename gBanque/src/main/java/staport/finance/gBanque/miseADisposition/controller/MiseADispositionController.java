package staport.finance.gBanque.miseADisposition.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.miseADisposition.dtos.*;
import staport.finance.gBanque.miseADisposition.services.MiseADispositionService;


import java.util.NoSuchElementException;

@RestController
@RequestMapping("/misesADisposition")
public class MiseADispositionController {

    private final MiseADispositionService miseADispositionService;

    public MiseADispositionController(MiseADispositionService miseADispositionService) {
        this.miseADispositionService = miseADispositionService;
    }


    @PostMapping
    public ResponseEntity<MiseADispositionResponseDto> createMiseAdisposition(@RequestBody MiseAdispositionRequestDto requestDto) {
        return ResponseEntity.ok(miseADispositionService.createEncaissementMiseAdisposition(requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MiseADispositionResponseDto> updateMiseAdisposition(@PathVariable Long id, @RequestBody MiseAdispositionRequestDto requestDto) {
        return ResponseEntity.ok(miseADispositionService.editEncaissementMiseAdisposition(id, requestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MiseADispositionResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(miseADispositionService.getMiseAdispositionById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMiseAdisposition(@PathVariable Long id) {
        miseADispositionService.deleteMiseAdispositionById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/decaissements")
    public ResponseEntity<Page<MiseADispositionResponseDto>> getDecaissementMisesADisposition(
            MiseADispositionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        Page<MiseADispositionResponseDto> result = miseADispositionService.getDecaissementForMiseAdisposition(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissements")
    public ResponseEntity<Page<MiseADispositionResponseDto>> getEncaissementMisesADisposition(
            MiseADispositionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        Page<MiseADispositionResponseDto> result = miseADispositionService.getEncaissementForMiseAdisposition(searchDto, pageable);
        return ResponseEntity.ok(result);
    }


    @PatchMapping("/toggle/{id}")
    public ResponseEntity<MiseADispositionResponseDto> toggleMiseADispositionStatus(@PathVariable Long id) {
        try {
            MiseADispositionResponseDto updatedMiseADisposition = miseADispositionService.toggleMiseADispositionStatus(id);
            return ResponseEntity.ok(updatedMiseADisposition);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }



    @GetMapping("/encours/decaissements/{banqueId}")
    public ResponseEntity<EncoursMiseAdisposition> getEncoursDecaissementsForMiseADispositionByBank(
            @PathVariable Long banqueId,
            EncoursMiseAdispositionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        EncoursMiseAdisposition result = miseADispositionService.getEncoursDecaissementsForMiseADispositionByBank(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/encours/encaissements/{banqueId}")
    public ResponseEntity<EncoursMiseAdisposition> getEncoursEncaissementsForMiseADispositionByBank(
            @PathVariable Long banqueId,
            EncoursMiseAdispositionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        EncoursMiseAdisposition result = miseADispositionService.getEncoursEncaissementsForMiseADispositionByBank(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }
}
