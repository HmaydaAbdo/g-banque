import { AfterViewInit, Component, ElementRef, EventEmitter, inject, Input, OnChanges, OnDestroy, Output } from '@angular/core';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { Subscription } from 'rxjs';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';
import { RecapService } from '../../../etatsDesEncours/views/lesEncours/recap/services/recap.service';
import { RecaputilatifItem } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { TransactionSourceLabels } from '../../../transactions/dtos/TransactionSource';
import { parseLocalDate } from '../../../global/types/date.utils';
import { HorizonPrevision } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { ColonnePlan, PlanBanque } from '../../dtos/PlanTresorerie';
import { EncoursNavigationService } from '../../services/encours-navigation.service';
import { PlanTresorerieTableComponent } from '../plan-tresorerie-table/plan-tresorerie-table.component';
import { MouvementsListComponent } from '../mouvements-list/mouvements-list.component';

/** Nombre d'opérations montrées dans le détail d'une période (les plus grosses d'abord). */
const OPERATIONS_AFFICHEES = 8;

/**
 * Détail d'une banque (ou du groupe), déplié sous les cartes : indicateurs, opérations réelles d'une
 * période (celle du point bas par défaut), plus gros mouvements et flux par famille.
 * Les opérations viennent du Récapitulatif (mêmes critères, date de référence bornée à la période).
 */
@Component({
  selector: 'app-detail-banque',
  standalone: true,
  imports: [
    DatePipe, DecimalPipe, NgClass, SkeletonModule, TooltipModule,
    PlanTresorerieTableComponent, MouvementsListComponent,
  ],
  templateUrl: './detail-banque.component.html',
  styleUrl: './detail-banque.component.scss'
})
export class DetailBanqueComponent implements OnChanges, AfterViewInit, OnDestroy {

  private readonly recapService = inject(RecapService);
  private readonly navigation = inject(EncoursNavigationService);
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private chargement?: Subscription;

  @Input({ required: true }) plan!: PlanBanque;
  @Input({ required: true }) colonnes: ColonnePlan[] = [];
  /** Code de la période choisie ; vide = celle qui contient le point bas. */
  @Input() colonne = '';
  /** Horizon de l'écran, emporté dans l'adresse quand on ouvre les encours. */
  @Input() horizon: HorizonPrevision = 'FIN_DE_MOIS';

  @Output() fermer = new EventEmitter<void>();
  @Output() colonneChange = new EventEmitter<string>();

  operations: RecaputilatifItem[] = [];
  totalOperations = 0;
  chargementOperations = false;
  readonly source = TransactionSourceLabels;

  /** Période dont on affiche les opérations. */
  get periode(): ColonnePlan {
    return this.colonnes.find(c => c.code === this.colonne) ?? this.periodeDuPointBas();
  }

  /** Dernier jour du plan (fin du mois ou du trimestre). */
  get dateHorizon(): string {
    return this.colonnes[this.colonnes.length - 1]?.au ?? '';
  }

  /** Les opérations ne sont consultables que pour une banque : un total n'a pas de liste unique. */
  get listeDisponible(): boolean {
    return this.plan?.banqueId !== null;
  }

  get statut(): 'ok' | 'warn' | 'danger' {
    if (this.plan.besoinFinancement > 0) {
      return 'danger';
    }
    return this.plan.joursSoldeNegatif > 0 ? 'warn' : 'ok';
  }

  /** Le détail s'ouvre sous les cartes : on l'amène à l'écran. */
  ngAfterViewInit(): void {
    this.element.nativeElement.scrollIntoView?.({ block: 'nearest', behavior: 'smooth' });
  }

  ngOnChanges(): void {
    this.charger();
  }

  ngOnDestroy(): void {
    this.chargement?.unsubscribe();
  }

  choisirPeriode(code: string): void {
    this.colonneChange.emit(code);
  }

  /**
   * Ouvre le Récapitulatif de la banque sur son mois en cours, avec les filtres de l'écran : l'horizon
   * et, pour une opération, sa source et les dates de la période affichée.
   */
  ouvrirEncours(operation?: RecaputilatifItem): void {
    const periode = this.periode;
    const filtres: Record<string, string | null> = { horizon: this.horizon === 'FIN_DE_TRIMESTRE' ? this.horizon : null };
    if (operation) {
      filtres['source'] = operation.source;
      filtres['du'] = periode.du;
      filtres['au'] = periode.au;
    }
    this.navigation.ouvrirRecapitulatif(this.plan.banqueId!, filtres);
  }

  /** Date d'une opération : son échéance si elle en a une, sinon son émission. */
  dateDe(item: RecaputilatifItem): string {
    return item.dateEcheance || item.dateEmission;
  }

  private periodeDuPointBas(): ColonnePlan {
    const date = this.plan.pointBas.date;
    return this.colonnes.find(c => date <= c.au) ?? this.colonnes[this.colonnes.length - 1];
  }

  private charger(): void {
    this.chargement?.unsubscribe();
    this.operations = [];
    this.totalOperations = 0;
    if (!this.plan || !this.listeDisponible || this.colonnes.length === 0) {
      return;
    }
    const p = this.periode;
    this.chargementOperations = true;
    this.chargement = this.recapService.getRecapitulatif(
      this.plan.banqueId!,
      {
        libelleOuObservation: '',
        dateReferenceDepuis: p.du ? parseLocalDate(p.du) : null,
        dateReferenceJusqua: parseLocalDate(p.au),
      },
      0, OPERATIONS_AFFICHEES, 'montant,desc'
    ).subscribe({
      next: reponse => {
        this.operations = reponse.page.content;
        this.totalOperations = reponse.page.page.totalElements;
        this.chargementOperations = false;
      },
      error: () => {
        this.chargementOperations = false;
      }
    });
  }
}
