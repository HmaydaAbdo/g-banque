// line-navigation.service.ts
// Emplacement recommandé : src/app/shared/services/line-navigation.service.ts

import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';

/**
 * Enum pour distinguer les types de lignes fonctionnelles
 */
export enum LineType {
  GENERAL = 'GENERAL',
  SPECIFIC = 'SPECIFIC'
}

/**
 * Service centralisé pour gérer la navigation vers les écrans de suivi des lignes
 * Supporte les lignes générales (LigneFonctionnelle) et spécifiques (LigneFonctionnelleSpecifique)
 */
@Injectable({
  providedIn: 'root',
})
export class LineNavigationService {

  constructor(
    private router: Router,
    private messageService: MessageService
  ) {}

  /**
   * Redirige vers l'écran de détails approprié selon la nature et le type de ligne
   *
   * @param lineId - ID de la ligne fonctionnelle
   * @param natureLabel - Label de la nature (ex: "Factoring", "Préfinancement", "Droit constaté")
   * @param lineType - Type de ligne (GENERAL ou SPECIFIC)
   *
   * @example
   * // Pour une ligne générale
   * this.lineNavigationService.redirectToLineDetails(123, 'Factoring', LineType.GENERAL);
   *
   * @example
   * // Pour une ligne spécifique
   * this.lineNavigationService.redirectToLineDetails(456, 'Préfinancement', LineType.SPECIFIC);
   */
  redirectToLineDetails(lineId: number, natureLabel: string, lineType: LineType = LineType.SPECIFIC): void {
    const nature = natureLabel?.trim();

    // Déterminer le paramètre type pour les queryParams
    const typeParam = lineType === LineType.GENERAL ? 'general' : 'specific';

    switch (nature) {
      case 'Factoring':
        this.router.navigate(['/portail', 'factoring-encours', lineId], {
          queryParams: { type: typeParam }
        });
        break;

      case 'Préfinancement':
        this.router.navigate(['/portail', 'prefi-encours', lineId], {
          queryParams: { type: typeParam }
        });
        break;

      case 'Droit constaté':
        this.router.navigate(['/portail', 'dc-encours', lineId], {
          queryParams: { type: typeParam }
        });
        break;

      case 'Escompte Commercial':
        this.router.navigate(['/portail', 'lcn-encours', lineId], {
          queryParams: { type: typeParam }
        });
        break;

      default:
        this.showNoTrackingScreenWarning(nature);
        console.warn(`Redirection non gérée pour le label de nature: "${nature}"`);
        break;
    }
  }

  /**
   * Redirige automatiquement en détectant le type de ligne depuis l'objet
   * Détecte si c'est une ligne spécifique (avec marchés) ou générale (sans marché)
   *
   * @param ligne - Objet ligne avec propriétés id, natureLigneFonctionnelleLabel, et optionnellement marches
   *
   * @example
   * // Détection automatique du type
   * this.lineNavigationService.redirectToLineDetailsAuto(ligne);
   */
  redirectToLineDetailsAuto(ligne: any): void {
    if (!ligne) {
      console.error('Ligne object is null or undefined');
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Impossible de naviguer : ligne non définie',
        life: 3000
      });
      return;
    }

    if (!ligne.id || !ligne.natureLigneFonctionnelleLabel) {
      console.error('Ligne object missing required properties (id or natureLigneFonctionnelleLabel)', ligne);
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Données de ligne incomplètes',
        life: 3000
      });
      return;
    }

    // Détection automatique : si des marchés sont associés, c'est une ligne spécifique
    const lineType = ligne.marches && ligne.marches.length > 0 ? LineType.SPECIFIC : LineType.GENERAL;

    this.redirectToLineDetails(ligne.id, ligne.natureLigneFonctionnelleLabel, lineType);
  }

  /**
   * Affiche un message d'avertissement quand aucun écran de suivi n'est configuré
   * pour une nature de ligne donnée
   *
   * @param nature - Label de la nature de ligne
   */
  private showNoTrackingScreenWarning(nature: string): void {
    this.messageService.add({
      severity: 'warning',
      summary: 'Aucun écran de suivi configuré',
      detail: `Aucun écran de suivi n'est configuré pour la nature : "${nature}"`,
      life: 5000
    });
  }

  /**
   * Vérifie si une nature de ligne a un écran de suivi configuré
   *
   * @param natureLabel - Label de la nature de ligne
   * @returns true si un écran de suivi existe, false sinon
   *
   * @example
   * if (this.lineNavigationService.hasTrackingScreen('Factoring')) {
   *   // Écran de suivi disponible
   * }
   */
  hasTrackingScreen(natureLabel: string): boolean {
    const nature = natureLabel?.trim();
    const supportedNatures = ['Factoring', 'Préfinancement', 'Droit constaté'];
    return supportedNatures.includes(nature);
  }

  /**
   * Retourne la liste des natures de lignes supportées pour le suivi
   *
   * @returns Array des natures supportées
   */
  getSupportedNatures(): string[] {
    return ['Factoring', 'Préfinancement', 'Droit constaté'];
  }
}
