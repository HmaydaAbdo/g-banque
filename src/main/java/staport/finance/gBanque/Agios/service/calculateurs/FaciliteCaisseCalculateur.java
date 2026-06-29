package staport.finance.gBanque.Agios.service.calculateurs;

// @deprecated Remplacé par FcAgiosCalculateur — ce fichier sera supprimé.
// Conservé temporairement pour éviter les erreurs de compilation liées aux targets compilés.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.repositories.LigneFonctionnelleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** @deprecated Utiliser {@link FcAgiosCalculateur} à la place. */
@Deprecated
public class FaciliteCaisseCalculateur implements AgiosCalculateur {

    private static final Logger log = LoggerFactory.getLogger(FaciliteCaisseCalculateur.class);
    private static final String NATURE_FC_LABEL = "Facilité de Caisse";
    private static final BigDecimal DIVISEUR_360 = new BigDecimal("360");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;

    /**
     * Cache des lignes FC par banque — évite de requêter la base N fois
     * pour la même banque pendant un recalcul de 90 jours.
     */
    private final Map<Long, LigneFonctionnelle> cacheLignesFC = new ConcurrentHashMap<>();

    public FaciliteCaisseCalculateur(LigneFonctionnelleRepository ligneFonctionnelleRepository) {
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
    }

    @Override
    public TypeAgios getType() {
        return TypeAgios.FACILITE_CAISSE;
    }

    @Override
    public boolean estEligible(Banque banque) {
        return trouverLigneFC(banque.getId()) != null;
    }

    @Override
    public ResultatJournalier calculerJour(Banque banque, LocalDate jour, BigDecimal soldeJournalier) {
        // Pas de découvert → pas d'agios
        if (soldeJournalier.compareTo(BigDecimal.ZERO) >= 0) {
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
            Specification<LigneFonctionnelle> spec = (root, query, cb) ->
                    cb.and(
                            cb.equal(root.get("banque").get("id"), id),
                            cb.isTrue(root.get("actif")),
                            cb.like(cb.lower(root.get("natureLigneFonctionnelle").get("label")),
                                    "%" + NATURE_FC_LABEL.toLowerCase() + "%")
                    );

            Optional<LigneFonctionnelle> ligne = ligneFonctionnelleRepository.findOne(spec);
            if (ligne.isEmpty()) {
                log.debug("Banque {} : aucune ligne FC active", id);
                return null;
            }
            return ligne.get();
        });
    }
}