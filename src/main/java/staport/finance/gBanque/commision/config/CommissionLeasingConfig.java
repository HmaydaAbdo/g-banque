package staport.finance.gBanque.commision.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration des commissions de leasing par banque.
 * Les valeurs sont chargées depuis application.yml : commission.leasing
 * Structure YAML attendue :
 * commission:
 *   leasing:
 *     CIH: 11
 *     BMCI: 16.5
 *     SGMB: 11
 */
@Configuration
@ConfigurationProperties(prefix = "commission")
public class CommissionLeasingConfig {

    /**
     * Map des commissions de leasing par nom de banque
     * Clé: nom de la banque (CIH, BMCI, SGMB)
     * Valeur: montant de la commission en DH
     */
    private Map<String, BigDecimal> leasing = new HashMap<>();

    /**
     * Retourne toutes les commissions de leasing configurées
     */
    public Map<String, BigDecimal> getLeasing() {
        return leasing;
    }

    /**
     * Définit les commissions de leasing (utilisé par Spring Boot)
     */
    public void setLeasing(Map<String, BigDecimal> leasing) {
        this.leasing = leasing;
    }

    /**
     * Récupère la commission pour une banque spécifique
     *
     * @param nomBanque Le nom de la banque (CIH, BMCI, SGMB)
     * @return Le montant de commission en DH, ou null si non trouvé
     */
    public BigDecimal getCommission(String nomBanque) {
        return leasing.get(nomBanque);
    }

    /**
     * Récupère la commission pour une banque, avec une valeur par défaut
     *
     * @param nomBanque Le nom de la banque
     * @param defaultValue Valeur par défaut si la banque n'est pas trouvée
     * @return Le montant de commission en DH
     */
    public BigDecimal getCommissionOrDefault(String nomBanque, BigDecimal defaultValue) {
        return leasing.getOrDefault(nomBanque, defaultValue);
    }

    /**
     * Vérifie si une commission existe pour une banque
     *
     * @param nomBanque Le nom de la banque
     * @return true si la commission existe
     */
    public boolean hasCommission(String nomBanque) {
        return leasing.containsKey(nomBanque);
    }

    /**
     * Retourne toutes les banques configurées
     */
    public java.util.Set<String> getBanquesConfigurees() {
        return leasing.keySet();
    }

    /**
     * Retourne le Map complet des commissions (alias pour compatibilité)
     */
    public Map<String, BigDecimal> getCommissions() {
        return leasing;
    }
}