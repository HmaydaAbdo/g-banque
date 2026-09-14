import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../auth/services/auth.service';
import { ButtonModule } from 'primeng/button';

/**
 * Page d'accueil générique — affiché aux utilisateurs dont le département
 * n'a pas encore son propre tableau de bord.
 * Remplacé progressivement par des accueils dédiés (Finance déjà disponible,
 * Marchés et autres à venir).
 */
@Component({
  selector: 'app-accueil-bienvenue',
  standalone: true,
  imports: [ButtonModule],
  templateUrl: './accueil-bienvenue.component.html',
  styleUrl: './accueil-bienvenue.component.scss'
})
export class AccueilBienvenueComponent {

  private authService = inject(AuthService);
  private router      = inject(Router);

  get username(): string {
    return this.authService.getUsername() ?? '';
  }

  navigateToModule(): void {
    this.router.navigate(['/portail']);
  }
}
