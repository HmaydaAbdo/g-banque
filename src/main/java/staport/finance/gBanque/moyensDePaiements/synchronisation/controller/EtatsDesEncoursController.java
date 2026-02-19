package staport.finance.gBanque.moyensDePaiements.synchronisation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import staport.finance.gBanque.moyensDePaiements.synchronisation.services.EtasDesEncoursService;


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


}