package staport.finance.gBanque.moyensDePaiements.synchronisation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.DeactivationResult;
import staport.finance.gBanque.moyensDePaiements.synchronisation.services.EtasDesEncoursService;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller to expose endpoints for managing and triggering
 * the batch jobs related to "Etats des Encours" (e.g., payment import job).
 */
@RestController
@RequestMapping("/funPayment") // Base path for this controller's endpoints
public class EtatsDesEncoursController {

    private static final Logger log = LoggerFactory.getLogger(EtatsDesEncoursController.class);

    private final EtasDesEncoursService etasDesEncoursService;

    public EtatsDesEncoursController(EtasDesEncoursService etasDesEncoursService) {
        this.etasDesEncoursService = etasDesEncoursService;
    }

    /**
     * Endpoint to manually trigger the payment import batch job.
     * Uses POST request as it initiates an action (launching a job).
     *
     * @return ResponseEntity with job execution details on success, or an error message on failure.
     */
    @PostMapping("/synchronisation")
    public ResponseEntity<String> launchPaymentImportJob() {
        log.info("Received request to launch payment import batch job.");
        try {
            JobExecution jobExecution = etasDesEncoursService.launchPaymentImportJob();
            String message = String.format("Payment import job launched successfully. JobExecution ID: %d, Status: %s",
                    jobExecution.getId(), jobExecution.getStatus());
            log.info(message);
            return ResponseEntity.ok(message); // HTTP 200 OK
        } catch (JobExecutionException e) {
            String errorMessage = "Failed to launch payment import job: " + e.getMessage();
            log.error(errorMessage, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage); // HTTP 500 Internal Server Error
        } catch (Exception e) {
            String errorMessage = "An unexpected error occurred while launching the payment import job: " + e.getMessage();
            log.error(errorMessage, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage); // HTTP 500 Internal Server Error
        }
    }

    @PatchMapping("/deactivate-old-payments")
    public ResponseEntity<Map<String, Object>> deactivateOldPayments() {
        log.info("Réception d'une demande de désactivation des paiements antérieurs à 2026.");
        try {
            DeactivationResult result = etasDesEncoursService.deactivateOldPayments();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Désactivation des paiements terminée avec succès");
            response.put("chequesDesactives", result.getChequesDeactivated());
            response.put("virementsDesactives", result.getVirementsDeactivated());
            response.put("misesADispositionDesactivees", result.getMiseADispositionsDeactivated());
            response.put("lcnDesactives", result.getLcnsDeactivated());
            response.put("totalDesactive", result.getTotalDeactivated());

            log.info("Désactivation terminée avec succès: {}", result);
            return ResponseEntity.ok(response); // HTTP 200 OK

        } catch (Exception e) {
            String errorMessage = "Une erreur s'est produite lors de la désactivation des paiements: " + e.getMessage();
            log.error(errorMessage, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", errorMessage);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse); // HTTP 500
        }
    }
}