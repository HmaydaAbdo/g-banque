package staport.finance.gBanque.cmt.batch.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.cmt.batch.scheduler.CreditMoyenTermeMainLeveeJobScheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Déclenchement manuel du job batch CMT Main Levée.
 */
@Tag(name = "Batch CMT", description = "Déclenchement manuel du job batch Main Levée")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/batch/cmt")
public class CmtBatchController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final CreditMoyenTermeMainLeveeJobScheduler cmtScheduler;

    public CmtBatchController(CreditMoyenTermeMainLeveeJobScheduler cmtScheduler) {
        this.cmtScheduler = cmtScheduler;
    }

    @Operation(summary = "Lancer manuellement le job CMT Main Levée",
            description = "Réservé aux administrateurs système — déclenche immédiatement le traitement des main levées")
    @ApiResponse(responseCode = "500", description = "Échec du démarrage du job")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runCmtJob() {
        try {
            JobExecution execution = cmtScheduler.runJobManually();

            if (execution != null) {
                return ResponseEntity.ok(buildSuccessResponse("CMT Main Levée Job", execution));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildErrorResponse("Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    @Operation(summary = "Consulter le statut du job CMT Main Levée")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCmtJobStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("jobName", "CMT Main Levée Job");
            status.put("isRunning", cmtScheduler.isJobRunning());
            status.put("isEnabled", cmtScheduler.isJobEnabled());
            status.put("cronExpression", cmtScheduler.getJobCron());
            status.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

            return ResponseEntity.ok(status);

        } catch (Exception e) {
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
