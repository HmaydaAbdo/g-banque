package staport.finance.gBanque.miseADisposition.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.miseADisposition.dtos.EncoursMiseAdisposition;
import staport.finance.gBanque.miseADisposition.dtos.EncoursMiseAdispositionSearchDto;
import staport.finance.gBanque.miseADisposition.dtos.MiseADispositionResponseDto;
import staport.finance.gBanque.miseADisposition.dtos.MiseADispositionSearchDto;
import staport.finance.gBanque.miseADisposition.services.MiseADispositionService;


import java.util.NoSuchElementException;

@RestController
@RequestMapping("/misesADisposition")
public class MiseADispositionController {

    private final MiseADispositionService miseADispositionService;

    public MiseADispositionController(MiseADispositionService miseADispositionService) {
        this.miseADispositionService = miseADispositionService;
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
