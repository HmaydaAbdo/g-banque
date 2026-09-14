package staport.finance.gBanque.Agios.service.calculateurs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Résolution de la ligne Facilité de Caisse via {@link LigneFonctionnelleReader} — jamais via
 * {@code LigneFonctionnelleRepository} directement (encapsulation des domaines, architecture.md).
 * Le code système immuable {@link NatureLigneFonctionnelleService#FC_CODE} est utilisé au lieu
 * d'une recherche par libellé (LIKE), alignée sur le pattern déjà établi pour PREFI/FACTORING/DC.
 */
@Component
public class FcAgiosCalculateur implements AgiosCalculateur {

    private static final Logger log = LoggerFactory.getLogger(FcAgiosCalculateur.class);
    private static final BigDecimal DIVISEUR_360 = new BigDecimal("360");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final LigneFonctionnelleReader ligneFonctionnelleReader;

    /**
     * Cache des lignes FC par banque — évite de requêter la base N fois
     * pour la même banque pendant un recalcul de 90 jours.
     */
    private final Map<Long, LigneFonctionnelle> cacheLignesFC = new ConcurrentHashMap<>();

    public FcAgiosCalculateur(LigneFonctionnelleReader ligneFonctionnelleReader) {
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
    }

    @Override
    public TypeAgios getType() {
        return TypeAgios.FACILITE_CAISSE;
    }

    @Override
    public boolean estEligible(Banque banque) {
        LigneFonctionnelle ligne = trouverLigneFC(banque.getId());
        if (ligne == null) {
            log.info("FC — {} : aucune ligne Facilité de Caisse active → banque ignorée", banque.getNom());
            return false;
        }
        log.info("FC — {} : ligne id={} taux={}% → éligible", banque.getNom(), ligne.getId(), ligne.getTaux());
        return true;
    }

    @Override
    public ResultatJournalier calculerJour(Banque banque, LocalDate jour, BigDecimal soldeJournalier) {
        // Pas de découvert → pas d'agios
        if (soldeJournalier.compareTo(BigDecimal.ZERO) >= 0) {
            log.debug("FC — {} — {} : solde={} ≥ 0, pas de découvert", banque.getNom(), jour, soldeJournalier);
            return ResultatJournalier.ZERO;
        }

        LigneFonctionnelle ligneFC = trouverLigneFC(banque.getId());

        if (ligneFC == null) {
            return ResultatJournalier.ZERO;
        }

        BigDecimal decouvert = soldeJournalier.abs();
        BigDecimal taux = ligneFC.getTaux();

        // Agios HT = (découvert × taux / 100) / 360
        BigDecimal agiosHT = decouvert
                .multiply(taux)
                .divide(BigDecimal.valueOf(100).multiply(DIVISEUR_360), 8, ROUNDING)
                .setScale(SCALE, ROUNDING);

        String metadata = "ligneFC_id=" + ligneFC.getId();

        return new ResultatJournalier(decouvert, taux, agiosHT, metadata);
    }

    /** Vide le cache — peut être appelé entre les banques si nécessaire */
    public void viderCache() {
        cacheLignesFC.clear();
    }

    private LigneFonctionnelle trouverLigneFC(Long banqueId) {
        return cacheLignesFC.computeIfAbsent(banqueId, id -> {
            LigneFonctionnelle ligne = ligneFonctionnelleReader
                    .findByBanqueIdAndNatureLigneFonctionnelleCodeAndActifTrue(id, NatureLigneFonctionnelleService.FC_CODE)
                    .orElse(null);

            if (ligne == null) {
                log.debug("Banque {} : aucune ligne FC active", id);
            }
            return ligne;
        });
    }
}
