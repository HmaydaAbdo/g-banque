import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { CautionResponseDto } from '../dtos/CautionResponseDto';

/**
 * Store RxJS pour le module Caution.
 *
 * Principe : BehaviorSubject privé → état mutable en interne uniquement.
 *            Observable public   → les composants lisent en lecture seule.
 *
 * Aucun composant ne peut appeler .next() directement — tout passe par
 * les méthodes publiques du store (SRP + encapsulation).
 */
@Injectable({ providedIn: 'root' })
export class CautionStore {

  // ── Liste paginée ─────────────────────────────────────────────────────────

  private readonly _cautions = new BehaviorSubject<CautionResponseDto[]>([]);
  readonly cautions$ = this._cautions.asObservable();

  // ── Pagination ────────────────────────────────────────────────────────────

  private readonly _totalElements = new BehaviorSubject<number>(0);
  readonly totalElements$ = this._totalElements.asObservable();

  // ── État de chargement ────────────────────────────────────────────────────

  private readonly _loading = new BehaviorSubject<boolean>(false);
  readonly loading$ = this._loading.asObservable();

  // ── Caution sélectionnée (pour le détail / formulaire) ───────────────────

  private readonly _selected = new BehaviorSubject<CautionResponseDto | null>(null);
  readonly selected$ = this._selected.asObservable();

  // ── Mutations ─────────────────────────────────────────────────────────────

  /** Remplace la liste complète (résultat d'une recherche paginée). */
  set(data: CautionResponseDto[]): void {
    this._cautions.next(data);
  }

  /** Met à jour le total d'éléments (pour le paginator). */
  setTotal(total: number): void {
    this._totalElements.next(total);
  }

  /** Active / désactive l'indicateur de chargement. */
  setLoading(loading: boolean): void {
    this._loading.next(loading);
  }

  /** Sélectionne la caution courante (pour edition ou détail). */
  selectCaution(caution: CautionResponseDto | null): void {
    this._selected.next(caution);
  }

  /**
   * Mise à jour locale immutable d'une caution après toggle ou modification.
   * Évite un rechargement complet de la liste.
   */
  update(updated: CautionResponseDto): void {
    const current = this._cautions.getValue();
    this._cautions.next(
      current.map(c => c.id === updated.id ? updated : c)
    );
  }

  /**
   * Suppression locale d'une caution après hard-delete confirmé.
   */
  remove(id: number): void {
    const current = this._cautions.getValue();
    this._cautions.next(current.filter(c => c.id !== id));
  }

  /** Réinitialise l'état complet du store. */
  clear(): void {
    this._cautions.next([]);
    this._totalElements.next(0);
    this._selected.next(null);
  }
}
