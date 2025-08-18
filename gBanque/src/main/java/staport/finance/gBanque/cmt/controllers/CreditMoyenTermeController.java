package staport.finance.gBanque.cmt.controllers;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.cmt.dtos.DeleteResponceDto;
import staport.finance.gBanque.cmt.echeances.dtos.EcheanceCMTSearchDto;
import staport.finance.gBanque.cmt.echeances.dtos.TableauAmortissementCMTResponseDto;
import staport.finance.gBanque.cmt.echeances.services.EcheanceCMTService;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeRequestDto;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeResponseDto;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeSearchDto;
import staport.finance.gBanque.cmt.services.CreditMoyenTermeService;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;


/**
 * Contrôleur REST pour la gestion des opérations sur les crédits à moyen terme (CMT).
 * Gère les requêtes HTTP entrantes et délègue les tâches aux services correspondants.
 */
@RestController
@RequestMapping("/credits-cmt") // Définit le chemin de base de l'API pour ce contrôleur
public class CreditMoyenTermeController {

    private final CreditMoyenTermeService creditMoyenTermeService;
    private final EcheanceCMTService echeanceCmtService;

    /**
     * Constructeur pour le contrôleur, injectant les services nécessaires.
     *
     * @param creditMoyenTermeService Le service pour les opérations sur les crédits.
     * @param echeanceCmtService Le service pour les opérations sur les échéances.
     */
    public CreditMoyenTermeController(CreditMoyenTermeService creditMoyenTermeService, EcheanceCMTService echeanceCmtService) {
        this.creditMoyenTermeService = creditMoyenTermeService;
        this.echeanceCmtService = echeanceCmtService;
    }

    /**
     * Point d'accès pour créer un nouveau crédit à moyen terme.
     * Mappe à POST /api/credits-cmt
     *
     * @param requestDto Le DTO contenant les données du nouveau crédit.
     * @return ResponseEntity avec le DTO du crédit créé et le statut HTTP 201 (Created).
     */
    @PostMapping
    public ResponseEntity<CreditMoyenTermeResponseDto> createCreditMoyenTerme(@Valid @RequestBody CreditMoyenTermeRequestDto requestDto) {
        CreditMoyenTermeResponseDto createdCredit = creditMoyenTermeService.create(requestDto);
        return new ResponseEntity<>(createdCredit, HttpStatus.CREATED);
    }

    /**
     * Récupère une liste paginée et triée de crédits, avec des critères de recherche optionnels.
     * Mappe à GET /api/credits-cmt
     *
     * @param searchDto Un DTO contenant les filtres de recherche.
     * @param pageable Informations de pagination et de tri.
     * @return Une page de DTOs de réponse des crédits.
     */
    @GetMapping
    public ResponseEntity<Page<CreditMoyenTermeResponseDto>> searchActifsCreditsMoyenTerme(
            CreditMoyenTermeSearchDto searchDto,
            Pageable pageable) {
        Page<CreditMoyenTermeResponseDto> resultPage = creditMoyenTermeService.searchOnActifOnly(searchDto, pageable);
        return ResponseEntity.ok(resultPage);
    }

    /**
     * Point d'accès pour récupérer un crédit par son ID.
     * Mappe à GET /api/credits-cmt/{id}
     *
     * @param id L'ID du crédit à récupérer.
     * @return ResponseEntity avec le DTO du crédit trouvé.
     * @throws ResourceNotFoundException si le crédit n'est pas trouvé.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CreditMoyenTermeResponseDto> getCreditMoyenTermeById(@PathVariable Long id) {
        CreditMoyenTermeResponseDto credit = creditMoyenTermeService.findById(id);
        return ResponseEntity.ok(credit);
    }

    /**
     * Point d'accès pour mettre à jour un crédit existant par son ID.
     * Mappe à PUT /api/credits-cmt/{id}
     *
     * @param id L'ID du crédit à mettre à jour.
     * @param requestDto Le DTO contenant les données de mise à jour.
     * @return ResponseEntity avec le DTO du crédit mis à jour.
     * @throws ResourceNotFoundException si le crédit ou la banque n'est pas trouvé.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CreditMoyenTermeResponseDto> updateCreditMoyenTerme(@PathVariable Long id, @Valid @RequestBody CreditMoyenTermeRequestDto requestDto) {
        CreditMoyenTermeResponseDto updatedCredit = creditMoyenTermeService.update(id, requestDto);
        return ResponseEntity.ok(updatedCredit);
    }

    /**
     * Récupère le tableau d'amortissement pour un crédit spécifique,
     * avec pagination, tri et critères de recherche sur les échéances.
     * Mappe à GET /api/credits-cmt/{creditId}/tableau-amortissement
     *
     * @param creditId L'ID du crédit.
     * @param searchDto Un DTO contenant les filtres pour les échéances.
     * @param pageable Informations de pagination et de tri.
     * @return ResponseEntity avec le DTO du tableau d'amortissement.
     * @throws ResourceNotFoundException si le crédit n'est pas trouvé.
     */
    @GetMapping("/{creditId}/tableau-amortissement")
    public ResponseEntity<TableauAmortissementCMTResponseDto> getTableauAmortissementForCredit(
            @PathVariable Long creditId,
            EcheanceCMTSearchDto searchDto,
            Pageable pageable) {
        TableauAmortissementCMTResponseDto tableau = echeanceCmtService.getTableauAmortissementForCMT(creditId, searchDto, pageable);
        return ResponseEntity.ok(tableau);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<DeleteResponceDto> deleteCreditMoyenTerme(@PathVariable Long id) {
     return ResponseEntity.ok(this.creditMoyenTermeService.delete(id));
    }
}