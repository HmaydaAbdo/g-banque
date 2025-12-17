package staport.finance.gBanque.recapitulatif.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * DTO contenant le récapitulatif global d'une banque avec :
 * - Informations de la banque (ID, nom, solde actuel, plafond FC)
 * - Solde fin de mois calculé
 * - Reste disponible
 * - Liste des encaissements (crédits)
 * - Liste des décaissements (débits)
 */
public class RecaputilatifGlobal {

    private Long banqueId;
    private String banqueName;
    private BigDecimal soldeActuel;
    private BigDecimal plafondFC;
    private BigDecimal totalEncaissements;
    private BigDecimal totalDecaissements;
    private BigDecimal soldefin;
    private BigDecimal reste;

    private List<RecaputilatifItem> encaissement;
    private List<RecaputilatifItem> decaissement;


    public RecaputilatifGlobal(Long banqueId, String banqueName, BigDecimal soldeActuel,
                               BigDecimal plafondFC, List<RecaputilatifItem> encaissement,
                               List<RecaputilatifItem> decaissement) {
        this.banqueId = banqueId;
        this.banqueName = banqueName;
        this.soldeActuel = soldeActuel != null ? soldeActuel : BigDecimal.ZERO;
        this.plafondFC = plafondFC != null ? plafondFC : BigDecimal.ZERO;
        this.encaissement = encaissement;
        this.decaissement = decaissement;

        // Calcul automatique des totaux et des soldes
        this.totalEncaissements = calculerTotalEncaissements();
        this.totalDecaissements = calculerTotalDecaissements();
        this.soldefin = calculerSoldeFinDeMois();
        this.reste = calculerReste();
    }

    /**
     * Calcule le total des encaissements (somme des crédits)
     */
    private BigDecimal calculerTotalEncaissements() {
        if (encaissement == null || encaissement.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return encaissement.stream()
                .map(RecaputilatifItem::getCredit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcule le total des décaissements (somme des débits)
     */
    private BigDecimal calculerTotalDecaissements() {
        if (decaissement == null || decaissement.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return decaissement.stream()
                .map(RecaputilatifItem::getDebit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcule le solde fin de mois :
     * Solde fin = Solde actuel + Total encaissements - Total décaissements
     */
    private BigDecimal calculerSoldeFinDeMois() {
        return soldeActuel.add(totalEncaissements).subtract(totalDecaissements);
    }

    /**
     * Calcule le reste disponible :
     * Reste = Plafond FC - Solde fin de mois
     */
    private BigDecimal calculerReste() {
        return plafondFC.subtract(soldefin);
    }

    /**
     * Recalcule tous les montants (utile après modification des listes)
     */
    public void recalculer() {
        this.totalEncaissements = calculerTotalEncaissements();
        this.totalDecaissements = calculerTotalDecaissements();
        this.soldefin = calculerSoldeFinDeMois();
        this.reste = calculerReste();
    }

    // Getters and Setters
    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getBanqueName() {
        return banqueName;
    }

    public void setBanqueName(String banqueName) {
        this.banqueName = banqueName;
    }

    public BigDecimal getSoldeActuel() {
        return soldeActuel;
    }

    public void setSoldeActuel(BigDecimal soldeActuel) {
        this.soldeActuel = soldeActuel;
        recalculer();
    }

    public BigDecimal getSoldefin() {
        return soldefin;
    }

    public BigDecimal getPlafondFC() {
        return plafondFC;
    }

    public void setPlafondFC(BigDecimal plafondFC) {
        this.plafondFC = plafondFC;
        recalculer();
    }

    public BigDecimal getReste() {
        return reste;
    }

    public BigDecimal getTotalEncaissements() {
        return totalEncaissements;
    }

    public BigDecimal getTotalDecaissements() {
        return totalDecaissements;
    }

    public List<RecaputilatifItem> getEncaissement() {
        return encaissement;
    }

    public void setEncaissement(List<RecaputilatifItem> encaissement) {
        this.encaissement = encaissement;
        recalculer();
    }

    public List<RecaputilatifItem> getDecaissement() {
        return decaissement;
    }

    public void setDecaissement(List<RecaputilatifItem> decaissement) {
        this.decaissement = decaissement;
        recalculer();
    }
}