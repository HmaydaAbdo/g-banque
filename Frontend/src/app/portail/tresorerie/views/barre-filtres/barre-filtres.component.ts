import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { TooltipModule } from 'primeng/tooltip';
import {
  HORIZON_OPTIONS,
  HorizonPrevision,
} from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { TransactionSource, TransactionSourceLabels } from '../../../transactions/dtos/TransactionSource';
import { FiltresPlan, OPERATEURS, SensFiltre } from '../../plan-banque';
import { FiltresGlobal } from '../../plan-global';

export interface OptionFiltre<T> {
  label: string;
  value: T;
}

/** Résumé des opérations retenues par les filtres, affiché à côté des pastilles. */
export interface ResumeFiltre {
  nombre: number;
  entrees: number;
  sorties: number;
}

/**
 * Barre unique de la trésorerie (page globale et page d'une banque) : tous les filtres, l'horizon, la devise et l'export
 * sur une ligne, puis les filtres actifs en pastilles retirables avec le résumé des opérations retenues. La page porte
 * l'état (adresse) ; la barre ne fait qu'émettre les changements.
 */
@Component({
  selector: 'app-barre-filtres',
  standalone: true,
  imports: [FormsModule, ButtonModule, DropdownModule, InputNumberModule, InputTextModule, MultiSelectModule, TooltipModule],
  templateUrl: './barre-filtres.component.html',
  styleUrl: './barre-filtres.component.scss'
})
export class BarreFiltresComponent implements OnInit, OnDestroy {

  @Input({ required: true }) filtres!: FiltresPlan | FiltresGlobal;
  @Input({ required: true }) sources: OptionFiltre<TransactionSource>[] = [];
  /** Banques proposées (page globale) ; absent = pas de filtre de banque. */
  @Input() banques: OptionFiltre<number>[] | null = null;
  @Input({ required: true }) horizon: HorizonPrevision = 'FIN_DE_MOIS';
  /** Devises proposées ; le sélecteur n'apparaît qu'à partir de deux. */
  @Input() devises: string[] = [];
  @Input() devise = '';
  @Input() resume: ResumeFiltre | null = null;
  @Input() exportEnCours = false;
  @Input() exportPossible = true;
  @Input() exportInfo = '';

  @Output() filtresChange = new EventEmitter<Partial<FiltresGlobal>>();
  @Output() horizonChange = new EventEmitter<HorizonPrevision>();
  @Output() deviseChange = new EventEmitter<string>();
  @Output() effacer = new EventEmitter<void>();
  @Output() exporter = new EventEmitter<void>();

  readonly horizonOptions = HORIZON_OPTIONS;
  readonly operateurs = OPERATEURS;
  readonly sensOptions: OptionFiltre<SensFiltre>[] = [
    { label: 'Tous les sens', value: 'TOUS' },
    { label: 'Entrées', value: 'ENCAISSEMENT' },
    { label: 'Sorties', value: 'DECAISSEMENT' },
  ];

  /** Saisie de recherche, transmise après une courte pause de frappe. */
  texte = '';
  private readonly texte$ = new Subject<string>();
  private abonnement?: Subscription;

  ngOnInit(): void {
    this.texte = this.filtres.texte;
    this.abonnement = this.texte$.pipe(debounceTime(250)).subscribe(t => this.filtresChange.emit({ texte: t }));
  }

  ngOnDestroy(): void {
    this.abonnement?.unsubscribe();
  }

  get banquesChoisies(): number[] {
    return (this.filtres as FiltresGlobal).banques ?? [];
  }

  /** Un changement de valeur réel seulement (PrimeNG émet aussi à l'initialisation). */
  changer<K extends keyof FiltresGlobal>(cle: K, valeur: FiltresGlobal[K]): void {
    const actuelle = (this.filtres as FiltresGlobal)[cle];
    if (JSON.stringify(actuelle ?? null) !== JSON.stringify(valeur ?? null)) {
      this.filtresChange.emit({ [cle]: valeur } as Partial<FiltresGlobal>);
    }
  }

  saisirTexte(valeur: string): void {
    this.texte = valeur;
    this.texte$.next(valeur);
  }

  get actif(): boolean {
    const f = this.filtres;
    return this.banquesChoisies.length > 0 || f.sens !== 'TOUS' || f.sources.length > 0 || f.montant !== null || f.texte.trim() !== '';
  }

  /** Pastilles des filtres actifs, chacune retirable. */
  get pastilles(): { cle: string; libelle: string; retirer: () => void }[] {
    const p: { cle: string; libelle: string; retirer: () => void }[] = [];
    const f = this.filtres;
    this.banquesChoisies.forEach(id => p.push({
      cle: `banque-${id}`, libelle: this.banques?.find(b => b.value === id)?.label ?? `Banque ${id}`,
      retirer: () => this.filtresChange.emit({ banques: this.banquesChoisies.filter(x => x !== id) }),
    }));
    if (f.sens !== 'TOUS') {
      p.push({ cle: 'sens', libelle: f.sens === 'ENCAISSEMENT' ? 'Entrées' : 'Sorties', retirer: () => this.filtresChange.emit({ sens: 'TOUS' }) });
    }
    f.sources.forEach(s => p.push({
      cle: `src-${s}`, libelle: TransactionSourceLabels[s] ?? s,
      retirer: () => this.filtresChange.emit({ sources: f.sources.filter(x => x !== s) }),
    }));
    if (f.montant !== null) {
      const op = OPERATEURS.find(o => o.value === f.operateur)?.label ?? f.operateur;
      p.push({ cle: 'montant', libelle: `Montant ${op} ${this.montantTexte(f.montant)}`, retirer: () => this.filtresChange.emit({ montant: null }) });
    }
    if (f.texte.trim()) {
      p.push({ cle: 'texte', libelle: `« ${f.texte.trim()} »`, retirer: () => { this.texte = ''; this.filtresChange.emit({ texte: '' }); } });
    }
    return p;
  }

  toutEffacer(): void {
    this.texte = '';
    this.effacer.emit();
  }

  montantTexte(v: number): string {
    return Math.abs(v).toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
