package staport.finance.gBanque.LigneFonctionnelle.batch.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.LigneFonctionnelle.batch.scheduler.FunctionalLinesJobScheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Déclenchement manuel des jobs batch Functional Lines (general + specific).
 */
@RestController
@RequestMapping("/test/batch/functional-lines")
public class FunctionalLinesBatchController {

    private static final Logger log = LoggerFactory.getLogger(FunctionalLinesBatchController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final FunctionalLinesJobScheduler functionalLinesScheduler;

    public FunctionalLinesBatchController(FunctionalLinesJobScheduler functionalLinesScheduler) {
        this.functionalLinesScheduler = functionalLinesScheduler;
    }

    /**
     * Lance manuellement le job General Functional Lines.
     * POST /test/batch/functional-lines/run-general
     */
    @PostMapping("/run-general")
    public ResponseEntity<Map<String, Object>> runGeneralFunctionalLinesJob() {
        log.info("📋 Requête reçue pour lancer le job General Functional Lines");

        try {
            JobExecution execution = functionalLinesScheduler.runGeneralJobManually();

            if (execution != null) {
                log.info("✅ Job General Functional Lines lancé avec succès - ID: {}", execution.getId());
                return ResponseEntity.ok(buildSuccessResponse("General Functional Lines Job", execution));
            } else {
                log.error("❌ Échec du lancement du job General Functional Lines");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildErrorResponse("Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."));
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
    @PostMapping("/run-specific")
    public ResponseEntity<Map<String, Object>> runSpecificFunctionalLinesJob() {
        log.info("📋 Requête reçue pour lancer le job Specific Functional Lines");

        try {
            JobExecution execution = functionalLinesScheduler.runSpecificJobManually();

            if (execution != null) {
                log.info("✅ Job Specific Functional Lines lancé avec succès - ID: {}", execution.getId());
                return ResponseEntity.ok(buildSuccessResponse("Specific Functional Lines Job", execution));
            } else {
                log.error("❌ Échec du lancement du job Specific Functional Lines");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildErrorResponse("Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."));
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
    @PostMapping("/run-all")
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
