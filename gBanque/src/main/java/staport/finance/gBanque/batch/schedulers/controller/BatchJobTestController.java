package staport.finance.gBanque.batch.schedulers.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.batch.schedulers.functionalLines.FunctionalLinesJobScheduler;
import staport.finance.gBanque.batch.schedulers.leasing.ContratLeasingMainLeveeJobScheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for manual trigger of  batch jobs .
 *
 * @author Gbanque Development Team
 * @version 1.0
 */
@RestController
@RequestMapping("/test/batch")
public class BatchJobTestController {

    private static final Logger log = LoggerFactory.getLogger(BatchJobTestController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final FunctionalLinesJobScheduler functionalLinesScheduler;
    private final ContratLeasingMainLeveeJobScheduler leasingScheduler;

    @Autowired
    public BatchJobTestController(
            FunctionalLinesJobScheduler functionalLinesScheduler,
            ContratLeasingMainLeveeJobScheduler leasingScheduler) {
        this.functionalLinesScheduler = functionalLinesScheduler;
        this.leasingScheduler = leasingScheduler;
    }

    // ================================================================================
    // LEASING MAIN LEVÉE JOB
    // ================================================================================

    @PostMapping("/leasing/run")
    public ResponseEntity<Map<String, Object>> runLeasingJob() {

        try {
            JobExecution execution = leasingScheduler.runJobManually();

            if (execution != null) {
                Map<String, Object> response = buildSuccessResponse(
                        "Leasing Main Levée Job",
                        execution
                );

                log.info("✓ Job lancé avec succès - ID: {}", execution.getId());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = buildErrorResponse(
                        "Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."
                );

                log.error("✗ Échec du lancement du job");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse);
            }

        } catch (Exception e) {
            log.error("✗ Erreur lors du lancement du job Leasing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }


    @PostMapping("/functional-lines/run-general")
    public ResponseEntity<Map<String, Object>> runGeneralFunctionalLinesJob() {
        try {
            JobExecution execution = functionalLinesScheduler.runGeneralJobManually();

            if (execution != null) {
                Map<String, Object> response = buildSuccessResponse(
                        "General Functional Lines Job",
                        execution
                );

                log.info("✓ Job lancé avec succès - ID: {}", execution.getId());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = buildErrorResponse(
                        "Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."
                );

                log.error("✗ Échec du lancement du job");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse);
            }

        } catch (Exception e) {
            log.error("✗ Erreur lors du lancement du job General Functional Lines", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }


    @PostMapping("/functional-lines/run-specific")
    public ResponseEntity<Map<String, Object>> runSpecificFunctionalLinesJob() {

        try {
            JobExecution execution = functionalLinesScheduler.runSpecificJobManually();

            if (execution != null) {
                Map<String, Object> response = buildSuccessResponse(
                        "Specific Functional Lines Job",
                        execution
                );

                log.info("✓ Job lancé avec succès - ID: {}", execution.getId());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = buildErrorResponse(
                        "Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."
                );

                log.error("✗ Échec du lancement du job");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse);
            }

        } catch (Exception e) {
            log.error("✗ Erreur lors du lancement du job Specific Functional Lines", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }


    @PostMapping("/functional-lines/run-all")
    public ResponseEntity<Map<String, Object>> runAllFunctionalLinesJobs() {

        try {
            functionalLinesScheduler.runAllJobsManually();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tous les jobs Functional Lines ont été lancés séquentiellement");
            response.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
            response.put("executionMode", "SEQUENTIAL");
            response.put("generalJobEnabled", functionalLinesScheduler.isGeneralJobEnabled());
            response.put("specificJobEnabled", functionalLinesScheduler.isSpecificJobEnabled());

            log.info("✓ Tous les jobs lancés avec succès");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("✗ Erreur lors du lancement des jobs Functional Lines", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }



    private Map<String, Object> buildSuccessResponse(String jobName, JobExecution execution) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("jobName", jobName);
        response.put("jobExecutionId", execution.getId());
        response.put("jobInstanceId", execution.getJobInstance().getInstanceId());
        response.put("status", execution.getStatus().toString());
        response.put("startTime", formatDateTime(execution.getStartTime()));
        response.put("endTime", formatDateTime(execution.getEndTime()));
        response.put("exitCode", execution.getExitStatus().getExitCode());
        response.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        // Ajouter les statistiques si disponibles
        if (execution.getStatus() == BatchStatus.COMPLETED) {
            response.put("completed", true);
            response.put("message", "Le job s'est terminé avec succès");
        } else if (execution.getStatus() == BatchStatus.STARTED) {
            response.put("completed", false);
            response.put("message", "Le job est en cours d'exécution");
        }

        return response;
    }

    private Map<String, Object> buildErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorMessage);
        response.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        return response;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "N/A";
    }
}