package staport.finance.gBanque.LigneFonctionnelle.generalLignes.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ILigneFonctionnelleService {
    BigDecimal getPlafondFCInternal(Long banqueId, Long fcNatureId);

    Map<Long, BigDecimal> getActivePlafondsForBanks(List<Long> bankIds, String fcCode);

    /**
     * Indique si une banque possède au moins une ligne fonctionnelle active.
     * Utilisé comme guard de suppression dans {@code IBanqueService#deleteBanque}.
     */
    boolean hasActiveLignesForBanque(Long banqueId);
}
