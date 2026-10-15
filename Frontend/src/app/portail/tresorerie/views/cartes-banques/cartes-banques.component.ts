import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { PlanBanque } from '../../dtos/PlanTresorerie';
import { montantCompact } from '../../format';
import {
  excedent, joursEntre, NiveauSituation, niveauSituation, ordonnerParUrgence, partFacilite, trajectoire,
} from '../../situation';
import { CaseChoisie } from '../position-grid/position-grid.component';

/**
 * Une carte par banque, classées par urgence : situation, besoin (ou solde le plus bas) avec son jour et son
 * ancienneté, soldes actuel et prévu, part de facilité de caisse utilisée, trajectoire du solde. Une banque
 * saine affiche ce qu'elle peut prêter quand une autre est en besoin. Un clic ouvre le détail de la banque.
 */
@Component({
  selector: 'app-cartes-banques',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './cartes-banques.component.html',
  styleUrl: './cartes-banques.component.scss'
})
export class CartesBanquesComponent {

  /** Banques de la devise affichée ; le classement par urgence est fait ici. */
  @Input({ required: true }) set banques(valeur: PlanBanque[]) {
    this.ordonnees = ordonnerParUrgence(valeur);
    this.uneBanqueEnBesoin = valeur.some(b => niveauSituation(b) === 'danger');
  }
  /** Date du calcul (yyyy-MM-dd) : référence de l'ancienneté d'un besoin. */
  @Input({ required: true }) dateCalcul = '';
  @Input() selection: number | null | undefined;
  @Output() choisir = new EventEmitter<CaseChoisie>();

  ordonnees: PlanBanque[] = [];
  /** L'excédent des banques saines n'est montré que s'il peut combler un besoin. */
  uneBanqueEnBesoin = false;

  readonly niveau = niveauSituation;
  readonly trajectoire = trajectoire;
  readonly partFacilite = partFacilite;
  readonly excedent = excedent;

  libelleSituation(niveau: NiveauSituation): string {
    return niveau === 'danger' ? 'Besoin' : niveau === 'warn' ? 'Facilité de caisse utilisée' : 'Sans souci';
  }

  compact(valeur: number): string {
    return montantCompact(valeur);
  }

  /** « Nouveau » le jour où le besoin apparaît, sinon « depuis N j ». */
  anciennete(plan: PlanBanque): string {
    if (!plan.enBesoinDepuis) {
      return '';
    }
    const jours = joursEntre(plan.enBesoinDepuis, this.dateCalcul);
    return jours > 0 ? `depuis ${jours} j` : 'nouveau';
  }

  ouvrir(plan: PlanBanque): void {
    this.choisir.emit({ banqueId: plan.banqueId, devise: plan.devise, colonne: '' });
  }
}
