package staport.finance.gBanque.LigneFonctionnelle.services;

import java.math.BigDecimal;

public interface ILigneFonctionnelleService {
    BigDecimal getPlafondFCInternal(Long banqueId, Long fcNatureId);
}
