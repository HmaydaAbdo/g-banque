package staport.finance.gBanque.rapprochement.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.rapprochement.dtos.RapprochementRequestDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementResponseDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementSearchDto;
import staport.finance.gBanque.rapprochement.services.RapprochementService;

@RestController
@RequestMapping("/rapprochement")
public class RapprochementController {

    private final RapprochementService rapprochementService;

    public RapprochementController(RapprochementService rapprochementService) {
        this.rapprochementService = rapprochementService;
    }



    // --- 1. POST pour clôturer (201 Created) ---
    @PostMapping("/cloturer")
    public ResponseEntity<RapprochementResponseDto> cloturerRapprochement(@RequestBody RapprochementRequestDto requestDto) {

        RapprochementResponseDto responseDto = rapprochementService.addRapprochement(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    // --- 2. GET by ID (Récupération avec transactions paginées) ---
    @GetMapping("/{id}")
    public ResponseEntity<RapprochementResponseDto> getRapprochementById(
            @PathVariable Long id,
            Pageable pageable) {

        RapprochementResponseDto dto = rapprochementService.getRapprochementById(id, pageable);

        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<Page<RapprochementResponseDto>> searchRapprochements(
            RapprochementSearchDto searchDto,
            Pageable pageable) {

        Page<RapprochementResponseDto> rapprochementPage =
                rapprochementService.searchRapprochements(searchDto, pageable);

        return ResponseEntity.ok(rapprochementPage);
    }
}