package staport.finance.gBanque.lcn.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.lcn.dtos.*;
import staport.finance.gBanque.lcn.services.LcnService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/lcns")
public class LcnController {

    private final LcnService lcnService;

    public LcnController(LcnService lcnService) {
        this.lcnService = lcnService;
    }


    @PostMapping()
    public ResponseEntity<LcnResponseDTO> createLcn(@RequestBody LcnRequestDto requestDto) {
        LcnResponseDTO createdLcn = lcnService.createLcn(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLcn);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LcnResponseDTO> updateLcn(@PathVariable Long id, @RequestBody LcnRequestDto requestDto) {
        return ResponseEntity.ok(lcnService.editEncaissementLcn(id, requestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LcnResponseDTO> getLcnById(@PathVariable Long id ){
        return ResponseEntity.ok(lcnService.getLcnById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLcn(@PathVariable Long id) {
        lcnService.deleteLcn(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/decaissements")
    public ResponseEntity<Page<LcnResponseDTO>> getAllDecaissements(
            LcnSearchDTO searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        Page<LcnResponseDTO> result = lcnService.getDecaissements(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissements")
    public ResponseEntity<Page<LcnResponseDTO>> getAllEncaissements(
            LcnSearchDTO searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        Page<LcnResponseDTO> result = lcnService.getEncaissements(searchDto, pageable);
        return ResponseEntity.ok(result);
    }


    @PatchMapping("/toggle/{id}")
    public ResponseEntity<LcnResponseDTO> toggleLcnStatus(@PathVariable Long id) {
        try {
            LcnResponseDTO updatedLcn = lcnService.toggleLcnStatus(id);
            return ResponseEntity.ok(updatedLcn);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    @GetMapping("/decaissements/encours/{banqueId}")
    public ResponseEntity<EncoursLcnResponse> getEncoursDecaissementsForLcn(
            @PathVariable Long banqueId,
            EncoursLcnSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEcheance") Pageable pageable) {

        EncoursLcnResponse result = lcnService.getEncoursDecaissementsForLcn(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissements/encours/{banqueId}")
    public ResponseEntity<EncoursLcnResponse> getEncoursEncaissementsForLcn(
            @PathVariable Long banqueId,
            EncoursLcnSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEcheance") Pageable pageable) {

        EncoursLcnResponse result = lcnService.getEncoursEncaissementsForLcn(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }
}