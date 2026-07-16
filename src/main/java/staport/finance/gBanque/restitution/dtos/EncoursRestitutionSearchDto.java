package staport.finance.gBanque.restitution.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran d'encours des restitutions d'une banque (banqueId fourni
 * séparément par le path variable — mirroring {@code EncoursImpotEtTaxeSearchDto}).
 */
public record EncoursRestitutionSearchDto(

        String libelle,
        BigDecimal montant,
        Status status

) {}
