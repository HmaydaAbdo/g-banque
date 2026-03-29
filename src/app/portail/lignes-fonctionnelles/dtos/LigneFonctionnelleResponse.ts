import { SousLigneResponseDto } from "./SousLigneResponseDto";

/**
 * DTO de réponse pour une ligne fonctionnelle.
 *
 * Correspond au DTO Java : staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleResponse
 *
 * ✅ Inclut maintenant :
 * - Informations marché (marcheId, numeroMarche, nomMarche)
 * - Calculs financiers (encours, solde)
 * - Gestion hiérarchique (sous-lignes)
 */
export interface LigneFonctionnelleResponse {
  // ========== IDENTIFICATION ==========

  /**
   * ID unique de la ligne fonctionnelle
   */
  id: number;

  // ========== BANQUE ==========

  /**
   * ID de la banque associée
   */
  banqueId: number;

  /**
   * Nom de la banque
   */
  banqueNom: string;

  // ========== NATURE ==========

  /**
   * ID de la nature de ligne fonctionnelle
   */
  natureLigneFonctionnelleId: number;

  /**
   * Label de la nature (ex: "Escompte Commercial", "Préfinancement")
   */
  natureLigneFonctionnelleLabel: string;

  /**
   * Description détaillée de la nature
   */
  natureLigneFonctionnelleDescription: string;

  // ========== MARCHÉ (Optionnel) ==========

  /**
   * ✅ NOUVEAU : ID du marché associé
   * - null = ligne générale (non associée à un marché)
   * - number = ligne spécifique à ce marché
   */
  marcheId?: number | null;

  /**
   * ✅ NOUVEAU : Numéro du marché
   */
  numeroMarche?: string | null;

  /**
   * ✅ NOUVEAU : Nom du marché
   */
  nomMarche?: string | null;

  // ========== MONTANTS & CALCULS ==========

  /**
   * Plafond autorisé
   */
  plafond: number;

  /**
   * Encours calculé (selon la nature)
   * - null si non applicable à cette nature
   */
  encours?: number | null;

  /**
   * Solde disponible (selon la nature)
   * - null si non applicable à cette nature
   */
  solde?: number | null;

  /**
   * Taux en pourcentage (0-100)
   */
  taux: number;

  // ========== DATES ==========

  /**
   * Date de début de validité
   * Format: ISO 8601 string from backend, parsed to Date
   */
  dateDepart: string;

  /**
   * Date de fin de validité
   */
  dateFin: string;

  /**
   * Date de création de l'enregistrement
   */
  dateCreation: string;

  /**
   * Date de dernière modification
   */
  dateModification: string;

  // ========== ÉTAT ==========

  /**
   * Ligne active ou désactivée
   */
  actif: boolean;

  /**
   * Indique si la ligne est expirée (dateFin < now)
   */
  expired: boolean;

  // ========== HIÉRARCHIE (Sous-lignes) ==========

  /**
   * ID de la ligne mère (si c'est une sous-ligne)
   * - null si ligne indépendante ou mère
   */
  ligneMereId?: number | null;

  /**
   * Liste des sous-lignes (si ligne mère)
   * - Vide [] si pas de sous-lignes
   */
  sousLignes: SousLigneResponseDto[];

  /**
   * Indique si cette ligne est une ligne mère
   */
  estLigneMere: boolean;

  /**
   * Indique si cette ligne est une sous-ligne
   */
  estSousLigne: boolean;
}
