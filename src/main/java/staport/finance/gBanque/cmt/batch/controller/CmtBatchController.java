package staport.finance.gBanque.cmt.batch.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.cmt.batch.scheduler.CreditMoyenTermeMainLeveeJobScheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Déclenchement manuel du job batch CMT Main Levée.
 */
@RestController
@RequestMapping("/test/batch/cmt")
public class CmtBatchController {

    private static final Logger log = LoggerFactory.getLogger(CmtBatchController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final CreditMoyenTermeMainLeveeJobScheduler cmtScheduler;

    public CmtBatchController(CreditMoyenTermeMainLeveeJobScheduler cmtScheduler) {
        this.cmtScheduler = cmtScheduler;
    }

    /**
     * Lance manuellement le job CMT Main Levée.
     * POST /test/batch/cmt/run
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runCmtJob() {
        log.info("📋 Requête reçue pour lancer le job CMT Main Levée");

        try {
            JobExecution execution = cmtScheduler.runJobManually();

            if (execution != null) {
                log.info("✅ Job CMT Main Levée lancé avec succès - ID: {}", execution.getId());
                return ResponseEntity.ok(buildSuccessResponse("CMT Main Levée Job", execution));
            } else {
                log.error("❌ Échec du lancement du job CMT Main Levée");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildErrorResponse("Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."));
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
    @GetMapping("/status")
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
