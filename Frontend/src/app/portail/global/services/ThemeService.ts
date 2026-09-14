import { Injectable, signal, effect } from '@angular/core';

export type Theme = 'light' | 'dark';

/**
 * ThemeService — gère le thème clair/sombre de l'application.
 *
 * Usage dans un composant :
 *   constructor(public themeService: ThemeService) {}
 *
 *   // Lire le thème courant (signal)
 *   theme = this.themeService.theme;
 *
 *   // Basculer
 *   <button (click)="themeService.toggle()">...</button>
 *
 *   // Ou forcer
 *   <button (click)="themeService.set('dark')">...</button>
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'gbanque-theme';

  /** Signal du thème courant — reactive, utilisable dans les templates */
  readonly theme = signal<Theme>(this.loadInitial());

  constructor() {
    // Applique [data-theme] sur <html> à chaque changement du signal
    effect(() => {
      document.documentElement.setAttribute('data-theme', this.theme());
      localStorage.setItem(this.STORAGE_KEY, this.theme());
    });
  }

  /** Bascule entre light et dark */
  toggle(): void {
    this.theme.update(t => (t === 'light' ? 'dark' : 'light'));
  }

  /** Force un thème précis */
  set(theme: Theme): void {
    this.theme.set(theme);
  }

  get isDark(): boolean {
    return this.theme() === 'dark';
  }

  // ─── initialisation ────────────────────────────────────────────────────────

  private loadInitial(): Theme {
    // 1. Préférence sauvegardée
    const saved = localStorage.getItem(this.STORAGE_KEY) as Theme | null;
    if (saved === 'light' || saved === 'dark') return saved;

    // 2. Préférence système
    if (window.matchMedia('(prefers-color-scheme: dark)').matches) return 'dark';

    // 3. Défaut
    return 'light';
  }
}
