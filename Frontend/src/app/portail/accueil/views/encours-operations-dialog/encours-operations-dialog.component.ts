import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DialogModule } from 'primeng/dialog';
import { RecapService } from '../../../etatsDesEncours/views/lesEncours/recap/services/recap.service';
import { HorizonPrevision } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { SensOperation } from '../../../transactions/dtos/SensOperation';
import { dateCourte } from '../../../tresorerie/format';
import { RecapitulatifCompte } from '../../dtos/RecapitulatifCompte';
import { BlocSens, construireTimeline, PeriodeTimeline } from '../../encours-timeline';

export type VueEncours = 'ENTREES' | 'SORTIES' | 'TOUT';

/** Nombre maximal d'opérations demandées d'un coup : un seul appel, pas de pagination dans le popup. */
const TAILLE_MAX = 2000;

/**
 * Popup des encours d'une banque, ouvert depuis les lignes « Encours entrées » / « Encours sorties » d'une carte de
 * l'accueil : les opérations jusqu'à l'horizon de la carte, en timeline (Échues puis semaines ou mois), chaque
 * période groupée par sens puis par source. Les totaux viennent des mêmes lignes que celles de la carte.
 */
@Component({
  selector: 'app-encours-operations-dialog',
  standalone: true,
  imports: [DialogModule, RouterLink],
  templateUrl: './encours-operations-dialog.component.html',
  styleUrl: './encours-operations-dialog.component.scss',
})
export class EncoursOperationsDialogComponent implements OnChanges {

  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Input() compte: RecapitulatifCompte | null = null;
  @Input() horizon: HorizonPrevision = 'FIN_DE_MOIS';
  /** Ligne cliquée sur la carte : détermine la vue présélectionnée à l'ouverture. */
  @Input() sensInitial: SensOperation = SensOperation.ENCAISSEMENT;

  vue: VueEncours = 'ENTREES';
  periodes: PeriodeTimeline[] = [];
  totalEntrees = 0;
  totalSorties = 0;
  nombreEntrees = 0;
  nombreSorties = 0;
  chargement = false;
  erreur = false;
  /** Vrai si le serveur a plus d'opérations que celles reçues (jamais en pratique, signalé par prudence). */
  tronque = false;

  private repliees = new Set<string>();
  private recapService = inject(RecapService);

  ngOnChanges(changes: SimpleChanges): void {
    if (this.visible && this.compte && (changes['visible'] || changes['compte'] || changes['horizon'])) {
      this.vue = this.sensInitial === SensOperation.DECAISSEMENT ? 'SORTIES' : 'ENTREES';
      this.repliees.clear();
      this.charger(this.compte.banqueId);
    }
  }

  fermer(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  choisirVue(vue: VueEncours): void {
    this.vue = vue;
  }

  get titre(): string {
    const nom = this.compte?.nom ?? '';
    return this.vue === 'ENTREES' ? `${nom} · Encours entrées` : this.vue === 'SORTIES' ? `${nom} · Encours sorties` : `${nom} · Encours`;
  }

  get libelleHorizon(): string {
    return this.horizon === 'FIN_DE_TRIMESTRE' ? 'Fin de trimestre' : 'Fin de mois';
  }

  get totalAffiche(): number {
    return this.vue === 'ENTREES' ? this.totalEntrees : this.vue === 'SORTIES' ? -this.totalSorties : this.totalEntrees - this.totalSorties;
  }

  get nombreAffiche(): number {
    return this.vue === 'ENTREES' ? this.nombreEntrees : this.vue === 'SORTIES' ? this.nombreSorties : this.nombreEntrees + this.nombreSorties;
  }

  get periodesAffichees(): PeriodeTimeline[] {
    return this.periodes.filter(p => (this.vue !== 'SORTIES' && p.entrees) || (this.vue !== 'ENTREES' && p.sorties));
  }

  blocs(periode: PeriodeTimeline): { sens: 'ENTREES' | 'SORTIES'; bloc: BlocSens }[] {
    const blocs: { sens: 'ENTREES' | 'SORTIES'; bloc: BlocSens }[] = [];
    if (this.vue !== 'SORTIES' && periode.entrees) {
      blocs.push({ sens: 'ENTREES', bloc: periode.entrees });
    }
    if (this.vue !== 'ENTREES' && periode.sorties) {
      blocs.push({ sens: 'SORTIES', bloc: periode.sorties });
    }
    return blocs;
  }

  cle(periode: PeriodeTimeline, sens: string, source: string): string {
    return `${periode.code}|${sens}|${source}`;
  }

  estReplie(cle: string): boolean {
    return this.repliees.has(cle);
  }

  basculer(cle: string): void {
    if (!this.repliees.delete(cle)) {
      this.repliees.add(cle);
    }
  }

  montant(valeur: number): string {
    return valeur.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  jourMois(iso: string): string {
    return iso ? dateCourte(iso).slice(0, 5) : '—';
  }

  private charger(banqueId: number): void {
    this.chargement = true;
    this.erreur = false;
    this.recapService.getRecapitulatif(banqueId, { libelleOuObservation: '', periode: this.horizon }, 0, TAILLE_MAX, 'dateReference,asc')
      .subscribe({
        next: reponse => {
          const lignes = reponse.page.content;
          this.tronque = reponse.page.page.totalElements > lignes.length;
          this.periodes = construireTimeline(lignes, new Date(), this.horizon);
          this.totalEntrees = this.periodes.reduce((s, p) => s + (p.entrees?.total ?? 0), 0);
          this.totalSorties = this.periodes.reduce((s, p) => s + (p.sorties?.total ?? 0), 0);
          this.nombreEntrees = this.periodes.reduce((s, p) => s + (p.entrees?.nombre ?? 0), 0);
          this.nombreSorties = this.periodes.reduce((s, p) => s + (p.sorties?.nombre ?? 0), 0);
          this.chargement = false;
        },
        error: () => {
          this.periodes = [];
          this.chargement = false;
          this.erreur = true;
        },
      });
  }
}
