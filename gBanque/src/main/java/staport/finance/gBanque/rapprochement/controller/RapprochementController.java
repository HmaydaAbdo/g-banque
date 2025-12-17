package staport.finance.gBanque.rapprochement.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @GetMapping("/{id}/report/pdf")
    public ResponseEntity<byte[]> downloadRapprochementReport(@PathVariable Long id) {

        // 1. Générer le contenu du rapport (byte[])
        byte[] pdfContent = rapprochementService.generateRapprochementPdf(id);

        // 2. Définir le nom du fichier pour le téléchargement
        String filename = "Rapport_Rapprochement_" + id + ".pdf";

        // 3. Configurer les En-têtes HTTP
        HttpHeaders headers = new HttpHeaders();

        // Indique le type de contenu (PDF)
        headers.setContentType(MediaType.APPLICATION_PDF);

        // Force le téléchargement plutôt que l'affichage dans le navigateur
        headers.setContentDispositionFormData("attachment", filename);

        // Indique la taille du fichier
        headers.setContentLength(pdfContent.length);

        // 4. Retourner la réponse
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }
}