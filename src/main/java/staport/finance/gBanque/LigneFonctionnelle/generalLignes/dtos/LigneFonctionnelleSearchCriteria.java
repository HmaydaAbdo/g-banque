package staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos;

/**
 * Critères de recherche/filtrage des lignes fonctionnelles générales. Tous les champs sont
 * facultatifs (liés en ET logique quand plusieurs sont renseignés).
 */
public record LigneFonctionnelleSearchCriteria(
        Long banqueId,
        Long natureLigneFonctionnelleId,
        Long marcheId,
        Integer yearDepart,
        Boolean actif,
        Boolean isExpired
) {
    /** Retourne une copie de ce critère avec {@code banqueId} remplacé. */
    public LigneFonctionnelleSearchCriteria withBanqueId(Long banqueId) {
        return new LigneFonctionnelleSearchCriteria(banqueId, natureLigneFonctionnelleId, marcheId, yearDepart, actif, isExpired);
    }

    /** Retourne une copie de ce critère avec {@code marcheId} remplacé. */
    public LigneFonctionnelleSearchCriteria withMarcheId(Long marcheId) {
        return new LigneFonctionnelleSearchCriteria(banqueId, natureLigneFonctionnelleId, marcheId, yearDepart, actif, isExpired);
    }
}
