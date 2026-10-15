import { inject, Injectable } from '@angular/core';
import { Params, Router } from '@angular/router';
import { RapprochementService } from '../../rapprochement/service/rapprochement-service.service';

/**
 * Ouvre le Récapitulatif d'une banque directement sur son rapprochement « En cours » (le mois en
 * cours), sans passer par l'écran de choix de la période, et en emportant les filtres voulus dans
 * l'adresse (`horizon`, `source`, `du`, `au`…) pour que l'écran s'ouvre déjà filtré.
 */
@Injectable({
  providedIn: 'root'
})
export class EncoursNavigationService {

  private readonly rapprochementService = inject(RapprochementService);
  private readonly router = inject(Router);

  ouvrirRecapitulatif(banqueId: number, queryParams: Params = {}): void {
    this.rapprochementService.getActifs(banqueId).subscribe(actifs => {
      this.router.navigate(
        ['/portail/etats-des-encours', banqueId, actifs.enCoursId, 'recapitulatif'],
        { queryParams }
      );
    });
  }
}
