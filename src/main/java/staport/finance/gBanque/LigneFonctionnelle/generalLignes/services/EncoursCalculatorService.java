package staport.finance.gBanque.LigneFonctionnelle.generalLignes.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.specs.LcnSpecification;
import staport.finance.gBanque.moyensDePaiements.lcn.services.LcnReader;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.transactions.service.ITransactionService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Service centralisé pour le calcul de l'encours et du solde selon la nature de la ligne fonctionnelle.
 * Utilisé par les lignes fonctionnelles générales ET spécifiques.
 * <p>
 * Une ligne générale peut être partagée entre plusieurs marchés. Toutes les méthodes qui
 * dépendent du marché acceptent désormais une {@code List<Marche>} et agrègent les données
 * sur l'ensemble des marchés liés.
 */
@Service
public class EncoursCalculatorService {

    private static final Logger log = LoggerFactory.getLogger(EncoursCalculatorService.class);
    private final LcnReader lcnReader;
    private final ITransactionService transactionService;

    public EncoursCalculatorService(LcnReader lcnReader,
                                    ITransactionService transactionService) {
        this.lcnReader = lcnReader;
        this.transactionService = transactionService;
    }

    /**
     * Calcule l'encours selon la nature de la ligne et les marchés associés.
     *
     * @param ligneId  ID de la ligne fonctionnelle
     * @param natureId ID de la nature de ligne fonctionnelle
     * @param marches  Marchés associés (vide pour une ligne générale sans marché)
     * @param banque   Banque associée (pour Facilité de Caisse)
     * @param plafond  Plafond de la ligne
     * @return L'encours calculé, ou null si la nature ne nécessite pas de calcul ou si les données requises sont absentes
     */
    public BigDecimal calculateEncours(Long ligneId, Long natureId, List<Marche> marches, Banque banque,
                                       BigDecimal plafond) {
        if (natureId == null) {
            log.warn("Nature ID is null, cannot calculate encours");
            return null;
        }

        List<Marche> safeMarches = marches != null ? marches : Collections.emptyList();

        return switch (natureId.intValue()) {
            case 1 ->  // Facilité de Caisse
                    calculateEncoursFaciliteCaisse(ligneId, banque, natureId);
            case 2 ->  // Escompte Commercial
                    calculateEncoursEscompteCommercial(ligneId);
            case 11 -> // Préfinancement
                    calculateEncoursPrefinancement(ligneId, safeMarches, natureId);
            case 12 -> // Droit Constaté
                    calculateEncoursDroitConstate(ligneId, safeMarches, plafond, natureId);
            case 13 -> // Factoring
                    calculateEncoursFactoring(ligneId, safeMarches, plafond, natureId);
            default -> {
                log.debug("Nature ID {} does not require encours calculation", natureId);
                yield null;
            }
        };
    }

    /**
     * Calcule le solde selon la nature de la ligne.
     *
     * @param ligneId  ID de la ligne fonctionnelle
     * @param natureId ID de la nature de ligne fonctionnelle
     * @param marches  Marchés associés (vide pour une ligne générale sans marché)
     * @param plafond  Plafond de la ligne
     * @param encours  Encours calculé
     * @return Le solde calculé, ou null si la nature ne nécessite pas de calcul ou si les données requises sont absentes
     */
    public BigDecimal calculateSolde(Long ligneId, Long natureId, List<Marche> marches,
                                     BigDecimal plafond, BigDecimal encours) {
        if (natureId == null || encours == null) {
            return null;
        }

        List<Marche> safeMarches = marches != null ? marches : Collections.emptyList();

        return switch (natureId.intValue()) {
            case 1 ->  // Facilité de Caisse
                    calculateSoldeFaciliteCaisse(plafond, encours);
            case 2 ->  // Escompte Commercial
                    calculateSoldeEscompteCommercial(plafond, encours);
            case 11 -> // Préfinancement
                    calculateSoldePrefinancement(plafond, encours);
            case 12 -> // Droit Constaté
                    calculateSoldeDroitConstate(ligneId, safeMarches, natureId);
            case 13 -> // Factoring
                    calculateSoldeFactoring(ligneId, safeMarches, natureId);
            default -> {
                log.debug("Nature ID {} does not require solde calculation", natureId);
                yield null;
            }
        };
    }

    // ========== FACILITÉ DE CAISSE (ID: 1) ==========

    /**
     * Facilité de Caisse : encours = solde de la banque
     * ⚠️ REQUIS : Une banque DOIT être associée pour cette nature
     */
    private BigDecimal calculateEncoursFaciliteCaisse(Long ligneId, Banque banque, Long natureId) {
        if (banque == null) {
            log.warn("Banque absente pour la nature 'Facilité de Caisse' (ligne ID: {}) - Encours set to null", ligneId);
            return null;
        }

        BigDecimal soldeBanque = transactionService.computeBalance(banque.getId());
        log.debug("Encours Facilité de Caisse = Solde de la banque '{}': {}",
                banque.getNom(), soldeBanque);

        return soldeBanque != null ? soldeBanque : BigDecimal.ZERO;
    }

    /**
     * Facilité de Caisse : solde = plafond - |encours négatif|
     */
    private BigDecimal calculateSoldeFaciliteCaisse(BigDecimal plafond, BigDecimal soldeBanque) {
        BigDecimal solde;

        if (soldeBanque.signum() >= 0) {
            solde = plafond;
        } else {
            solde = plafond.subtract(soldeBanque.abs());
        }

        log.debug("Solde Facilité de Caisse: {} (Plafond: {}, Solde Banque: {})",
                solde, plafond, soldeBanque);

        return solde;
    }

    // ========== ESCOMPTE COMMERCIAL (ID: 2) ==========

    /**
     * Escompte Commercial : encours = somme des montants des LCN
     */
    private BigDecimal calculateEncoursEscompteCommercial(Long ligneId) {
        log.debug("Calculating encours for Escompte Commercial (ligne ID: {})", ligneId);

        List<Lcn> lcnList = lcnReader.findAll(LcnSpecification.hasLigneFonctionnelle(ligneId));

        log.debug("Found {} LCN(s) for ligne fonctionnelle ID: {}", lcnList.size(), ligneId);

        BigDecimal encours = lcnList.stream()
                .map(Lcn::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Encours Escompte Commercial calculé pour ligne ID {}: {} (basé sur {} LCN)",
                ligneId, encours, lcnList.size());

        return encours;
    }

    /**
     * Escompte Commercial : solde = plafond - encours
     */
    private BigDecimal calculateSoldeEscompteCommercial(BigDecimal plafond, BigDecimal encours) {
        BigDecimal solde = plafond.subtract(encours);
        log.debug("Solde Escompte Commercial: {} (Plafond: {} - Encours: {})", solde, plafond, encours);
        return solde;
    }

    // ========== PRÉFINANCEMENT (ID: 11) ==========

    /**
     * Préfinancement : encours = Σ REMBOURSEMENT.montant agrégé sur tous les marchés liés.
     * Si aucun marché n'est associé, retourne null.
     */
    private BigDecimal calculateEncoursPrefinancement(Long ligneId, List<Marche> marches, Long natureId) {
        if (marches.isEmpty()) {
            log.warn("Marchés absents pour la nature 'Préfinancement' (ligne ID: {}) - Encours set to null", ligneId);
            return null;
        }

        BigDecimal encours = marches.stream()
                .flatMap(m -> {
                    List<Prefinancement> prefs = m.getPrefinancements();
                    return prefs != null ? prefs.stream() : java.util.stream.Stream.empty();
                })
                .filter(p -> p.getType() == Type.REMBOURSEMENT)
                .map(Prefinancement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Encours Préfinancement calculé (ligne ID {}): {}", ligneId, encours);
        return encours;
    }

    /**
     * Préfinancement : solde = plafond - encours
     */
    private BigDecimal calculateSoldePrefinancement(BigDecimal plafond, BigDecimal encours) {
        BigDecimal solde = plafond.subtract(encours);
        log.debug("Solde Préfinancement: {} (Plafond: {} - Encours: {})", solde, plafond, encours);
        return solde;
    }

    // ========== DROIT CONSTATÉ (ID: 12) ==========

    /**
     * Droit Constaté : encours = plafond - Σ DEBLOCAGE + Σ REMBOURSEMENT agrégé sur tous les marchés.
     */
    private BigDecimal calculateEncoursDroitConstate(Long ligneId, List<Marche> marches, BigDecimal plafond, Long natureId) {
        if (marches.isEmpty()) {
            log.warn("Marchés absents pour la nature 'Droit Constaté' (ligne ID: {}) - Encours set to null", ligneId);
            return null;
        }

        List<DroitConstate> allDc = marches.stream()
                .flatMap(m -> {
                    List<DroitConstate> dcs = m.getDroitConstates();
                    return dcs != null ? dcs.stream() : java.util.stream.Stream.empty();
                })
                .toList();

        if (allDc.isEmpty()) {
            log.debug("No droits constatés found, encours = plafond ({})", plafond);
            return plafond;
        }

        BigDecimal totalDeblocages = allDc.stream()
                .filter(dc -> dc.getType() == Type.DEBLOCAGE)
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemboursements = allDc.stream()
                .filter(dc -> dc.getType() == Type.REMBOURSEMENT)
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal encours = plafond.subtract(totalDeblocages).add(totalRemboursements);
        log.debug("Encours Droit Constaté: {} (Plafond: {} - Déblocages: {} + Remboursements: {})",
                encours, plafond, totalDeblocages, totalRemboursements);
        return encours;
    }

    /**
     * Droit Constaté : solde = Σ DEBLOCAGE - Σ REMBOURSEMENT agrégé sur tous les marchés.
     */
    private BigDecimal calculateSoldeDroitConstate(Long ligneId, List<Marche> marches, Long natureId) {
        if (marches.isEmpty()) {
            log.warn("Marchés absents pour la nature 'Droit Constaté' (ligne ID: {}) - Solde set to null", ligneId);
            return null;
        }

        List<DroitConstate> allDc = marches.stream()
                .flatMap(m -> {
                    List<DroitConstate> dcs = m.getDroitConstates();
                    return dcs != null ? dcs.stream() : java.util.stream.Stream.empty();
                })
                .toList();

        if (allDc.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDeblocages = allDc.stream()
                .filter(dc -> dc.getType() == Type.DEBLOCAGE)
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemboursements = allDc.stream()
                .filter(dc -> dc.getType() == Type.REMBOURSEMENT)
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal solde = totalDeblocages.subtract(totalRemboursements);
        log.debug("Solde Droit Constaté: {} (Déblocages: {} - Remboursements: {})",
                solde, totalDeblocages, totalRemboursements);
        return solde;
    }

    // ========== FACTORING (ID: 13) ==========

    /**
     * Factoring : encours = plafond - Σ DEBLOCAGE + Σ REMBOURSEMENT agrégé sur tous les marchés.
     */
    private BigDecimal calculateEncoursFactoring(Long ligneId, List<Marche> marches, BigDecimal plafond, Long natureId) {
        if (marches.isEmpty()) {
            log.warn("Marchés absents pour la nature 'Factoring' (ligne ID: {}) - Encours set to null", ligneId);
            return null;
        }

        List<Factoring> allFactoring = marches.stream()
                .flatMap(m -> {
                    List<Factoring> fs = m.getFactorings();
                    return fs != null ? fs.stream() : java.util.stream.Stream.empty();
                })
                .toList();

        if (allFactoring.isEmpty()) {
            log.debug("No factorings found, encours = plafond ({})", plafond);
            return plafond;
        }

        BigDecimal totalDeblocages = allFactoring.stream()
                .filter(f -> f.getType() == Type.DEBLOCAGE)
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemboursements = allFactoring.stream()
                .filter(f -> f.getType() == Type.REMBOURSEMENT)
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal encours = plafond.subtract(totalDeblocages).add(totalRemboursements);
        log.debug("Encours Factoring: {} (Plafond: {} - Déblocages: {} + Remboursements: {})",
                encours, plafond, totalDeblocages, totalRemboursements);
        return encours;
    }

    /**
     * Factoring : solde = Σ DEBLOCAGE - Σ REMBOURSEMENT agrégé sur tous les marchés.
     */
    private BigDecimal calculateSoldeFactoring(Long ligneId, List<Marche> marches, Long natureId) {
        if (marches.isEmpty()) {
            log.warn("Marchés absents pour la nature 'Factoring' (ligne ID: {}) - Solde set to null", ligneId);
            return null;
        }

        List<Factoring> allFactoring = marches.stream()
                .flatMap(m -> {
                    List<Factoring> fs = m.getFactorings();
                    return fs != null ? fs.stream() : java.util.stream.Stream.empty();
                })
                .toList();

        if (allFactoring.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDeblocages = allFactoring.stream()
                .filter(f -> f.getType() == Type.DEBLOCAGE)
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemboursements = allFactoring.stream()
                .filter(f -> f.getType() == Type.REMBOURSEMENT)
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal solde = totalDeblocages.subtract(totalRemboursements);
        log.debug("Solde Factoring: {} (Déblocages: {} - Remboursements: {})",
                solde, totalDeblocages, totalRemboursements);
        return solde;
    }
}
