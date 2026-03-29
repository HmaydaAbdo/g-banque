package staport.finance.gBanque.LigneFonctionnelle.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ILigneFonctionnelleService {
    BigDecimal getPlafondFCInternal(Long banqueId, Long fcNatureId);

    Map<Long, BigDecimal> getActivePlafondsForBanks(List<Long> bankIds, String fcCode);
}
