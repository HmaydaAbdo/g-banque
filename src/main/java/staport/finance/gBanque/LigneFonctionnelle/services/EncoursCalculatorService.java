package staport.finance.gBanque.LigneFonctionnelle.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service centralisé pour le calcul de l'encours et du solde selon la nature de la ligne fonctionnelle.
 * Utilisé par les lignes fonctionnelles générales ET spécifiques.
 */
@Service
public class EncoursCalculatorService {

    private static final Logger logger = LoggerFactory.getLogger(EncoursCalculatorService.class);

    /**
     * Calcule l'encours selon la nature de la ligne et le marché associé.
     *
     * @param ligneId ID de la ligne fonctionnelle
     * @param natureId ID de la nature de ligne fonctionnelle
     * @param marche Marché associé (peut être null pour lignes générales)
     * @param banque Banque associée (pour Facilité de Caisse)
     * @param plafond Plafond de la ligne
     * @param encoursStocke Encours stocké en base (pour Escompte Commercial)
     * @return L'encours calculé, ou null si la nature ne nécessite pas de calcul
     */
    public BigDecimal calculateEncours(Long ligneId, Long natureId, Marche marche, Banque banque,
                                       BigDecimal plafond, BigDecimal encoursStocke) {
        if (natureId == null) {
            logger.warn("Nature ID is null, cannot calculate encours");
            return null;
        }

        return switch (natureId.intValue()) {
            case 1 -> // Facilité de Caisse
                    calculateEncoursFaciliteCaisse(ligneId, banque, natureId);
            case 2 -> // Escompte Commercial
                    calculateEncoursEscompteCommercial(encoursStocke);
            case 11 -> // Préfinancement
                    calculateEncoursPrefinancement(ligneId, marche, natureId);
            case 12 -> // Droit Constaté
                    calculateEncoursDroitConstate(ligneId, marche, plafond, natureId);
            case 13 -> // Factoring
                    calculateEncoursFactoring(ligneId, marche, plafond, natureId);
            default -> {
                logger.debug("Nature ID {} does not require encours calculation", natureId);
                yield null;
            }
        };
    }

    /**
     * Calcule le solde selon la nature de la ligne.
     *
     * @param ligneId ID de la ligne fonctionnelle
     * @param natureId ID de la nature de ligne fonctionnelle
     * @param marche Marché associé (peut être null)
     * @param plafond Plafond de la ligne
     * @param encours Encours calculé
     * @return Le solde calculé, ou null si la nature ne nécessite pas de calcul
     */
    public BigDecimal calculateSolde(Long ligneId, Long natureId, Marche marche,
                                     BigDecimal plafond, BigDecimal encours) {
        if (natureId == null || encours == null) {
            return null;
        }

        return switch (natureId.intValue()) {
            case 1 -> // Facilité de Caisse
                    calculateSoldeFaciliteCaisse(plafond, encours);
            case 2 -> // Escompte Commercial
                    calculateSoldeEscompteCommercial(plafond, encours);
            case 11 -> // Préfinancement
                    calculateSoldePrefinancement(plafond, encours);
            case 12 -> // Droit Constaté
                    calculateSoldeDroitConstate(ligneId, marche, natureId);
            case 13 -> // Factoring
                    calculateSoldeFactoring(ligneId, marche, natureId);
            default -> {
                logger.debug("Nature ID {} does not require solde calculation", natureId);
                yield null;
            }
        };
    }

    // ========== FACILITÉ DE CAISSE (ID: 1) ==========

    /**
     * Facilité de Caisse : encours = solde de la banque
     * ⚠️ REQUIS : Une banque DOIT être associée pour cette nature
     *
     * @param ligneId ID de la ligne fonctionnelle
     * @param banque Banque associée
     * @param natureId ID de la nature
     * @return Le solde de la banque (encours)
     */
    private BigDecimal calculateEncoursFaciliteCaisse(Long ligneId, Banque banque, Long natureId) {
        validateBanqueRequired(ligneId, banque, natureId);

        BigDecimal soldeBanque = banque.getSoldeActuel();
        logger.debug("Encours Facilité de Caisse = Solde de la banque '{}': {}",
                banque.getNom(), soldeBanque);

        return soldeBanque != null ? soldeBanque : BigDecimal.ZERO;
    }

    /**
     * Facilité de Caisse : solde = plafond - encours
     *
     * @param plafond Plafond de la ligne
     * @param encours Encours (= solde de la banque)
     * @return Le solde disponible
     */
    private BigDecimal calculateSoldeFaciliteCaisse(BigDecimal plafond, BigDecimal encours) {
        BigDecimal solde = plafond.subtract(encours);
        logger.debug("Solde Facilité de Caisse: {} (Plafond: {} - Encours/Solde Banque: {})",
                solde, plafond, encours);
        return solde;
    }

    // ========== ESCOMPTE COMMERCIAL (ID: 2) ==========

    /**
     * Escompte Commercial : encours = valeur stockée en base
     */
    private BigDecimal calculateEncoursEscompteCommercial(BigDecimal encoursStocke) {
        logger.debug("Calculating encours for Escompte Commercial: {}", encoursStocke);
        return encoursStocke != null ? encoursStocke : BigDecimal.ZERO;
    }

    /**
     * Escompte Commercial : solde = plafond - encours
     */
    private BigDecimal calculateSoldeEscompteCommercial(BigDecimal plafond, BigDecimal encours) {
        BigDecimal solde = plafond.subtract(encours);
        logger.debug("Solde Escompte Commercial: {} (Plafond: {} - Encours: {})", solde, plafond, encours);
        return solde;
    }

    // ========== PRÉFINANCEMENT (ID: 11) ==========

    /**
     * Préfinancement : encours = Σ remboursements
     * REQUIS : Un marché DOIT être associé pour cette nature
     */
    private BigDecimal calculateEncoursPrefinancement(Long ligneId, Marche marche, Long natureId) {
        validateMarcheRequired(ligneId, marche, natureId, "Préfinancement");

        List<Prefinancement> prefinancements = marche.getPrefinancements();
        logger.debug("Calculating encours Préfinancement from {} prefinancements",
                prefinancements != null ? prefinancements.size() : 0);

        if (prefinancements == null || prefinancements.isEmpty()) {
            logger.debug("No prefinancements found, encours = 0");
            return BigDecimal.ZERO;
        }

        BigDecimal encours = prefinancements.stream()
                .filter(p -> p.getType() == Type.REMBOURSEMENT)
                .map(Prefinancement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        logger.debug("Encours Préfinancement calculated: {}", encours);
        return encours;
    }

    /**
     * Préfinancement : solde = plafond - encours
     */
    private BigDecimal calculateSoldePrefinancement(BigDecimal plafond, BigDecimal encours) {
        BigDecimal solde = plafond.subtract(encours);
        logger.debug("Solde Préfinancement: {} (Plafond: {} - Encours: {})",
                solde, plafond, encours);
        return solde;
    }

    // ========== DROIT CONSTATÉ (ID: 12) ==========

    /**
     * Droit Constaté : encours = plafond - Σ déblocages + Σ remboursements
     * ⚠ REQUIS : Un marché DOIT être associé pour cette nature
     */
    private BigDecimal calculateEncoursDroitConstate(Long ligneId, Marche marche, BigDecimal plafond, Long natureId) {
        validateMarcheRequired(ligneId, marche, natureId, "Droit Constaté");

        List<DroitConstate> droitConstates = marche.getDroitConstates();
        logger.debug("Calculating encours Droit Constaté from {} droits constatés",
                droitConstates != null ? droitConstates.size() : 0);

        if (droitConstates == null || droitConstates.isEmpty()) {
            logger.debug("No droits constatés found, encours = plafond ({})", plafond);
            return plafond;
        }

        BigDecimal totalDeblocages = droitConstates.stream()
                .filter(dc -> dc.getType() == Type.DEBLOCAGE)
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemboursements = droitConstates.stream()
                .filter(dc -> dc.getType() == Type.REMBOURSEMENT)
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal encours = plafond.subtract(totalDeblocages).add(totalRemboursements);

        logger.debug("Encours Droit Constaté: {} (Plafond: {} - Déblocages: {} + Remboursements: {})",
                encours, plafond, totalDeblocages, totalRemboursements);
        return encours;
    }

    /**
     * Droit Constaté : solde = Σ déblocages - Σ remboursements
     *  REQUIS : Un marché DOIT être associé pour cette nature
     */
    private BigDecimal calculateSoldeDroitConstate(Long ligneId, Marche marche, Long natureId) {
        validateMarcheRequired(ligneId, marche, natureId, "Droit Constaté");

        List<DroitConstate> droitConstates = marche.getDroitConstates();

        if (droitConstates == null || droitConstates.isEmpty()) {
            logger.debug("No droits constatés found, solde = 0");
            return BigDecimal.ZERO;
        }

        BigDecimal totalDeblocages = droitConstates.stream()
                .filter(dc -> dc.getType() == Type.DEBLOCAGE)
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemboursements = droitConstates.stream()
                .filter(dc -> dc.getType() == Type.REMBOURSEMENT)
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal solde = totalDeblocages.subtract(totalRemboursements);

        logger.debug("Solde Droit Constaté: {} (Déblocages: {} - Remboursements: {})",
                solde, totalDeblocages, totalRemboursements);
        return solde;
    }

    // ========== FACTORING (ID: 13) ==========

    /**
     * Factoring : encours = plafond - Σ déblocages + Σ remboursements
     * REQUIS : Un marché DOIT être associé pour cette nature
     */
    private BigDecimal calculateEncoursFactoring(Long ligneId, Marche marche, BigDecimal plafond, Long natureId) {
        validateMarcheRequired(ligneId, marche, natureId, "Factoring");

        List<Factoring> factorings = marche.getFactorings();
        logger.debug("Calculating encours Factoring from {} factorings",
                factorings != null ? factorings.size() : 0);

        if (factorings == null || factorings.isEmpty()) {
            logger.debug("No factorings found, encours = plafond ({})", plafond);
            return plafond;
        }

        BigDecimal totalDeblocages = factorings.stream()
                .filter(f -> f.getType() == Type.DEBLOCAGE)
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemboursements = factorings.stream()
                .filter(f -> f.getType() == Type.REMBOURSEMENT)
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal encours = plafond.subtract(totalDeblocages).add(totalRemboursements);

        logger.debug("Encours Factoring: {} (Plafond: {} - Déblocages: {} + Remboursements: {})",
                encours, plafond, totalDeblocages, totalRemboursements);
        return encours;
    }

    /**
     * Factoring : solde = Σ déblocages - Σ remboursements
     * REQUIS : Un marché DOIT être associé pour cette nature
     */
    private BigDecimal calculateSoldeFactoring(Long ligneId, Marche marche, Long natureId) {
        validateMarcheRequired(ligneId, marche, natureId, "Factoring");

        List<Factoring> factorings = marche.getFactorings();

        if (factorings == null || factorings.isEmpty()) {
            logger.debug("No factorings found, solde = 0");
            return BigDecimal.ZERO;
        }

        BigDecimal totalDeblocages = factorings.stream()
                .filter(f -> f.getType() == Type.DEBLOCAGE)
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemboursements = factorings.stream()
                .filter(f -> f.getType() == Type.REMBOURSEMENT)
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal solde = totalDeblocages.subtract(totalRemboursements);

        logger.debug("Solde Factoring: {} (Déblocages: {} - Remboursements: {})",
                solde, totalDeblocages, totalRemboursements);
        return solde;
    }

    // ========== VALIDATIONS ==========

    /**
     * Valide qu'une banque est présente pour les natures qui l'exigent.
     *
     * @param ligneId  ID de la ligne fonctionnelle
     * @param banque   La banque (peut être null)
     * @param natureId ID de la nature
     */
    private void validateBanqueRequired(Long ligneId, Banque banque, Long natureId) {
        if (banque == null) {
            logger.error("Banque requise mais absente pour la nature '{}' (ID: {})", "Facilité de Caisse", natureId);
            throw new ResourceNotFoundException(
                    "Une banque est obligatoire pour la ligne avec l'id " + ligneId + " et de nature '" + "Facilité de Caisse" + "'. " +
                            "Impossible de calculer l'encours et le solde sans banque associée."
            );
        }
    }

    /**
     * Valide qu'un marché est présent pour les natures qui l'exigent.
     *
     * @param ligneId ID de la ligne fonctionnelle
     * @param marche Le marché (peut être null)
     * @param natureId ID de la nature
     * @param natureLabel Label de la nature (pour le message d'erreur)
     */
    private void validateMarcheRequired(Long ligneId, Marche marche, Long natureId, String natureLabel) {
        if (marche == null) {
            logger.error("Marché requis mais absent pour la nature '{}' (ID: {})", natureLabel, natureId);
            throw new ResourceNotFoundException(
                    "Un marché est obligatoire pour la ligne avec l'id " + ligneId + " et de nature '" + natureLabel + "'. " +
                            "Impossible de calculer l'encours et le solde sans marché associé."
            );
        }
    }
}