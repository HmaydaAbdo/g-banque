package staport.finance.gBanque.commision.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration des commissions de leasing avec gestion des règles spécifiques par banque/organisme.
 *
 * Exemple de configuration YAML :
 * commission:
 *   leasing:
 *     default-commissions:
 *       CIH: 11
 *       SGMB: 11
 *     organisme-rules:
 *       BMCI:
 *         organisme-name: "BMCI Leasing"
 *         commission-if-match: 16.5
 *         commission-if-not-match: 55
 */
@Component
@ConfigurationProperties(prefix = "commission.leasing")
public class CommissionLeasingConfig {

    private static final Logger log = LoggerFactory.getLogger(CommissionLeasingConfig.class);

    private Map<String, BigDecimal> defaultCommissions = new HashMap<>();
    private Map<String, OrganismeRule> organismeRules = new HashMap<>();

    // ========== GETTERS ET SETTERS OBLIGATOIRES ==========

    public Map<String, BigDecimal> getDefaultCommissions() {
        return defaultCommissions;
    }

    public void setDefaultCommissions(Map<String, BigDecimal> defaultCommissions) {
        this.defaultCommissions = defaultCommissions;
    }

    public Map<String, OrganismeRule> getOrganismeRules() {
        return organismeRules;
    }

    public void setOrganismeRules(Map<String, OrganismeRule> organismeRules) {
        this.organismeRules = organismeRules;
    }

    /**
     * Récupère le montant de la commission en fonction de la banque et de l'organisme.
     * Logique de résolution :
     * 1. Si une règle conditionnelle existe pour la banque (organismeRules)
     *    - Compare le nom de l'organisme avec celui de la règle
     *    - Retourne commissionIfMatch si correspondance
     *    - Retourne commissionIfNotMatch sinon
     * 2. Sinon, cherche dans les commissions par défaut (defaultCommissions)
     *
     * @param nomBanque Nom de la banque de prélèvement
     * @param nomOrganisme Nom de l'organisme de leasing
     * @return Le montant de la commission, ou null si aucune commission n'est configurée
     */
    /** Normalise un nom de banque : trim + suppression des espaces multiples + lowercase. */
    private String normalize(String name) {
        if (name == null) return null;
        return name.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    /** Cherche une clé dans une Map de façon insensible à la casse et aux espaces. */
    private <V> V findIgnoreCase(Map<String, V> map, String key) {
        if (key == null) return null;
        String normalizedKey = normalize(key);
        return map.entrySet().stream()
                .filter(e -> normalize(e.getKey()).equals(normalizedKey))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public BigDecimal getCommission(String nomBanque, String nomOrganisme) {
        if (nomBanque == null) {
            log.warn("Tentative de récupération de commission avec nomBanque null");
            return null;
        }

        log.debug("Recherche de commission pour Banque: {}, Organisme: {}", nomBanque, nomOrganisme);

        // Vérifier si une règle spécifique existe pour cette banque (insensible à la casse)
        OrganismeRule rule = findIgnoreCase(organismeRules, nomBanque);
        if (rule != null) {
            log.debug("Règle conditionnelle trouvée pour la banque: {}", nomBanque);

            // Si l'organisme correspond à la règle spécifique
            if (nomOrganisme != null && nomOrganisme.equalsIgnoreCase(rule.getOrganismeName())) {
                log.debug("Organisme '{}' correspond à la règle. Commission: {}",
                        nomOrganisme, rule.getCommissionIfMatch());
                return rule.getCommissionIfMatch();
            }

            // Sinon, utiliser la commission par défaut de la règle
            log.debug("Organisme '{}' ne correspond pas à '{}'. Commission: {}",
                    nomOrganisme, rule.getOrganismeName(), rule.getCommissionIfNotMatch());
            return rule.getCommissionIfNotMatch();
        }

        // Pas de règle spécifique, utiliser la commission par défaut (insensible à la casse)
        BigDecimal commission = findIgnoreCase(defaultCommissions, nomBanque);
        if (commission != null) {
            log.debug("Commission par défaut trouvée pour {}: {}", nomBanque, commission);
        } else {
            log.debug("Aucune commission configurée pour la banque: {}", nomBanque);
        }

        return commission;
    }

    /**
     * Vérifie si une commission est configurée pour une banque donnée (insensible à la casse)
     *
     * @param nomBanque Nom de la banque
     * @return true si une commission existe (règle ou défaut), false sinon
     */
    public boolean hasCommission(String nomBanque) {
        boolean hasDefault = findIgnoreCase(defaultCommissions, nomBanque) != null;
        boolean hasRule    = findIgnoreCase(organismeRules, nomBanque) != null;

        log.debug("Vérification commission pour {}: hasDefault={}, hasRule={}",
                nomBanque, hasDefault, hasRule);

        //LOG AJOUTÉ POUR DEBUG
        log.debug("  → defaultCommissions keys: {}", defaultCommissions.keySet());
        log.debug("  → organismeRules keys: {}", organismeRules.keySet());

        return hasDefault || hasRule;
    }

    /**
     * Classe interne représentant une règle conditionnelle basée sur l'organisme
     */
    public static class OrganismeRule {
        private String organismeName;           // Nom de l'organisme à matcher
        private BigDecimal commissionIfMatch;    // Commission si l'organisme correspond
        private BigDecimal commissionIfNotMatch; // Commission si l'organisme ne correspond pas

        public String getOrganismeName() {
            return organismeName;
        }

        public void setOrganismeName(String organismeName) {
            this.organismeName = organismeName;
        }

        public BigDecimal getCommissionIfMatch() {
            return commissionIfMatch;
        }

        public void setCommissionIfMatch(BigDecimal commissionIfMatch) {
            this.commissionIfMatch = commissionIfMatch;
        }

        public BigDecimal getCommissionIfNotMatch() {
            return commissionIfNotMatch;
        }

        public void setCommissionIfNotMatch(BigDecimal commissionIfNotMatch) {
            this.commissionIfNotMatch = commissionIfNotMatch;
        }

        @Override
        public String toString() {
            return "OrganismeRule{" +
                    "organismeName='" + organismeName + '\'' +
                    ", commissionIfMatch=" + commissionIfMatch +
                    ", commissionIfNotMatch=" + commissionIfNotMatch +
                    '}';
        }
    }
}