package staport.finance.gBanque.LigneFonctionnelle.generalLignes.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.specs.LcnSpecification;
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
    private final LcnRepository lcnRepository;

    public EncoursCalculatorService(LcnRepository lcnRepository) {
        this.lcnRepository = lcnRepository;
    }

    /**
     * Calcule l'encours selon la nature de la ligne et le marché associé.
     *
     * @param ligneId ID de la ligne fonctionnelle
     * @param natureId ID de la nature de ligne fonctionnelle
     * @param marche Marché associé (peut être null pour lignes générales)
     * @param banque Banque associée (pour Facilité de Caisse)
     * @param plafond Plafond de la ligne
     * @return L'encours calculé, ou null si la nature ne nécessite pas de calcul ou si les données requises sont absentes
     */
    public BigDecimal calculateEncours(Long ligneId, Long natureId, Marche marche, Banque banque,
                                       BigDecimal plafond) {
        if (natureId == null) {
            logger.warn("Nature ID is null, cannot calculate encours");
            return null;
        }

        return switch (natureId.intValue()) {
            case 1 -> // Facilité de Caisse
                    calculateEncoursFaciliteCaisse(ligneId, banque, natureId);
            case 2 -> // Escompte Commercial
                    calculateEncoursEscompteCommercial(ligneId);
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
     * @return Le solde calculé, ou null si la nature ne nécessite pas de calcul ou si les données requises sont absentes
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
     * @return Le solde de la banque (encours), ou null si banque absente
     */
    private BigDecimal calculateEncoursFaciliteCaisse(Long ligneId, Banque banque, Long natureId) {
        if (banque == null) {
            logger.warn("Banque absente pour la nature 'Facilité de Caisse' (ligne ID: {}) - Encours set to null", ligneId);
            return null;
        }

        BigDecimal soldeBanque = banque.getSoldeActuel();
        logger.debug("Encours Facilité de Caisse = Solde de la banque '{}': {}",
                banque.getNom(), soldeBanque);

        return soldeBanque != null ? soldeBanque : BigDecimal.ZERO;
    }

    /**
     * Facilité de Caisse : solde = plafond - encours
     *
     * @param plafond Plafond de la ligne
     * @return Le solde disponible
     */
    private BigDecimal calculateSoldeFaciliteCaisse(BigDecimal plafond, BigDecimal soldeBanque) {

        BigDecimal solde;

        if (soldeBanque.signum() >= 0) {
            // Pas d'utilisation de la facilité
            solde = plafond;
        } else {
            // Utilisation de la facilité
            solde = plafond.subtract(soldeBanque.abs());
        }

        logger.debug(
                "Solde Facilité de Caisse: {} (Plafond: {}, Solde Banque: {})",
                solde, plafond, soldeBanque
        );

        return solde;
    }


    // ========== ESCOMPTE COMMERCIAL (ID: 2) ==========

    /**
     * Escompte Commercial : encours = somme des montants des LCN de type ESCOMPTE
     * Calcul dynamique via le repository
     *
     * @param ligneId ID de la ligne fonctionnelle
     * @return L'encours calculé (somme des LCN actifs)
     */
    private BigDecimal calculateEncoursEscompteCommercial(Long ligneId) {
        logger.debug("Calculating encours for Escompte Commercial (ligne ID: {})", ligneId);

        //  Récupérer tous les LCN  de cette ligne fonctionnelle
        List<Lcn> lcnList = lcnRepository.findAll(LcnSpecification.hasLigneFonctionnelle(ligneId));

        logger.debug("Found {} active LCN(s) for ligne fonctionnelle ID: {}", lcnList.size(), ligneId);

        //  Calculer la somme des montants
        BigDecimal encours = lcnList.stream()
                .map(Lcn::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        logger.debug("Encours Escompte Commercial calculé pour ligne ID {}: {} (basé sur {} LCN actifs)",
                ligneId, encours, lcnList.size());

        return encours;
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
     * Note: Si le marché est absent, retourne null
     */
    private BigDecimal calculateEncoursPrefinancement(Long ligneId, Marche marche, Long natureId) {
        if (marche == null) {
            logger.warn("Marché absent pour la nature 'Préfinancement' (ligne ID: {}) - Encours set to null", ligneId);
            return null;
        }

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
     * Note: Si le marché est absent, retourne null
     */
    private BigDecimal calculateEncoursDroitConstate(Long ligneId, Marche marche, BigDecimal plafond, Long natureId) {
        if (marche == null) {
            logger.warn("Marché absent pour la nature 'Droit Constaté' (ligne ID: {}) - Encours set to null", ligneId);
            return null;
        }

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
     * Note: Si le marché est absent, retourne null
     */
    private BigDecimal calculateSoldeDroitConstate(Long ligneId, Marche marche, Long natureId) {
        if (marche == null) {
            logger.warn("Marché absent pour la nature 'Droit Constaté' (ligne ID: {}) - Solde set to null", ligneId);
            return null;
        }

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
     * Note: Si le marché est absent, retourne null
     */
    private BigDecimal calculateEncoursFactoring(Long ligneId, Marche marche, BigDecimal plafond, Long natureId) {
        if (marche == null) {
            logger.warn("Marché absent pour la nature 'Factoring' (ligne ID: {}) - Encours set to null", ligneId);
            return null;
        }

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
     * Note: Si le marché est absent, retourne null
     */
    private BigDecimal calculateSoldeFactoring(Long ligneId, Marche marche, Long natureId) {
        if (marche == null) {
            logger.warn("Marché absent pour la nature 'Factoring' (ligne ID: {}) - Solde set to null", ligneId);
            return null;
        }

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
}