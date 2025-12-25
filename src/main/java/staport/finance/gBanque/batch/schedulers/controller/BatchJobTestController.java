package staport.finance.gBanque.batch.schedulers.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.batch.schedulers.cmt.CreditMoyenTermeMainLeveeJobScheduler;
import staport.finance.gBanque.batch.schedulers.functionalLines.FunctionalLinesJobScheduler;
import staport.finance.gBanque.batch.schedulers.leasing.ContratLeasingMainLeveeJobScheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur REST pour le déclenchement manuel des jobs batch.
 * Permet de tester et exécuter les jobs batch à la demande.
 *
 * @author Gbanque Development Team
 * @version 1.1
 */
@RestController
@RequestMapping("/test/batch")
public class BatchJobTestController {

    private static final Logger log = LoggerFactory.getLogger(BatchJobTestController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final FunctionalLinesJobScheduler functionalLinesScheduler;
    private final ContratLeasingMainLeveeJobScheduler leasingScheduler;
    private final CreditMoyenTermeMainLeveeJobScheduler cmtScheduler;

    @Autowired
    public BatchJobTestController(
            FunctionalLinesJobScheduler functionalLinesScheduler,
            ContratLeasingMainLeveeJobScheduler leasingScheduler,
            CreditMoyenTermeMainLeveeJobScheduler cmtScheduler) {
        this.functionalLinesScheduler = functionalLinesScheduler;
        this.leasingScheduler = leasingScheduler;
        this.cmtScheduler = cmtScheduler;
        log.info("BatchJobTestController initialisé avec succès");
    }

    // ================================================================================
    // FUNCTIONAL LINES JOBS
    // ================================================================================

    /**
     * Lance manuellement le job General Functional Lines.
     * POST /test/batch/functional-lines/run-general
     */
    @PostMapping("/functional-lines/run-general")
    public ResponseEntity<Map<String, Object>> runGeneralFunctionalLinesJob() {
        log.info("📋 Requête reçue pour lancer le job General Functional Lines");

        try {
            JobExecution execution = functionalLinesScheduler.runGeneralJobManually();

            if (execution != null) {
                Map<String, Object> response = buildSuccessResponse(
                        "General Functional Lines Job",
                        execution
                );

                log.info("✅ Job General Functional Lines lancé avec succès - ID: {}", execution.getId());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = buildErrorResponse(
                        "Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."
                );

                log.error("❌ Échec du lancement du job General Functional Lines");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse);
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors du lancement du job General Functional Lines", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Lance manuellement le job Specific Functional Lines.
     * POST /test/batch/functional-lines/run-specific
     */
    @PostMapping("/functional-lines/run-specific")
    public ResponseEntity<Map<String, Object>> runSpecificFunctionalLinesJob() {
        log.info("📋 Requête reçue pour lancer le job Specific Functional Lines");

        try {
            JobExecution execution = functionalLinesScheduler.runSpecificJobManually();

            if (execution != null) {
                Map<String, Object> response = buildSuccessResponse(
                        "Specific Functional Lines Job",
                        execution
                );

                log.info("✅ Job Specific Functional Lines lancé avec succès - ID: {}", execution.getId());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = buildErrorResponse(
                        "Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."
                );

                log.error("❌ Échec du lancement du job Specific Functional Lines");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse);
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors du lancement du job Specific Functional Lines", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Lance manuellement tous les jobs Functional Lines de manière séquentielle.
     * POST /test/batch/functional-lines/run-all
     */
    @PostMapping("/functional-lines/run-all")
    public ResponseEntity<Map<String, Object>> runAllFunctionalLinesJobs() {
        log.info("📋 Requête reçue pour lancer tous les jobs Functional Lines");

        try {
            functionalLinesScheduler.runAllJobsManually();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tous les jobs Functional Lines ont été lancés séquentiellement");
            response.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
            response.put("executionMode", "SEQUENTIAL");
            response.put("generalJobEnabled", functionalLinesScheduler.isGeneralJobEnabled());
            response.put("specificJobEnabled", functionalLinesScheduler.isSpecificJobEnabled());

            log.info("✅ Tous les jobs Functional Lines lancés avec succès");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors du lancement des jobs Functional Lines", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    // ================================================================================
    // LEASING MAIN LEVÉE JOB
    // ================================================================================

    /**
     * Lance manuellement le job Leasing Main Levée.
     * POST /test/batch/leasing/run
     */
    @PostMapping("/leasing/run")
    public ResponseEntity<Map<String, Object>> runLeasingJob() {
        log.info("📋 Requête reçue pour lancer le job Leasing Main Levée");

        try {
            JobExecution execution = leasingScheduler.runJobManually();

            if (execution != null) {
                Map<String, Object> response = buildSuccessResponse(
                        "Leasing Main Levée Job",
                        execution
                );

                log.info("✅ Job Leasing Main Levée lancé avec succès - ID: {}", execution.getId());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = buildErrorResponse(
                        "Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."
                );

                log.error("❌ Échec du lancement du job Leasing Main Levée");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse);
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors du lancement du job Leasing Main Levée", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    // ================================================================================
    // CMT MAIN LEVÉE JOB - NOUVEAU
    // ================================================================================

    /**
     * Lance manuellement le job CMT Main Levée.
     * POST /test/batch/cmt/run
     */
    @PostMapping("/cmt/run")
    public ResponseEntity<Map<String, Object>> runCmtJob() {
        log.info("📋 Requête reçue pour lancer le job CMT Main Levée");

        try {
            JobExecution execution = cmtScheduler.runJobManually();

            if (execution != null) {
                Map<String, Object> response = buildSuccessResponse(
                        "CMT Main Levée Job",
                        execution
                );

                log.info("✅ Job CMT Main Levée lancé avec succès - ID: {}", execution.getId());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = buildErrorResponse(
                        "Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."
                );

                log.error("❌ Échec du lancement du job CMT Main Levée");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse);
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors du lancement du job CMT Main Levée", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Récupère le statut actuel du job CMT.
     * GET /test/batch/cmt/status
     */
    @GetMapping("/cmt/status")
    public ResponseEntity<Map<String, Object>> getCmtJobStatus() {
        log.debug("Vérification du statut du job CMT Main Levée");

        try {
            Map<String, Object> status = new HashMap<>();
            status.put("jobName", "CMT Main Levée Job");
            status.put("isRunning", cmtScheduler.isJobRunning());
            status.put("isEnabled", cmtScheduler.isJobEnabled());
            status.put("cronExpression", cmtScheduler.getJobCron());
            status.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du statut du job CMT", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    // ================================================================================
    // STATUS ENDPOINTS
    // ================================================================================


    // ================================================================================
    // HELPER METHODS
    // ================================================================================

    /**
     * Construit une réponse de succès avec les détails de l'exécution du job.
     */
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

    /**
     * Construit une réponse d'erreur.
     */
    private Map<String, Object> buildErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorMessage);
        response.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        return response;
    }

    /**
     * Formate une date/heure.
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "N/A";
    }
}