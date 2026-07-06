package staport.finance.gBanque.LigneFonctionnelle.batch.controller;

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
import staport.finance.gBanque.LigneFonctionnelle.batch.scheduler.FunctionalLinesJobScheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Déclenchement manuel des jobs batch Functional Lines (general + specific).
 * Réservé SYSTEM_ADMIN, mirroring {@code CmtBatchController}/{@code LeasingBatchController}.
 */
@Tag(name = "Batch Lignes Fonctionnelles", description = "Déclenchement manuel des jobs batch d'expiration des lignes fonctionnelles")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/batch/functional-lines")
public class FunctionalLinesBatchController {

    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final FunctionalLinesJobScheduler functionalLinesScheduler;

    public FunctionalLinesBatchController(FunctionalLinesJobScheduler functionalLinesScheduler) {
        this.functionalLinesScheduler = functionalLinesScheduler;
    }

    @Operation(summary = "Lancer manuellement le job General Functional Lines")
    @ApiResponse(responseCode = "500", description = "Échec du démarrage du job")
    @PreAuthorize(SYSTEM_ADMIN)
    @PostMapping("/run-general")
    public ResponseEntity<Map<String, Object>> runGeneralFunctionalLinesJob() {
        try {
            JobExecution execution = functionalLinesScheduler.runGeneralJobManually();

            if (execution != null) {
                return ResponseEntity.ok(buildSuccessResponse("General Functional Lines Job", execution));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildErrorResponse("Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    @Operation(summary = "Lancer manuellement le job Specific Functional Lines")
    @ApiResponse(responseCode = "500", description = "Échec du démarrage du job")
    @PreAuthorize(SYSTEM_ADMIN)
    @PostMapping("/run-specific")
    public ResponseEntity<Map<String, Object>> runSpecificFunctionalLinesJob() {
        try {
            JobExecution execution = functionalLinesScheduler.runSpecificJobManually();

            if (execution != null) {
                return ResponseEntity.ok(buildSuccessResponse("Specific Functional Lines Job", execution));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildErrorResponse("Le job n'a pas pu démarrer. Vérifiez les logs pour plus de détails."));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    @Operation(summary = "Lancer manuellement tous les jobs Functional Lines", description = "Exécution séquentielle : General puis Specific.")
    @ApiResponse(responseCode = "500", description = "Échec du démarrage d'un job")
    @PreAuthorize(SYSTEM_ADMIN)
    @PostMapping("/run-all")
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

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    @Operation(summary = "Consulter le statut des jobs Functional Lines")
    @PreAuthorize(SYSTEM_ADMIN)
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFunctionalLinesJobsStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("schedulerEnabled", functionalLinesScheduler.isSchedulerEnabled());
            status.put("cronExpression", functionalLinesScheduler.getJobsCron());
            status.put("sequentialMode", functionalLinesScheduler.isSequentialMode());
            status.put("abortOnGeneralFailure", functionalLinesScheduler.isAbortOnGeneralFailure());
            status.put("generalJobEnabled", functionalLinesScheduler.isGeneralJobEnabled());
            status.put("generalJobRunning", functionalLinesScheduler.isGeneralJobRunning());
            status.put("specificJobEnabled", functionalLinesScheduler.isSpecificJobEnabled());
            status.put("specificJobRunning", functionalLinesScheduler.isSpecificJobRunning());
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
