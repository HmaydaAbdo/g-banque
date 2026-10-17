import {Decimal} from "decimal.js";
import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";
import {SensOperation} from "../../../../../transactions/dtos/SensOperation";


export interface RecaputilatifItem {
  id:number;
  dateEmission: string;
  /** Vide pour les sources sans échéance (chèque, virement, commission…). */
  dateEcheance: string | null;
  /**
   * Date qui range l'opération dans le temps et pilote l'horizon : émission pour la plupart des sources, échéance pour
   * le leasing, le CMT et les LCN. Toujours renseignée par le backend.
   */
  dateReference: string;
  /** Peut être vide sur d'anciennes lignes. */
  libelle: string | null;
  /** Numéro de pièce porté par la source ; `null` quand la source n'en a pas. */
  numero: string | null;
  sens: SensOperation;
  debit: Decimal;
  credit: Decimal;
  observations: string;
  source:TransactionSource;
  /**
   * Clé d'identité stable `${source}:${id}` — `id` seul n'est pas unique tous sources confondues
   * (chaque source a sa propre séquence en base, deux sources différentes peuvent donc partager
   * le même id). Calculée côté client juste après réception de la réponse (voir
   * `loadRecapitulatif()`), jamais renvoyée par le backend.
   *
   * Sert de `dataKey` à `<p-table>` : sans un `dataKey` explicite, PrimeNG retombe sur une
   * comparaison par égalité profonde des objets pour déterminer si une ligne est sélectionnée —
   * fragile dès qu'une page est rechargée (tri, pagination, filtre) et que les objets sont
   * recréés. Avec ce `dataKey`, PrimeNG (et `TableSelectionState`, qui utilise la même clé) se
   * basent tous les deux sur la même chaîne stable, éliminant toute ambiguïté de correspondance
   * entre l'état de sélection interne et l'affichage des cases à cocher.
   */
  compositeKey: string;
}
