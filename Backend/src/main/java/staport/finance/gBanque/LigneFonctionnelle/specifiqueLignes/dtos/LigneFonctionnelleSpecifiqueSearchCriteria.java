package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos;

/**
 * Critères de recherche/filtrage des lignes fonctionnelles spécifiques. Tous les champs sont
 * facultatifs (liés en ET logique quand plusieurs sont renseignés).
 */
public record LigneFonctionnelleSpecifiqueSearchCriteria(
        Long banqueId,
        Long natureLigneFonctionnelleId,
        Integer yearDepart,
        Boolean actif,
        Boolean isExpired,
        Long marcheId
) {
    /** Retourne une copie de ce critère avec {@code banqueId} remplacé. */
    public LigneFonctionnelleSpecifiqueSearchCriteria withBanqueId(Long banqueId) {
        return new LigneFonctionnelleSpecifiqueSearchCriteria(banqueId, natureLigneFonctionnelleId, yearDepart, actif, isExpired, marcheId);
    }
}
