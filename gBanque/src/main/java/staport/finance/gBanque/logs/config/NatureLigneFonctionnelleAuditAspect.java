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
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.logs.entities.Log;
import staport.finance.gBanque.logs.entities.LogEntryDetail;
import staport.finance.gBanque.logs.services.LogService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Aspect
@Component
public class NatureLigneFonctionnelleAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(NatureLigneFonctionnelleAuditAspect.class);

    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final LogService logService;
    private final EntityManager entityManager;

    @Autowired
    public NatureLigneFonctionnelleAuditAspect(NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository,
                                               LogService logService,
                                               EntityManager entityManager) {
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.logService = logService;
        this.entityManager = entityManager;
    }

    // Pointcut pour cibler UNIQUEMENT les opérations de modification (update)
    @Pointcut("execution(* staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService.updateNatureLigneFonctionnelle(..))")
    public void natureLigneFonctionnelleUpdateOperation() {}

    @Around("natureLigneFonctionnelleUpdateOperation()")
    public Object auditNatureLigneFonctionnelleUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("Audit Aspect (NatureLigneFonctionnelle): Intercepting method: {}", joinPoint.getSignature().toShortString());

        Object[] args = joinPoint.getArgs();
        Long natureLigneFonctionnelleId = null;
        NatureLigneFonctionnelle originalNatureLigneFonctionnelle = null;
        String operationType = "UPDATE"; // Définir directement à UPDATE, car on n'audite que les mises à jour

        // Extraire l'ID pour l'opération de mise à jour
        if (args.length > 0 && args[0] instanceof Long) {
            natureLigneFonctionnelleId = (Long) args[0];
            logger.debug("Audit Aspect (NatureLigneFonctionnelle): Found ID for UPDATE operation: {}", natureLigneFonctionnelleId);
            // Récupérer l'entité originale avant de procéder
            Optional<NatureLigneFonctionnelle> optionalOriginal = natureLigneFonctionnelleRepository.findById(natureLigneFonctionnelleId);
            if (optionalOriginal.isPresent()) {
                originalNatureLigneFonctionnelle = optionalOriginal.get();
                // Détacher l'entité originale pour une comparaison fiable
                entityManager.detach(originalNatureLigneFonctionnelle);
                logger.debug("Audit Aspect (NatureLigneFonctionnelle): Original entity (ID: {}) detached.", natureLigneFonctionnelleId);
            } else {
                logger.warn("Audit Aspect (NatureLigneFonctionnelle): Original NatureLigneFonctionnelle not found for ID: {}. Cannot audit update.", natureLigneFonctionnelleId);
                // Procéder quand même, mais sans audit de l'état original
                return joinPoint.proceed();
            }
        } else {
            logger.warn("Audit Aspect (NatureLigneFonctionnelle): No valid ID found for update operation. Skipping audit for this call.");
            return joinPoint.proceed(); // Procéder sans audit si l'ID est introuvable
        }

        // Exécuter la méthode cible (mise à jour)
        Object result = joinPoint.proceed();
        logger.info("Audit Aspect (NatureLigneFonctionnelle): Target method {} executed. Result type: {}",
                joinPoint.getSignature().toShortString(),
                result != null ? result.getClass().getSimpleName() : "null");

        // Déterminer l'entité finale et son ID après l'opération
        NatureLigneFonctionnelle finalNatureLigneFonctionnelle = null;
        if (result instanceof NatureLigneFonctionnelleResponse) {
            // L'ID doit être le même que natureLigneFonctionnelleId car c'est une mise à jour
            // Nous utilisons natureLigneFonctionnelleId pour récupérer l'état final.
            Optional<NatureLigneFonctionnelle> optionalFinal = natureLigneFonctionnelleRepository.findById(natureLigneFonctionnelleId);
            if (optionalFinal.isPresent()) {
                finalNatureLigneFonctionnelle = optionalFinal.get();
                logger.debug("Audit Aspect (NatureLigneFonctionnelle): Final entity (ID: {}) fetched after operation.", natureLigneFonctionnelleId);
            } else {
                logger.warn("Audit Aspect (NatureLigneFonctionnelle): Final NatureLigneFonctionnelle not found for ID: {}. Audit may be incomplete.", natureLigneFonctionnelleId);
            }
        }

        if (finalNatureLigneFonctionnelle != null) {
            String userName = "System";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                userName = authentication.getName();
            }

            Log logEntry = new Log(userName, "NatureLigneFonctionnelle", finalNatureLigneFonctionnelle.getId());
            logEntry.setOperationType(operationType);

            List<LogEntryDetail> changedFields = new ArrayList<>();

            // Comparer les états pour l'opération UPDATE
            compareAndLogFields(originalNatureLigneFonctionnelle, finalNatureLigneFonctionnelle, changedFields);
            logger.info("Audit Aspect (NatureLigneFonctionnelle): Number of changed fields detected for update: {}", changedFields.size());

            if (!changedFields.isEmpty()) {
                changedFields.forEach(logEntry::addLogDetail);
                logService.saveLogWithDetails(logEntry, changedFields);
                logger.info("Audit Aspect (NatureLigneFonctionnelle): Successfully saved log for {} operation on NatureLigneFonctionnelle ID: {}", operationType, natureLigneFonctionnelleId);
            } else {
                logger.info("Audit Aspect (NatureLigneFonctionnelle): No changes detected for NatureLigneFonctionnelle ID: {}. No log details saved.", natureLigneFonctionnelleId);
            }
        } else {
            logger.warn("Audit Aspect (NatureLigneFonctionnelle): Skipping log save. Either final entity or original entity was null for update operation.");
        }

        return result; // Retourner le résultat de la méthode originale
    }

    private void compareAndLogFields(NatureLigneFonctionnelle oldNature, NatureLigneFonctionnelle newNature, List<LogEntryDetail> changedFields) {
        logger.debug("Audit Aspect (NatureLigneFonctionnelle): Starting field comparison for ID: {}", oldNature.getId());

        // Comparer les champs simples (label, description)
        compareField(oldNature.getLabel(), newNature.getLabel(), "label", oldNature.getId(), changedFields);
        compareField(oldNature.getDescription(), newNature.getDescription(), "description", oldNature.getId(), changedFields);

        logger.debug("Audit Aspect (NatureLigneFonctionnelle): Field comparison completed for ID: {}", oldNature.getId());
    }

    private <T> void compareField(T oldValue, T newValue, String fieldName, Long entityId, List<LogEntryDetail> changedFields) {
        logger.debug("Audit Aspect (NatureLigneFonctionnelle): Comparing field '{}' for ID {}: Old='{}', New='{}'", fieldName, entityId, oldValue, newValue);
        if (!Objects.equals(oldValue, newValue)) {
            logger.info("Audit Aspect (NatureLigneFonctionnelle): Field '{}' changed from '{}' to '{}' for ID {}", fieldName, oldValue, newValue, entityId);
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