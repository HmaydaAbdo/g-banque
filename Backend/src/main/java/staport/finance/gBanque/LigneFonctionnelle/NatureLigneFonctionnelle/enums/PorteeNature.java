package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.enums;

/**
 * Définit les types de lignes fonctionnelles auxquels une NatureLigneFonctionnelle
 * peut être rattachée.
 *
 * GENERALE              → uniquement dans les lignes fonctionnelles générales
 * SPECIFIQUE            → uniquement dans les lignes fonctionnelles spécifiques
 * GENERALE_ET_SPECIFIQUE → dans les deux types de lignes
 */
public enum PorteeNature {

    GENERALE,
    SPECIFIQUE,
    GENERALE_ET_SPECIFIQUE
}
