package staport.finance.gBanque.leasing.batch.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.leasing.batch.scheduler.ContratLeasingMainLeveeJobScheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Déclenchement manuel du job batch Leasing Main Levée.
 */
@RestController
@RequestMapping("/test/batch/leasing")
public class LeasingBatchController {

    private static final Logger log = LoggerFactory.getLogger(LeasingBatchController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ContratLeasingMainLeveeJobScheduler leasingScheduler;

    public LeasingBatchController(ContratLeasingMainLeveeJobScheduler leasingScheduler) {
        this.leasingScheduler = leasingScheduler;
    }

    /**
     * Lance manuellement le job Leasing Main Levée.
     * POST /test/batch/leasing/run
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runLeasingJob() {
        log.info("📋 Requête reçue pour lancer le job Leasing Main Levée");

        try {
            JobExecution execution = leasingScheduler.runJobManually();

            if (execution != null) {
                log.info("✅ Job Leasing Main Levée lancé avec succès - ID: {}", execution.getId());
                return ResponseEntity.ok(buildSuccessResponse("Leasing Main Levée Job", execution));
            } else {
                log.error("❌ Échec du lancement du job Leasing Main Levée");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildErrorResponse("Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."));
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors du lancement du job Leasing Main Levée", e);
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
