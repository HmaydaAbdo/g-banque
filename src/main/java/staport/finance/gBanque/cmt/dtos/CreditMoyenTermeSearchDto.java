package staport.finance.gBanque.cmt.dtos;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO utilisé pour la recherche et le filtrage de crédits moyen terme.
 * Tous les champs sont optionnels pour permettre des requêtes flexibles.
 */
public record CreditMoyenTermeSearchDto(
        Long banqueId,
        String designation,
        Boolean completed,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebutMin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebutMax
) {
}
