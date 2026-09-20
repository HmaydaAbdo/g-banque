/**
 * Gère la sélection multi-lignes d'un `p-table` PrimeNG en mode `[lazy]="true"`
 * (pagination côté serveur), de façon indépendante du rechargement de page.
 *
 * Problème résolu : le binding natif `[(selection)]` de PrimeNG ne garantit pas
 * la persistance de la sélection entre deux pages chargées en lazy — dès que
 * `value` change (nouvelle page), la sélection peut être perdue de façon
 * silencieuse. Cette classe reprend le contrôle en conservant la sélection
 * dans une Map indexée par clé, décorrélée de la page actuellement affichée.
 *
 * Usage dans un composant (clé = id numérique, cas le plus courant) :
 * ```ts
 * selectionState = new TableSelectionState<VirementResponseDto>(v => v.id);
 *
 * get pageSelection() { return this.selectionState.getPageSelection(this.virements); }
 * onSelectionChange(sel: VirementResponseDto[]) { this.selectionState.onPageSelectionChange(this.virements, sel); }
 * ```
 *
 * Usage avec une clé composite (ex. écran Récapitulatif, où `entityId` seul n'est
 * unique que combiné à `source` — plusieurs sources peuvent porter le même id) :
 * ```ts
 * selectionState = new TableSelectionState<RecaputilatifItem, string>(item => `${item.source}:${item.id}`);
 * ```
 *
 * Usage dans le template :
 * ```html
 * <p-table [value]="virements" [selection]="pageSelection" (selectionChange)="onSelectionChange($event)" dataKey="id">
 * ```
 *
 * IMPORTANT — stabilité de référence :
 * `getPageSelection()` est mémorisé. Tant que le tableau `pageItems` passé en
 * argument garde la même référence (i.e. la page n'a pas été rechargée) et que
 * la sélection sous-jacente n'a pas réellement changé, la méthode renvoie
 * exactement le même tableau (même référence) à chaque appel. C'est essentiel
 * car `getPageSelection()` est généralement appelé depuis un getter de
 * composant évalué à chaque cycle de détection de changements Angular : sans
 * cette mémorisation, un nouveau tableau serait recréé à chaque cycle, ce que
 * PrimeNG interprète comme "la sélection a changé" en continu — d'où un
 * recalcul/re-rendu en boucle de la table, pouvant geler l'écran rien qu'en
 * changeant de page, même sans aucune case cochée.
 *
 * @template T Type des éléments de la table.
 * @template K Type de la clé d'identité (`number` par défaut — id simple ; `string` pour une
 *              clé composite, ex. `` `${source}:${id}` ``).
 */
export class TableSelectionState<T, K = number> {
  private readonly selectionMap = new Map<K, T>();

  /** Incrémenté à chaque mutation réelle de la sélection (voir invalidation du cache ci-dessous). */
  private revision = 0;

  /** Cache de mémorisation pour `getPageSelection()`. */
  private cachedPageItems: T[] | null = null;
  private cachedRevision = -1;
  private cachedResult: T[] = [];

  constructor(private readonly getId: (item: T) => K) {}

  /** Tous les éléments actuellement sélectionnés, toutes pages confondues. */
  get selectedItems(): T[] {
    return Array.from(this.selectionMap.values());
  }

  /** Les clés des éléments sélectionnés, toutes pages confondues. */
  get selectedIds(): K[] {
    return Array.from(this.selectionMap.keys());
  }

  /** Nombre total d'éléments sélectionnés, toutes pages confondues. */
  get count(): number {
    return this.selectionMap.size;
  }

  get isEmpty(): boolean {
    return this.selectionMap.size === 0;
  }

  /**
   * Sous-ensemble des éléments de la page actuellement affichée qui sont
   * sélectionnés — à binder sur `[selection]`.
   *
   * Mémorisé : renvoie la même référence de tableau tant que `pageItems` (même
   * référence) et l'état de sélection n'ont pas changé depuis le dernier appel.
   */
  getPageSelection(pageItems: T[]): T[] {
    if (pageItems === this.cachedPageItems && this.revision === this.cachedRevision) {
      return this.cachedResult;
    }
    this.cachedPageItems = pageItems;
    this.cachedRevision = this.revision;
    this.cachedResult = pageItems.filter((item) => this.selectionMap.has(this.getId(item)));
    return this.cachedResult;
  }

  /**
   * À appeler depuis `(selectionChange)`. Ne modifie que l'état des éléments
   * présents sur la page actuelle ; les sélections des autres pages restent
   * intactes.
   */
  onPageSelectionChange(pageItems: T[], newPageSelection: T[]): void {
    const newlySelectedIds = new Set(newPageSelection.map((item) => this.getId(item)));
    for (const item of pageItems) {
      const id = this.getId(item);
      if (newlySelectedIds.has(id)) {
        this.selectionMap.set(id, item);
      } else {
        this.selectionMap.delete(id);
      }
    }
    this.revision++;
  }

  /** Vide entièrement la sélection (toutes pages confondues). */
  clear(): void {
    this.selectionMap.clear();
    this.revision++;
  }

  /** Somme d'un champ numérique (ex. montant) sur tous les éléments sélectionnés. */
  sumBy(getValue: (item: T) => number | null | undefined): number {
    return this.selectedItems.reduce((sum, item) => sum + (Number(getValue(item)) || 0), 0);
  }
}
