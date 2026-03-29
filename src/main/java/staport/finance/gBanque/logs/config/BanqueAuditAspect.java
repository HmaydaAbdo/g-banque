package staport.finance.gBanque.logs.config;

import jakarta.persistence.EntityManager; // Importez EntityManager
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
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.logs.entities.Log;
import staport.finance.gBanque.logs.entities.LogEntryDetail;
import staport.finance.gBanque.logs.services.LogService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Aspect
@Component
public class BanqueAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(BanqueAuditAspect.class);

    private final BanqueRepository banqueRepository;
    private final LogService logService;
    private final EntityManager entityManager; // Déclarez EntityManager

    @Autowired
    public BanqueAuditAspect(BanqueRepository banqueRepository, LogService logService, EntityManager entityManager) {
        this.banqueRepository = banqueRepository;
        this.logService = logService;
        this.entityManager = entityManager; // Injectez EntityManager
    }

    @Pointcut("execution(* staport.finance.gBanque.Bank.services.BanqueService.save(..)) || " +
            "execution(* staport.finance.gBanque.Bank.services.BanqueService.update*(..))")
    public void banqueUpdateOperation() {}

    @Around("banqueUpdateOperation()")
    public Object auditBanqueUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("Audit Aspect: Intercepting method: {}", joinPoint.getSignature().toShortString());
        Object[] args = joinPoint.getArgs();
        Long banqueId = null;

        if (args.length > 0) {
            if (args[0] instanceof Long) {
                banqueId = (Long) args[0];
                logger.debug("Audit Aspect: Found Banque ID from first argument (Long): {}", banqueId);
            } else if (args[0] instanceof Banque) {
                Banque initialBanqueArgument = (Banque) args[0];
                if (initialBanqueArgument.getId() != null) {
                    banqueId = initialBanqueArgument.getId();
                    logger.debug("Audit Aspect: Found Banque ID from Banque object argument: {}", banqueId);
                } else {
                    logger.debug("Audit Aspect: Banque object argument has null ID (likely a new creation).");
                }
            } else {
                logger.debug("Audit Aspect: First argument is not a Long or Banque entity. Type: {}", args[0].getClass().getSimpleName());
            }
        } else {
            logger.debug("Audit Aspect: No arguments found for intercepted method.");
        }

        if (banqueId != null) {
            logger.info("Audit Aspect: Processing update for Banque ID: {}", banqueId);
            Optional<Banque> optionalOriginalBanque = banqueRepository.findById(banqueId);

            if (optionalOriginalBanque.isPresent()) {
                Banque originalBanque = optionalOriginalBanque.get();
                // DÉTACHER L'ENTITÉ ORIGINALE POUR ASSURER UNE COMPARAISON CORRECTE
                entityManager.detach(originalBanque); // <-- AJOUT IMPORTANT


                List<LogEntryDetail> changedFields = new ArrayList<>();

                // Exécuter la méthode du service
                Object result = joinPoint.proceed();
                logger.info("Audit Aspect: Target method {} executed. Result type: {}",
                        joinPoint.getSignature().toShortString(),
                        result != null ? result.getClass().getSimpleName() : "null");

                // Récupérer l'état final après l'exécution (et le flush dans le service)
                Optional<Banque> optionalFinalUpdatedBanque = banqueRepository.findById(banqueId);

                if (optionalFinalUpdatedBanque.isPresent()) {
                    Banque finalUpdatedBanque = optionalFinalUpdatedBanque.get();


                    compareAndLogFields(originalBanque, finalUpdatedBanque, changedFields);
                    logger.info("Audit Aspect: Number of changed fields detected: {}", changedFields.size());

                    if (!changedFields.isEmpty()) {
                        String userName = "System";
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                            userName = authentication.getName();
                            logger.debug("Audit Aspect: User authenticated as: {}", userName);
                        } else {
                            logger.debug("Audit Aspect: No authenticated user, using 'System'.");
                        }

                        Log log = new Log(userName, "Banque", banqueId);
                        log.setOperationType("UPDATE");
                        changedFields.forEach(log::addLogDetail);

                        logService.saveLogWithDetails(log, changedFields);
                        logger.info("Audit Aspect: Successfully saved log with {} details for Banque ID: {}", changedFields.size(), banqueId);
                    }
                } else {
                    logger.warn("Audit Aspect: Final updated Banque not found for ID: {}. Audit incomplete.", banqueId);
                }
                return result;
            } else {
                logger.warn("Audit Aspect: Original Banque not found for ID: {}. Skipping audit.", banqueId);
            }
        } else {
            logger.info("Audit Aspect: Banque ID is null. Skipping audit (likely a new creation or unhandled method signature).");
        }
        return joinPoint.proceed();
    }

    private void compareAndLogFields(Banque oldBanque, Banque newBanque, List<LogEntryDetail> changedFields) {
        logger.debug("Audit Aspect: Starting field comparison for Banque ID: {}", oldBanque.getId());
        compareField(oldBanque.getNom(), newBanque.getNom(), "nom", oldBanque, changedFields);
        compareField(oldBanque.getNumeroDeCompte(), newBanque.getNumeroDeCompte(), "numeroDeCompte", oldBanque, changedFields);
        compareField(oldBanque.getDevise(), newBanque.getDevise(), "devise", oldBanque, changedFields);
        compareField(oldBanque.getAddress(), newBanque.getAddress(), "address", oldBanque, changedFields);
        compareField(oldBanque.getAgence(), newBanque.getAgence(), "agence", oldBanque, changedFields);
        logger.debug("Audit Aspect: Field comparison completed for Banque ID: {}", oldBanque.getId());
    }

    private <T> void compareField(T oldValue, T newValue, String fieldName, Banque parentBanque, List<LogEntryDetail> changedFields) {
        logger.debug("Audit Aspect: Comparing field '{}': Old='{}', New='{}'", fieldName, oldValue, newValue);
        if (!Objects.equals(oldValue, newValue)) {
            logger.info("Audit Aspect: Field '{}' changed from '{}' to '{}'", fieldName, oldValue, newValue);
            LogEntryDetail detail = new LogEntryDetail(
                    parentBanque.getId() != null ? new Log(null, null, parentBanque.getId()) : null,
                    fieldName,
                    oldValue != null ? oldValue.toString() : null,
                    newValue != null ? newValue.toString() : null
            );
            changedFields.add(detail);
        }
    }
}