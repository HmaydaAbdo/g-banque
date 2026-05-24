package staport.finance.gBanque.rapprochement.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.rapprochement.dtos.RapprochementActifsDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementRequestDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementResponseDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementSearchDto;
import staport.finance.gBanque.rapprochement.services.RapprochementService;
import staport.finance.gBanque.reports.enums.ReportType;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;

@RestController
@RequestMapping("/rapprochement")
public class RapprochementController {

    private final RapprochementService rapprochementService;

    public RapprochementController(RapprochementService rapprochementService) {
        this.rapprochementService = rapprochementService;
    }



    // --- 0. GET actifs — snapshot EN_COURS + CLOTURE_PROVISOIRE pour une banque ---
    @GetMapping("/banque/{banqueId}/actifs")
    public ResponseEntity<RapprochementActifsDto> getActifs(@PathVariable Long banqueId) {
        return ResponseEntity.ok(rapprochementService.getActifs(banqueId));
    }

    // --- 1. POST clôture provisoire — EN_COURS → CLOTURE_PROVISOIRE ---
    @PostMapping("/cloturer")
    public ResponseEntity<RapprochementResponseDto> cloturerProvisoire(@RequestBody RapprochementRequestDto requestDto) {
        return ResponseEntity.ok(rapprochementService.cloturerProvisoire(requestDto.getBanqueId()));
    }

    // --- 2. POST clôture définitive — CLOTURE_PROVISOIRE → CLOTURE_DEFINITIVE ---
    @PostMapping("/{id}/cloturer-definitivement")
    public ResponseEntity<RapprochementResponseDto> cloturerDefinitivement(@PathVariable Long id) {
        return ResponseEntity.ok(rapprochementService.cloturerDefinitivement(id));
    }

    // --- 2. GET by ID (Récupération avec transactions paginées) ---
    @GetMapping("/{id}")
    public ResponseEntity<RapprochementResponseDto> getRapprochementById(
            @PathVariable Long id,
            @ModelAttribute TransactionSearchDto searchDto,
            Pageable pageable) {

        RapprochementResponseDto dto = rapprochementService.getRapprochementById(id,searchDto, pageable);

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
    public ResponseEntity<byte[]> downloadRapprochementReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "FINANCE") ReportType reportType) {

        // 1. Générer le contenu du rapport (byte[])
        byte[] pdfContent = rapprochementService.generateRapprochementPdf(id,reportType);

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