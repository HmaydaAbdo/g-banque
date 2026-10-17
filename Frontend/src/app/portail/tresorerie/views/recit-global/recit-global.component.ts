import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { NgClass } from '@angular/common';
import { RecaputilatifItem } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { SituationFc, situationFc } from '../../../accueil/fc-consommation';
import { FcBarreComponent } from '../../../accueil/views/fc-barre/fc-barre.component';
import { ColonnePlan, PlanBanque } from '../../dtos/PlanTresorerie';
import { montantCompact } from '../../format';
import { FiltresPlan, filtreActif, libellePeriode } from '../../plan-banque';
import {
  construireEtapesGlobales, construireJalonsGlobaux, construireResume, EtapeGlobale, JalonBanque, ResumeGlobal,
} from '../../plan-global';
import { joursEntre, niveauSituation, ordonnerParUrgence } from '../../situation';

/** Un élément de la frise : un jalon (losange, propre à une banque) ou une étape (une période du plan). */
type ElementFrise = { type: 'jalon'; jalon: JalonBanque } | { type: 'etape'; etape: EtapeGlobale };

/**
 * Vue « Récit » de la trésorerie de toutes les banques : une phrase qui résume l'horizon du groupe (en rappelant qu'une
 * facilité de caisse ne couvre que sa banque), le bandeau « À surveiller » (les banques de la plus urgente à la plus
 * sereine), puis la frise des périodes. Une étape dépliée liste les banques (solde, facilité de caisse, flux, statut) ;
 * le détail des opérations se lit dans la page de la banque, un clic plus loin.
 */
@Component({
  selector: 'app-recit-global',
  standalone: true,
  imports: [NgClass, FcBarreComponent],
  templateUrl: './recit-global.component.html',
  styleUrl: './recit-global.component.scss'
})
export class RecitGlobalComponent implements OnChanges {

  @Input({ required: true }) banques: PlanBanque[] = [];
  @Input({ required: true }) colonnes: ColonnePlan[] = [];
  @Input({ required: true }) operations: RecaputilatifItem[] = [];
  @Input({ required: true }) filtres!: FiltresPlan;
  @Input({ required: true }) ouvertes = new Set<string>();
  @Input() dateCalcul = '';

  @Output() basculer = new EventEmitter<string>();
  @Output() toutOuvrir = new EventEmitter<boolean>();
  /** Ouvre la page d'une banque, sur une période (code de colonne) ou sans. */
  @Output() ouvrirBanque = new EventEmitter<{ banqueId: number; colonne: string }>();

  frise: ElementFrise[] = [];
  surveillance: PlanBanque[] = [];
  resume: ResumeGlobal | null = null;
  readonly compact = montantCompact;

  ngOnChanges(): void {
    const jalons = construireJalonsGlobaux(this.banques, this.colonnes, this.operations);
    const etapes = construireEtapesGlobales(this.banques, this.colonnes, this.operations, this.filtres);
    this.surveillance = ordonnerParUrgence(this.banques);
    this.resume = construireResume(this.banques, this.operations, this.filtres, jalons);
    this.frise = [];
    etapes.forEach(etape => {
      this.frise.push({ type: 'etape', etape });
      jalons.filter(j => j.colonne === etape.index).forEach(jalon => this.frise.push({ type: 'jalon', jalon }));
    });
  }

  get filtre(): boolean {
    return filtreActif(this.filtres);
  }

  get toutesOuvertes(): boolean {
    return this.colonnes.length > 0 && this.colonnes.every(c => this.ouvertes.has(c.code));
  }

  get dateHorizon(): string {
    return this.colonnes.length ? this.colonnes[this.colonnes.length - 1].au : '';
  }

  estOuverte(etape: EtapeGlobale): boolean {
    return this.ouvertes.has(etape.colonne.code) || (this.filtre && etape.nombreFiltrees > 0);
  }

  fc(solde: number, plafond: number): SituationFc {
    return situationFc(solde, plafond);
  }

  /** Statut d'une banque sur tout l'horizon, au solde le plus bas. */
  statut(banque: PlanBanque): SituationFc {
    return situationFc(banque.pointBas.solde, banque.plafondFC);
  }

  /** « Nouveau » le jour où le besoin apparaît, sinon « depuis N j » ; vide si la banque n'est pas en besoin. */
  anciennete(banque: PlanBanque): string {
    if (!banque.enBesoinDepuis || niveauSituation(banque) !== 'danger') {
      return '';
    }
    const jours = joursEntre(banque.enBesoinDepuis, this.dateCalcul);
    return jours > 0 ? `depuis ${jours} j` : 'nouveau';
  }

  libelleEtape(etape: EtapeGlobale): string {
    return libellePeriode(etape.colonne, etape.index).titre;
  }

  sousTitreEtape(etape: EtapeGlobale): string {
    return libellePeriode(etape.colonne, etape.index).sous;
  }

  jourMois(iso: string): string {
    return iso ? `${iso.slice(8, 10)}/${iso.slice(5, 7)}` : '—';
  }

  signe(v: number): string {
    const a = Math.round(v * 100) / 100;
    return a === 0 ? 'aucun flux' : (a > 0 ? '+' : '') + montantCompact(a);
  }

  noms(liste: { nom: string; date: string }[]): string {
    return liste.map(b => b.nom).join(', ');
  }

  premiereDate(liste: { nom: string; date: string }[]): string {
    return liste.map(b => b.date).sort()[0] ?? '';
  }
}
