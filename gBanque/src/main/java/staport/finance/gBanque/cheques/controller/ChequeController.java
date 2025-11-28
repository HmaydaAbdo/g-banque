package staport.finance.gBanque.cheques.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.cheques.dtos.*;
import staport.finance.gBanque.cheques.services.ChequeService;


import java.util.NoSuchElementException;

@RestController
@RequestMapping("/cheques")
public class ChequeController {

    private final ChequeService chequeService;

    public ChequeController(ChequeService chequeService) {
        this.chequeService = chequeService;
    }


    @PostMapping
    public ResponseEntity<ChequeResponseDto> createCheque(@RequestBody ChequeRequestDto requestDto) {
        return ResponseEntity.ok(chequeService.createEncaissementCheque(requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChequeResponseDto> updateCheque(@PathVariable Long id, @RequestBody ChequeRequestDto requestDto) {
        return ResponseEntity.ok(chequeService.editEncaissementCheque(id, requestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChequeResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(chequeService.getChequeById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCheque(@PathVariable Long id) {
        chequeService.deleteCheque(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/decaissements")
    public ResponseEntity<Page<ChequeResponseDto>> getAllDecaissement(
            ChequeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        Page<ChequeResponseDto> result = chequeService.getAllDecaissement(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissements")
    public ResponseEntity<Page<ChequeResponseDto>> getAllEncaissement(
            ChequeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        Page<ChequeResponseDto> result = chequeService.getAllEncaissement(searchDto, pageable);
        return ResponseEntity.ok(result);
    }


    @PatchMapping("/toggle/{id}")
    public ResponseEntity<ChequeResponseDto> toggleChequeStatus(@PathVariable long id) {
        try {
            ChequeResponseDto updatedCheque = chequeService.toggleChequeStatus(id);
            return ResponseEntity.ok(updatedCheque);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }



    @GetMapping("/decaissements/encours/{banqueId}")
    public ResponseEntity<EncoursChequeResponseDto> getEncoursDecaissementForCheques(
            @PathVariable Long banqueId,
            EncoursChequeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        return ResponseEntity.ok(chequeService.getEncoursDecaissementForCheques(searchDto,banqueId, pageable));
    }

    @GetMapping("/encaissements/encours/{banqueId}")
    public ResponseEntity<EncoursChequeResponseDto> getEncoursEncaissementForCheques(
            @PathVariable Long banqueId,
            EncoursChequeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        return ResponseEntity.ok(chequeService.getEncoursEncaissementForCheques(searchDto,banqueId, pageable));
    }
}