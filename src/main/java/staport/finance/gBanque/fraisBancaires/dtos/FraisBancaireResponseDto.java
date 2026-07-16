package staport.finance.gBanque.fraisBancaires.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour un frais bancaire.
 *
 * <p>Remplace {@code FraisBancaireResposneDto} (nom mal orthographié) — renommage validé par
 * HMAYDA lors du sweep de conformité Frais bancaires.
 */
public record FraisBancaireResponseDto(

        Long id,
        String libelle,
        String observation,
        BigDecimal montant,
        Long banqueId,
        String banqueNom,
        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperation,
        Status status

) {}
