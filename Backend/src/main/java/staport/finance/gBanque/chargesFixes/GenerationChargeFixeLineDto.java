package staport.finance.gBanque.chargesFixes;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GenerationChargeFixeLineDto(
        Long configurationId,
        String banque,
        TypeChargeFixe type,
        String libelle,
        BigDecimal montant,
        LocalDate dateEmission,
        String statut,
        String motif
) {}
