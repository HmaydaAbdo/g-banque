import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

/**
 * Service utilitaire pour la navigation avec returnUrl.
 *
 * Stratégie : le returnUrl voyage AVEC la navigation via NavigationExtras.state
 * (stocké dans history.state), ce qui élimine tout risque de race-condition
 * ou de double-consommation lié à un état partagé dans un service singleton.
 */
@Injectable({ providedIn: 'root' })
export class NavigationStateService {

  constructor(private router: Router) {}

  /**
   * Navigue vers `targetUrl` en embarquant le returnUrl dans le NavigationExtras.state.
   */
  navigateWithReturnUrl(targetUrl: string | string[], returnUrl: string): Promise<boolean> {
    const commands = Array.isArray(targetUrl) ? targetUrl : [targetUrl];
    return this.router.navigate(commands, {
      state: { returnUrl }
    });
  }

  /**
   * Lit le returnUrl depuis `history.state` (alimenté par NavigationExtras.state).
   * Si absent, renvoie le `fallback`.
   *
   * Peut être appelé dans le constructor OU dans ngOnInit du composant cible.
   * Contrairement à l'ancien consumeReturnUrl(), il peut être lu plusieurs fois
   * (history.state persiste tant qu'on ne navigue pas ailleurs).
   */
  getReturnUrl(fallback: string): string {
    // D'abord essayer via getCurrentNavigation (disponible dans le constructor)
    const nav = this.router.getCurrentNavigation();
    const stateFromNav = nav?.extras?.state?.['returnUrl'];
    if (stateFromNav) {
      return stateFromNav;
    }
    // Fallback : history.state (disponible après la fin de la navigation, ex. dans ngOnInit)
    const stateFromHistory = history.state?.returnUrl;
    return stateFromHistory ?? fallback;
  }
}
