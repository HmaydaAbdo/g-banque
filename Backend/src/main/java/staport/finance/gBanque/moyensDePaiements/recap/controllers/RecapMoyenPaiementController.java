package staport.finance.gBanque.moyensDePaiements.recap.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.moyensDePaiements.recap.dtos.request.RecapMoyenPaiementSearchDto;
import staport.finance.gBanque.moyensDePaiements.recap.dtos.response.AgregatedRecapMoyenPaiementDto;
import staport.finance.gBanque.moyensDePaiements.recap.services.interfaces.IRecapMoyenPaiementService;

import java.time.LocalDate;

/**
 * REST Controller pour le Recap Encaissement / Recap Décaissement des moyens de paiement.
 *
 * <p>Vue consolidée, tous statuts confondus, des 5 moyens de paiement de la section
 * "Les moyens de paiement" (Chèques, LCN, Virements nationaux, Virements internationaux, Mises à
 * disposition), toutes banques confondues avec filtre banque. Tri par défaut : date de valeur,
 * décroissant.</p>
 */
@Tag(name = "Recap Moyens de Paiement", description = "Vue consolidée encaissement/décaissement de tous les moyens de paiement")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/recap-moyens-paiement")
@PreAuthorize(RecapMoyenPaiementController.FINANCE_OR_ADMIN)
public class RecapMoyenPaiementController {

    static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";

    private final IRecapMoyenPaiementService recapMoyenPaiementService;

    public RecapMoyenPaiementController(IRecapMoyenPaiementService recapMoyenPaiementService) {
        this.recapMoyenPaiementService = recapMoyenPaiementService;
    }

    @Operation(summary = "Recap des encaissements — tous moyens de paiement, tous statuts, toutes banques")
    @GetMapping("/encaissement")
    public ResponseEntity<AgregatedRecapMoyenPaiementDto> getEncaissements(
            @PageableDefault(sort = "dateValeur", direction = Sort.Direction.DESC) Pageable pageable,
            @ModelAttribute RecapMoyenPaiementSearchDto searchDto) {

        return new ResponseEntity<>(recapMoyenPaiementService.getEncaissements(pageable, searchDto), HttpStatus.OK);
    }

    @Operation(summary = "Recap des décaissements — tous moyens de paiement, tous statuts, toutes banques")
    @GetMapping("/decaissement")
    public ResponseEntity<AgregatedRecapMoyenPaiementDto> getDecaissements(
            @PageableDefault(sort = "dateValeur", direction = Sort.Direction.DESC) Pageable pageable,
            @ModelAttribute RecapMoyenPaiementSearchDto searchDto) {

        return new ResponseEntity<>(recapMoyenPaiementService.getDecaissements(pageable, searchDto), HttpStatus.OK);
    }

    @Operation(summary = "Exporter en PDF le recap des encaissements",
            description = "Mêmes critères que GET /recap-moyens-paiement/encaissement — sans pagination")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @GetMapping("/encaissement/export/pdf")
    public ResponseEntity<byte[]> exportEncaissementsPdf(@ModelAttribute RecapMoyenPaiementSearchDto searchDto) {
        byte[] pdf = recapMoyenPaiementService.exportEncaissementsPdf(searchDto);
        return pdfResponse(pdf, "recap_encaissement_moyens_paiement");
    }

    @Operation(summary = "Exporter en PDF le recap des décaissements",
            description = "Mêmes critères que GET /recap-moyens-paiement/decaissement — sans pagination")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @GetMapping("/decaissement/export/pdf")
    public ResponseEntity<byte[]> exportDecaissementsPdf(@ModelAttribute RecapMoyenPaiementSearchDto searchDto) {
        byte[] pdf = recapMoyenPaiementService.exportDecaissementsPdf(searchDto);
        return pdfResponse(pdf, "recap_decaissement_moyens_paiement");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filenamePrefix) {
        String filename = filenamePrefix + "_" + LocalDate.now() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
