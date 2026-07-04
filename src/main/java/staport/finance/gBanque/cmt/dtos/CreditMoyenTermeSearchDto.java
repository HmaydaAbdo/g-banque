package staport.finance.gBanque.cmt.dtos;

/**
 * DTO utilisé pour la recherche et le filtrage de crédits moyen terme.
 * Tous les champs sont optionnels pour permettre des requêtes flexibles.
 */
public record CreditMoyenTermeSearchDto(
        Long banqueId,
        String designation,
        Boolean completed
) {
}
