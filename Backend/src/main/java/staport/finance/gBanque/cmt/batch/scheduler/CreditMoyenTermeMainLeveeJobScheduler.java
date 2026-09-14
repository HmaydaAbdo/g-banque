package staport.finance.gBanque.cmt.batch.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.audit.BatchAuditContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler pour le Job de vérification des mains levées des Crédits à Moyen Terme (CMT).
 * Ce scheduler gère l'exécution du job de vérification des mains levées
 * qui identifie les crédits CMT nécessitant une "main levée" (levée de garantie).
 * Fonctionnalités :
 * - Prévention des exécutions concurrentes
 * - Configuration flexible via expressions CRON
 * - Possibilité de déclenchement manuel pour tests/maintenance
 * - Gestion complète des erreurs et logging détaillé
 * - Planifié pour s'exécuter APRÈS le job Leasing (éviter les conflits)
 * Planification par défaut : Tous les dimanches à 14h30 (30 minutes après le job Leasing)
 *
 * @author Gbanque Development Team
 * @version 1.0
 * @since 2025-12-20
 */
@Service
public class CreditMoyenTermeMainLeveeJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(CreditMoyenTermeMainLeveeJobScheduler.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JobLauncher jobLauncher;
    private final Job creditMoyenTermeMainLeveeJob;

    // Flag pour prévenir les exécutions concurrentes
    private final AtomicBoolean isJobRunning = new AtomicBoolean(false);

    // Propriétés de configuration
    @Value("${cmt.main-levee.job.cron}")
    private String jobCron;

    @Value("${cmt.main-levee.job.enabled}")
    private boolean jobEnabled;

    /**
     * Constructeur avec injection de dépendances.
     *
     * @param jobLauncher                    JobLauncher de Spring Batch
     * @param creditMoyenTermeMainLeveeJob   Job de vérification des mains levées CMT
     */
    public CreditMoyenTermeMainLeveeJobScheduler(
            JobLauncher jobLauncher,
            @Qualifier("creditMoyenTermeMainLeveeJob") Job creditMoyenTermeMainLeveeJob) {
        this.jobLauncher = jobLauncher;
        this.creditMoyenTermeMainLeveeJob = creditMoyenTermeMainLeveeJob;
        log.info("Initialisation de CreditMoyenTermeMainLeveeJobScheduler");
        log.info("Configuration - CRON: {}, Activé: {}", jobCron, jobEnabled);
    }

    /**
     * Exécution planifiée du job de vérification des mains levées CMT.
     *
     * Par défaut : Tous les dimanches à 14h30
     * CRON : "0 30 14 ? * SUN"
     * Propriété de configuration : cmt.main-levee.job.cron
     *
     * Justification de la planification :
     * - S'exécute 30 minutes après le job Leasing (14h00)
     * - Évite les conflits de verrous de base de données
     * - Permet un temps de traitement suffisant entre les jobs
     * - Planifié le dimanche pour minimiser l'impact sur les opérations quotidiennes
     */
    @Scheduled(cron = "${cmt.main-levee.job.cron}")
    public void runScheduledJob() {
        if (!jobEnabled) {
            log.info("Le scheduler du job CMT Main Levée est désactivé dans la configuration. Exécution planifiée ignorée.");
            return;
        }

        log.info("================================================================================");
        log.info("EXÉCUTION PLANIFIÉE DÉCLENCHÉE : Job CMT Main Levée");
        log.info("Heure de déclenchement : {}", LocalDateTime.now().format(DATE_FORMATTER));
        log.info("================================================================================");

        JobExecution jobExecution = runJobInternal("SCHEDULED");

        if (jobExecution != null) {
            log.info(" Job planifié terminé avec le statut : {}", jobExecution.getStatus());
        } else {
            log.error(" Le job planifié n'a pas pu être démarré");
        }

        log.info("================================================================================");
        log.info("Exécution planifiée terminée à {}", LocalDateTime.now().format(DATE_FORMATTER));
        log.info("================================================================================");
    }

    /**
     * Déclenchement manuel du job de vérification des mains levées CMT.
     * Utile pour les tests, la maintenance ou les exécutions à la demande.
     *
     * @return Résultat de l'exécution du job, ou null si le job n'a pas pu démarrer
     */
    public JobExecution runJobManually() {
        log.info(" Tentative de déclenchement manuel du job CMT Main Levée");

        if (!jobEnabled) {
            log.warn(" Impossible d'exécuter le job manuellement - le scheduler est désactivé dans la configuration");
            log.warn("Activez le job en définissant 'cmt.main-levee.job.enabled=true' dans les propriétés de l'application");
            return null;
        }

        return runJobInternal("MANUAL");
    }

    /**
     * Méthode interne pour exécuter le job avec gestion complète des erreurs.
     *
     * @param triggerType Type de déclenchement (SCHEDULED, MANUAL, etc.)
     * @return Résultat de l'exécution du job, ou null si le job n'a pas pu démarrer
     */
    private JobExecution runJobInternal(String triggerType) {
        // Prévention des exécutions concurrentes
        if (!isJobRunning.compareAndSet(false, true)) {
            log.warn(" Le job CMT Main Levée est déjà en cours d'exécution. Cette exécution {} est ignorée.", triggerType);
            log.warn("Si vous pensez que le job est bloqué, vérifiez la base de données pour des transactions suspendues.");
            return null;
        }

        JobExecution jobExecution = null;
        long startTime = System.currentTimeMillis();
        BatchAuditContext.setCurrentJobName("CmtMainLevee");

        try {
            log.info(" Démarrage du job CMT Main Levée (Déclenchement : {})...", triggerType);

            // Génération de paramètres uniques pour assurer une nouvelle instance de job à chaque exécution
            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("runDate", new Date())
                    .addLong("runTime", System.currentTimeMillis())
                    .addString("triggerType", triggerType)
                    .addString("jobName", creditMoyenTermeMainLeveeJob.getName())
                    .toJobParameters();

            // Lancement du job
            jobExecution = jobLauncher.run(creditMoyenTermeMainLeveeJob, jobParameters);
            long duration = System.currentTimeMillis() - startTime;

            // Log du résultat du lancement
            log.info(" Job CMT Main Levée lancé avec succès");
            log.info("  ID d'exécution du job : {}", jobExecution.getId());
            log.info("  ID d'instance du job : {}", jobExecution.getJobInstance().getInstanceId());
            log.info("  Statut du job : {}", jobExecution.getStatus());
            log.info("  Durée du lancement : {} ms", duration);

            return jobExecution;

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn(" Le job CMT Main Levée est déjà en cours d'exécution (détecté par Spring Batch)");
            log.warn("Cela ne devrait pas se produire grâce à notre mécanisme de verrouillage AtomicBoolean");
            log.warn("Cause possible : Plusieurs instances du scheduler ou environnement distribué");
            log.warn("Solution : Assurez-vous qu'une seule instance de l'application exécute le scheduler, ou utilisez des verrous distribués");
            return null;

        } catch (JobRestartException e) {
            log.error(" Échec du redémarrage du job CMT Main Levée", e);
            log.error("Cela indique généralement un problème de configuration de la capacité de redémarrage du job");
            log.error("Vérifiez que le job est configuré pour permettre les redémarrages");
            log.error("Vérifiez si l'instance précédente du job s'est terminée avec le statut STOPPED");
            return null;

        } catch (JobInstanceAlreadyCompleteException e) {
            log.error(" L'instance du job CMT Main Levée est déjà terminée", e);
            log.error("Cela suggère un problème avec la génération de paramètres de job uniques");
            log.error("Horodatage actuel : {}, Nom du job : {}",
                    System.currentTimeMillis(), creditMoyenTermeMainLeveeJob.getName());
            log.error("Vérifiez l'unicité des JobParameters dans la méthode runJobInternal");
            return null;

        } catch (JobParametersInvalidException e) {
            log.error(" Paramètres de job invalides pour le job CMT Main Levée", e);
            log.error("Veuillez vérifier la configuration du job et les validateurs de paramètres");
            log.error("Vérifiez si des validateurs personnalisés rejettent les paramètres fournis");
            return null;

        } catch (Exception e) {
            log.error(" Erreur inattendue lors du lancement du job CMT Main Levée", e);
            log.error("Type d'erreur : {}", e.getClass().getSimpleName());
            log.error("Message d'erreur : {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Trace complète de la pile :", e);
            }
            return null;

        } finally {
            // Toujours libérer le verrou, même si une exception s'est produite
            isJobRunning.set(false);
            BatchAuditContext.clearCurrentJobName();

            long totalDuration = System.currentTimeMillis() - startTime;

            if (jobExecution != null) {
                log.info(" Exécution du job CMT Main Levée terminée");
                log.info("  Statut final : {}", jobExecution.getStatus());
                log.info("  Code de sortie : {}", jobExecution.getExitStatus().getExitCode());
                log.info("  Temps total : {} ms ({} secondes)", totalDuration, totalDuration / 1000);

                // Log des statistiques détaillées si disponibles
                if (jobExecution.getStepExecutions() != null && !jobExecution.getStepExecutions().isEmpty()) {
                    long totalItemsProcessed = jobExecution.getStepExecutions().stream()
                            .mapToLong(StepExecution::getWriteCount)
                            .sum();
                    log.info("  Total d'éléments traités : {}", totalItemsProcessed);
                }

            } else {
                log.error(" L'exécution du job CMT Main Levée n'a pas pu démarrer");
                log.error("  Temps total : {} ms", totalDuration);
            }

            log.info("--------------------------------------------------------------------------------");
        }
    }

    /**
     * Vérifie si le job est actuellement en cours d'exécution.
     * Utile pour les vérifications d'état et le monitoring.
     *
     * @return true si le job est en cours d'exécution, false sinon
     */
    public boolean isJobRunning() {
        return isJobRunning.get();
    }

    /**
     * Retourne la configuration CRON actuelle du job.
     *
     * @return Expression CRON de planification
     */
    public String getJobCron() {
        return jobCron;
    }

    /**
     * Vérifie si le job est activé dans la configuration.
     *
     * @return true si le job est activé, false sinon
     */
    public boolean isJobEnabled() {
        return jobEnabled;
    }
}