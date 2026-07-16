package staport.finance.gBanque.fraisBancaires.dtos;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran d'encours des frais bancaires d'une banque (banqueId fourni
 * séparément par le path variable — mirroring {@code EncoursImpayeeSearchDto}). Le statut n'est
 * pas un critère ici : l'encours force toujours {@code Status.PREVU}.
 */
public record EncoursFraisBancaireSearchDto(

        String libelle,
        BigDecimal montant

) {}
