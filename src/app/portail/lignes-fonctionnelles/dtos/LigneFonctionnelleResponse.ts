import { SousLigneResponseDto } from "./SousLigneResponseDto";
import { MarcheRefDto } from "./MarcheRefDto";

/**
 * DTO de réponse pour une ligne fonctionnelle générale.
 * Correspond à : staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.LigneFonctionnelleResponse
 */
export interface LigneFonctionnelleResponse {

  // ========== IDENTIFICATION ==========
  id: number;

  // ========== BANQUE ==========
  banqueId: number;
  banqueNom: string;

  // ========== NATURE ==========
  natureLigneFonctionnelleId: number;
  natureLigneFonctionnelleLabel: string;
  natureLigneFonctionnelleDescription: string;

  // ========== MARCHÉS (ManyToMany — optionnel) ==========
  /**
   * Liste des marchés associés à cette ligne.
   * - [] = ligne générale non liée à un marché
   * - [m1, m2, ...] = ligne partagée entre plusieurs marchés
   */
  marches: MarcheRefDto[];

  // ========== MONTANTS & CALCULS ==========
  plafond: number;
  encours?: number | null;
  solde?: number | null;
  taux: number;

  // ========== DATES ==========
  dateDepart: string;
  dateFin: string;
  dateCreation: string;
  dateModification: string;

  // ========== ÉTAT ==========
  actif: boolean;
  expired: boolean;

  // ========== HIÉRARCHIE ==========
  ligneMereId?: number | null;
  sousLignes: SousLigneResponseDto[];
  estLigneMere: boolean;
  estSousLigne: boolean;
}
