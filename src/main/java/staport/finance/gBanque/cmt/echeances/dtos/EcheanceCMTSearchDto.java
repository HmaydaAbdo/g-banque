package staport.finance.gBanque.cmt.echeances.dtos;

import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;

/**
 * DTO utilisé pour la recherche et le filtrage des échéances de crédits à moyen terme.
 * Tous les champs sont optionnels pour permettre des requêtes flexibles.
 * {@code creditMoyenTermeId} est renseigné par le Service (pas par l'appelant HTTP).
 */
public record EcheanceCMTSearchDto(
        Long creditMoyenTermeId,
        Integer numeroEcheance,
        Integer month,
        Integer year,
        StatutEcheanceCMT statutPaiement
) {

    public EcheanceCMTSearchDto() {
        this(null, null, null, null, null);
    }

    /**
     * Retourne une copie de ce DTO avec l'ID du crédit renseigné.
     * Les DTOs étant immuables, cette méthode remplace l'ancien {@code setCreditMoyenTermeId}.
     */
    public EcheanceCMTSearchDto withCreditMoyenTermeId(Long creditMoyenTermeId) {
        return new EcheanceCMTSearchDto(creditMoyenTermeId, numeroEcheance, month, year, statutPaiement);
    }
}
