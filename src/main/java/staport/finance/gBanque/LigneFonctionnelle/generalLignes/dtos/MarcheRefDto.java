package staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos;

/**
 * Référence légère vers un Marché (id, numéro, nom) — utilisée dans {@link LigneFonctionnelleResponse}
 * pour lister les marchés associés à une ligne fonctionnelle sans exposer l'entité complète.
 */
public record MarcheRefDto(Long id, String numero, String nom) {
}
