package staport.finance.gBanque.leasing.echeancesLeasing.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.*;
import staport.finance.gBanque.leasing.echeancesLeasing.services.EcheanceLeasingService;


/**
 * REST Controller pour la gestion et la consultation des opérations d'EcheanceLeasing.
 * Inclut la récupération du tableau d'amortissement et la recherche avancée.
 */
@RestController
@RequestMapping("/echeances-leasing") // Préfixe d'API recommandé
public class EcheanceLeasingController {

    private final EcheanceLeasingService echeanceLeasingService;

    public EcheanceLeasingController(EcheanceLeasingService echeanceLeasingService) {
        this.echeanceLeasingService = echeanceLeasingService;
    }

    /**
     * Récupère le tableau d'amortissement complet pour un contrat de leasing spécifique.
     * Permet également de filtrer les échéances affichées dans le tableau par numéro, mois/année, montant, statut.
     * Mappe à GET /api/echeances-leasing/contrat/{contratId}/tableau-amortissement
     *
     * @param contratId L'ID du contrat de leasing.
     * @param searchDTO Les critères de recherche optionnels pour filtrer les échéances au sein du tableau.
     * Les champs `month`, `year`, `numeroEcheance`, `montantTTCMin`, `montantTTCMax`, `statutPaiement` sont utilisables.
     * @param pageable Informations de pagination pour les échéances du tableau.
     * @return ResponseEntity avec le TableauAmortissementDTO et HTTP Status 200 (OK).
     */
    @GetMapping("/tableau-amortissement/{contratId}")
    public ResponseEntity<TableauAmortissementDTO> getTableauAmortissement(
            @PathVariable Long contratId,
            EcheanceLeasingSearchDTO searchDTO,
            Pageable pageable) {
        TableauAmortissementDTO tableau = echeanceLeasingService.getTableauAmortissementForContrat(contratId, searchDTO, pageable);
        return ResponseEntity.ok(tableau);
    }

    /**
     * Permet de marquer une échéance comme payée.
     * Mappe à PUT /api/echeances-leasing/{id}/mark-as-paid
     *
     * @param id L'ID de l'échéance à marquer comme payée.
     * @return ResponseEntity avec l'échéance mise à jour et HTTP Status 200 (OK).
     */
    @PutMapping("/{id}/mark-as-paid")
    public ResponseEntity<EcheanceLeasingResponseDTO> markEcheanceAsPaid(@PathVariable Long id) {
        EcheanceLeasingResponseDTO updatedEcheance = echeanceLeasingService.markEcheanceAsPaid(id);
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
    public ResponseEntity<EncoursLeasingResponseDTO> getEncoursLeasing(
            @Valid @ModelAttribute EncoursLeasingRequestDTO requestDTO,
            Pageable pageable) {
        EncoursLeasingResponseDTO encours = echeanceLeasingService.getEncoursLeasingForBanque(requestDTO, pageable);
        return ResponseEntity.ok(encours);
    }

}