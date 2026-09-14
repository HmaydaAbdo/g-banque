package staport.finance.gBanque.decomptes.dtos.request;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran d'encours des décomptes. Tous les champs sont optionnels.
 */
public record EncoursDecompteSearchDto(

        String marcheNumero,

        String marcheName,

        BigDecimal montant

) {}
