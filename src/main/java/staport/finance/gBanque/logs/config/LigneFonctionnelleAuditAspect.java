package staport.finance.gBanque.logs.config;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.LigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.logs.entities.Log;
import staport.finance.gBanque.logs.entities.LogEntryDetail;
import staport.finance.gBanque.logs.services.LogService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Aspect
@Component
public class LigneFonctionnelleAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(LigneFonctionnelleAuditAspect.class);

    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    private final LogService logService;
    private final EntityManager entityManager;

    @Autowired
    public LigneFonctionnelleAuditAspect(LigneFonctionnelleRepository ligneFonctionnelleRepository,
                                         LogService logService,
                                         EntityManager entityManager) {
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        this.logService = logService;
        this.entityManager = entityManager;
    }

    // Pointcut pour cibler UNIQUEMENT les opérations de modification (update)
    @Pointcut("execution(* staport.finance.gBanque.LigneFonctionnelle.services.LigneFonctionnelleService.updateLigneFonctionnelle(..))")
    public void ligneFonctionnelleUpdateOperation() {}

    // Pointcut to target ONLY deactivate operations
    @Pointcut("execution(* staport.finance.gBanque.LigneFonctionnelle.services.LigneFonctionnelleService.deactivateLigneFonctionnelleByNatureAndBank(..))")
    public void ligneFonctionnelleDeactivateOperation() {}

    @Around("ligneFonctionnelleUpdateOperation()")
    public Object auditLigneFonctionnelleUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("Audit Aspect (LigneFonctionnelle): Intercepting method: {}", joinPoint.getSignature().toShortString());

        Object[] args = joinPoint.getArgs();
        Long ligneFonctionnelleId = null;
        LigneFonctionnelle originalLigneFonctionnelle = null;
        String operationType = "UPDATE"; // Définir directement à UPDATE

        // Extraire l'ID pour l'opération de mise à jour
        if (args.length > 0 && args[0] instanceof Long) {
            ligneFonctionnelleId = (Long) args[0];
            logger.debug("Audit Aspect (LigneFonctionnelle): Found ID for UPDATE operation: {}", ligneFonctionnelleId);
            // Récupérer l'entité originale avant de procéder
            Optional<LigneFonctionnelle> optionalOriginal = ligneFonctionnelleRepository.findById(ligneFonctionnelleId);
            if (optionalOriginal.isPresent()) {
                originalLigneFonctionnelle = optionalOriginal.get();
                // Détacher l'entité originale pour une comparaison fiable
                entityManager.detach(originalLigneFonctionnelle);
                logger.debug("Audit Aspect (LigneFonctionnelle): Original entity (ID: {}) detached.", ligneFonctionnelleId);
            } else {
                logger.warn("Audit Aspect (LigneFonctionnelle): Original LigneFonctionnelle not found for ID: {}. Cannot audit update.", ligneFonctionnelleId);
                // Procéder quand même, mais sans audit de l'état original
                return joinPoint.proceed();
            }
        } else {
            logger.warn("Audit Aspect (LigneFonctionnelle): No valid ID found for update operation. Skipping audit for this call.");
            return joinPoint.proceed(); // Procéder sans audit si l'ID est introuvable
        }


        // Exécuter la méthode cible (mise à jour)
        Object result = joinPoint.proceed();
        logger.info("Audit Aspect (LigneFonctionnelle): Target method {} executed. Result type: {}",
                joinPoint.getSignature().toShortString(),
                result != null ? result.getClass().getSimpleName() : "null");

        // Déterminer l'entité finale et son ID après l'opération
        LigneFonctionnelle finalLigneFonctionnelle = null;
        if (result instanceof LigneFonctionnelleResponse) {
            // L'ID doit être le même que ligneFonctionnelleId car c'est une mise à jour
            // Nous utilisons ligneFonctionnelleId pour récupérer l'état final.
            Optional<LigneFonctionnelle> optionalFinal = ligneFonctionnelleRepository.findById(ligneFonctionnelleId);
            if (optionalFinal.isPresent()) {
                finalLigneFonctionnelle = optionalFinal.get();
                logger.debug("Audit Aspect (LigneFonctionnelle): Final entity (ID: {}) fetched after operation.", ligneFonctionnelleId);
            } else {
                logger.warn("Audit Aspect (LigneFonctionnelle): Final LigneFonctionnelle not found for ID: {}. Audit may be incomplete.", ligneFonctionnelleId);
            }
        }

        if (finalLigneFonctionnelle != null) {
            String userName = "System";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                userName = authentication.getName();
            }

            Log logEntry = new Log(userName, "LigneFonctionnelle", finalLigneFonctionnelle.getId());
            logEntry.setOperationType(operationType);

            List<LogEntryDetail> changedFields = new ArrayList<>();

            // Comparer les états pour l'opération UPDATE
            compareAndLogFields(originalLigneFonctionnelle, finalLigneFonctionnelle, changedFields);
            logger.info("Audit Aspect (LigneFonctionnelle): Number of changed fields detected for update: {}", changedFields.size());

            if (!changedFields.isEmpty()) {
                changedFields.forEach(logEntry::addLogDetail);
                logService.saveLogWithDetails(logEntry, changedFields);
                logger.info("Audit Aspect (LigneFonctionnelle): Successfully saved log for {} operation on LigneFonctionnelle ID: {}", operationType, ligneFonctionnelleId);
            } else {
                logger.info("Audit Aspect (LigneFonctionnelle): No changes detected for LigneFonctionnelle ID: {}. No log details saved.", ligneFonctionnelleId);
            }
        } else {
            logger.warn("Audit Aspect (LigneFonctionnelle): Skipping log save. Either final entity or original entity was null for update operation.");
        }

        return result; // Retourner le résultat de la méthode originale
    }

    @Around("ligneFonctionnelleDeactivateOperation()")
    public Object auditLigneFonctionnelleDeactivate(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("Audit Aspect (LigneFonctionnelle): Intercepting deactivation method: {}", joinPoint.getSignature().toShortString());

        Object[] args = joinPoint.getArgs();
        Long banqueId = null;
        Long natureLigneFonctionnelleId = null;
        LigneFonctionnelle originalLigneFonctionnelle = null;
        String operationType = "DESACTIVATE";

        if (args.length == 2 && args[0] instanceof Long && args[1] instanceof Long) {
            banqueId = (Long) args[0];
            natureLigneFonctionnelleId = (Long) args[1];
            logger.debug("Audit Aspect (LigneFonctionnelle): Found Banque ID: {} and NatureLigneFonctionnelle ID: {} for DEACTIVATE operation.", banqueId, natureLigneFonctionnelleId);

            // Retrieve the original active entity before deactivation
            Optional<LigneFonctionnelle> optionalOriginal = ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banqueId, natureLigneFonctionnelleId);
            if (optionalOriginal.isPresent()) {
                originalLigneFonctionnelle = optionalOriginal.get();
                entityManager.detach(originalLigneFonctionnelle); // Detach for reliable comparison
                logger.debug("Audit Aspect (LigneFonctionnelle): Original active entity (ID: {}) detached for deactivation audit.", originalLigneFonctionnelle.getId());
            } else {
                logger.warn("Audit Aspect (LigneFonctionnelle): No active LigneFonctionnelle found for Banque ID: {} and Nature ID: {}. Cannot audit deactivation.", banqueId, natureLigneFonctionnelleId);
                return joinPoint.proceed(); // Proceed without audit if original not found
            }
        } else {
            logger.warn("Audit Aspect (LigneFonctionnelle): Invalid arguments for deactivation operation. Skipping audit for this call.");
            return joinPoint.proceed(); // Proceed without audit if arguments are invalid
        }

        // Execute the target method (deactivation)
        Object result = joinPoint.proceed();
        logger.info("Audit Aspect (LigneFonctionnelle): Deactivation method {} executed.", joinPoint.getSignature().toShortString());

        // After deactivation, the entity's 'actif' status should be false.
        // We fetch it again to get its final state.
        LigneFonctionnelle finalLigneFonctionnelle = ligneFonctionnelleRepository.findById(originalLigneFonctionnelle.getId())
                .orElse(null);

        if (finalLigneFonctionnelle != null) {
            String userName = "System";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                userName = authentication.getName();
            }

            Log logEntry = new Log(userName, "LigneFonctionnelle", finalLigneFonctionnelle.getId());
            logEntry.setOperationType(operationType);

            List<LogEntryDetail> changedFields = new ArrayList<>();

            // Explicitly log the 'actif' field change
            compareField(originalLigneFonctionnelle.isActif(), finalLigneFonctionnelle.isActif(), "actif", finalLigneFonctionnelle.getId(), changedFields);
            logger.info("Audit Aspect (LigneFonctionnelle): Deactivation - Number of changed fields detected: {}", changedFields.size());


            if (!changedFields.isEmpty()) {
                changedFields.forEach(logEntry::addLogDetail);
                logService.saveLogWithDetails(logEntry, changedFields);
                logger.info("Audit Aspect (LigneFonctionnelle): Successfully saved log for {} operation on LigneFonctionnelle ID: {}", operationType, finalLigneFonctionnelle.getId());
            } else {
                logger.warn("Audit Aspect (LigneFonctionnelle): Deactivation performed, but no 'actif' change detected for ID: {}. Log may be incomplete.", finalLigneFonctionnelle.getId());
            }
        } else {
            logger.warn("Audit Aspect (LigneFonctionnelle): Skipping log save for deactivation. Final LigneFonctionnelle entity not found after operation (ID: {}).", originalLigneFonctionnelle.getId());
        }

        return result;
    }

    private void compareAndLogFields(LigneFonctionnelle oldLigne, LigneFonctionnelle newLigne, List<LogEntryDetail> changedFields) {
        logger.debug("Audit Aspect (LigneFonctionnelle): Starting field comparison for ID: {}", oldLigne.getId());

        // Comparer les champs non relationnels simples
        compareField(oldLigne.getPlafond(), newLigne.getPlafond(), "plafond", oldLigne.getId(), changedFields);
        compareField(oldLigne.getTaux(), newLigne.getTaux(), "taux", oldLigne.getId(), changedFields);
        compareField(oldLigne.getDateDepart(), newLigne.getDateDepart(), "dateDepart", oldLigne.getId(), changedFields);
        compareField(oldLigne.getDateFin(), newLigne.getDateFin(), "dateFin", oldLigne.getId(), changedFields);

        // Comparer les IDs des entités liées (ne pas comparer les objets entiers pour éviter les problèmes de Lazy Loading)
        compareField(oldLigne.getBanque() != null ? oldLigne.getBanque().getId() : null,
                newLigne.getBanque() != null ? newLigne.getBanque().getId() : null,
                "banqueId", oldLigne.getId(), changedFields);

        compareField(oldLigne.getNatureLigneFonctionnelle() != null ? oldLigne.getNatureLigneFonctionnelle().getId() : null,
                newLigne.getNatureLigneFonctionnelle() != null ? newLigne.getNatureLigneFonctionnelle().getId() : null,
                "natureLigneFonctionnelleId", oldLigne.getId(), changedFields);

        logger.debug("Audit Aspect (LigneFonctionnelle): Field comparison completed for ID: {}", oldLigne.getId());
    }

    private <T> void compareField(T oldValue, T newValue, String fieldName, Long entityId, List<LogEntryDetail> changedFields) {
        logger.debug("Audit Aspect (LigneFonctionnelle): Comparing field '{}' for ID {}: Old='{}', New='{}'", fieldName, entityId, oldValue, newValue);

        boolean changed = false;
        // Gérer spécifiquement la comparaison des BigDecimal
        if (oldValue instanceof BigDecimal && newValue instanceof BigDecimal) {
            changed = ((BigDecimal) oldValue).compareTo((BigDecimal) newValue) != 0;
        } else {
            // Pour tous les autres types, utiliser Objects.equals (y compris pour null)
            changed = !Objects.equals(oldValue, newValue);
        }

        if (changed) {
            logger.info("Audit Aspect (LigneFonctionnelle): Field '{}' changed from '{}' to '{}' for ID {}", fieldName, oldValue, newValue, entityId);
            LogEntryDetail detail = new LogEntryDetail(
                    new Log(null, null, entityId), // Log temporaire, seulement l'ID est important ici
                    fieldName,
                    oldValue != null ? oldValue.toString() : null,
                    newValue != null ? newValue.toString() : null
            );
            changedFields.add(detail);
        }
    }
}