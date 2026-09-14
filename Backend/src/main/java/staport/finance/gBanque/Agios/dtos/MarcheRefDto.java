package staport.finance.gBanque.Agios.dtos;

/**
 * Référence légère vers un Marché (id, numéro, nom) — utilisée pour peupler le filtre
 * marché de l'écran de détail du calcul des agios, sans exposer l'entité complète.
 * Ne liste que les marchés effectivement présents dans les enregistrements journaliers
 * de la banque/type/période consultés (pas le référentiel marché complet).
 */
public record MarcheRefDto(Long id, String numero, String nom) {
}
