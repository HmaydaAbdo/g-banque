package staport.finance.gBanque.cmt.echeances.controllers;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.cmt.echeances.dtos.*;
import staport.finance.gBanque.cmt.echeances.services.EcheanceCMTService;


/**
 * Contrôleur REST pour la gestion et la consultation des échéances de crédit à moyen terme (CMT).
 * Il expose les points d'accès pour obtenir le tableau d'amortissement et marquer une échéance comme payée.
 */
@RestController
@RequestMapping("/echeances-cmt") // Préfixe d'API recommandé
public class EcheanceCMTController {

    private final EcheanceCMTService echeanceCmtService;

    public EcheanceCMTController(EcheanceCMTService echeanceCmtService) {
        this.echeanceCmtService = echeanceCmtService;
    }

    /**
     * Récupère le tableau d'amortissement complet pour un crédit à moyen terme spécifique.
     * Permet de filtrer les échéances affichées dans le tableau et de les paginer.
     * Mappe à GET /api/echeances-cmt/tableau-amortissement/{creditId}
     *
     * @param creditId L'ID du crédit à moyen terme.
     * @param searchDto Les critères de recherche optionnels pour filtrer les échéances.
     * @param pageable Informations de pagination et de tri pour les échéances du tableau.
     * @return ResponseEntity avec le TableauAmortissementCMTResponseDto et le statut HTTP 200 (OK).
     */
    @GetMapping("/tableau-amortissement/{creditId}")
    public ResponseEntity<TableauAmortissementCMTResponseDto> getTableauAmortissement(
            @PathVariable Long creditId,
            EcheanceCMTSearchDto searchDto,
            Pageable pageable) {
        TableauAmortissementCMTResponseDto tableau = echeanceCmtService.getTableauAmortissementForCMT(creditId, searchDto, pageable);
        return ResponseEntity.ok(tableau);
    }

    /**
     * Permet de marquer une échéance comme payée.
     * Mappe à PUT /api/echeances-cmt/{id}/mark-as-paid
     *
     * @param id L'ID de l'échéance à marquer comme payée.
     * @return ResponseEntity avec le DTO de l'échéance mise à jour et le statut HTTP 200 (OK).
     */
    @PutMapping("/{id}/mark-as-paid")
    public ResponseEntity<EcheanceCMTResponseDto> markEcheanceAsPaid(@PathVariable Long id) {
        EcheanceCMTResponseDto updatedEcheance = echeanceCmtService.markEcheanceAsPaid(id);
        return ResponseEntity.ok(updatedEcheance);
    }


    /**
     * Récupère les encours d'échéances de leasing pour une banque, avec une période et un organisme de leasing optionnel.
     * Mappe à GET /api/echeances-leasing/encours
     *
     * @param requestDTO Le DTO contenant l'ID de la banque, la période et l'ID de l'organisme de leasing optionnel.
     * @param pageable Les informations de pagination et de tri pour les échéances.
     * @return ResponseEntity avec l'EncoursLeasingResponseDTO et HTTP Status 200 (OK).
     */
    @GetMapping("/encours")
    public ResponseEntity<EncoursCMTResponseDTO> getEncoursCMT(
            @Valid @ModelAttribute EncoursCMTRequestDTO requestDTO,
            Pageable pageable) {
        EncoursCMTResponseDTO encours = echeanceCmtService.getEncoursCMTForBanque(requestDTO, pageable);
        return ResponseEntity.ok(encours);
    }
}