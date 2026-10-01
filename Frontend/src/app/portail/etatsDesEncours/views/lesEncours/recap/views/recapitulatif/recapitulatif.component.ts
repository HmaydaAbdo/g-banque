import { Component, inject, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { Table } from 'primeng/table';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Observable, Subscription } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MultiSelectModule } from 'primeng/multiselect';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { DialogModule } from 'primeng/dialog';
import { DividerModule } from 'primeng/divider';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { RecapService } from "../../services/recap.service";
import { RecaputilatifItem } from "../../dtos/RecaputilatifItem";
import { TransactionSource, TransactionSourceLabels } from "../../../../../../transactions/dtos/TransactionSource";
import { SourceBadgeComponent } from "../../../../../../global/shared-components/source-badge/source-badge.component";
import { SensOperation } from "../../../../../../transactions/dtos/SensOperation";
import { RecapitulatifSearchDto } from "../../dtos/RecapitulatifSearchDto";
import { AgregatedRecapDto } from "../../dtos/AgregatedRecapDto";
import { TableSelectionState } from "../../../shared/table-selection.state";
import { BatchValidationRequestDto } from "../../../../../dtos/lot/BatchValidationRequestDto";
import { BatchValidationResponseDto } from "../../../../../dtos/lot/BatchValidationResponseDto";
import { ValidationService } from "../../../../../services/validation/validation.service";
import { BankStateService } from "../../../../../../comptes-bancaire/services/bank-state.service";
import { TvaInputComponent } from "../../../shared/tva-input/tva-input.component";
import {
  decrireSaisieTva,
  messageErreurTva,
  SaisieTva,
  saisieTvaParDefaut
} from "../../../shared/tva-input/saisie-tva.model";
import { EcheanceLeasingService } from "../../../../../../leasing/contratsLeasing/services/EcheanceLeasingService";
import { EcheanceLeasingResponse } from "../../../../../../leasing/contratsLeasing/dtos/EcheanceLeasingResponse";
import { EcheanceCmtService } from "../../../../../../cmt/services/EcheanceCmtService";
import { EcheanceCMTResponseDto } from "../../../../../../cmt/dtos/EcheanceCMTResponseDto";
import { AuthService } from "../../../../../../../auth/services/auth.service";
import { Motif, MotifLabels } from "../../../../../../impotEtTaxes/dtos/Motif";
import { ChequeService } from "../../../../../../cheques/services/ChequeService";
import { ChequeResponseDto } from "../../../../../../cheques/dtos/ChequeResponseDto";
import { LcnService } from "../../../../../../LCN/service/LcnService";
import { LcnResponseDTO } from "../../../../../../LCN/dtos/LcnResponseDTO";
import { VirementService } from "../../../../../../Virements/service/VirementService";
import { VirementResponseDto } from "../../../../../../Virements/dtos/VirementResponseDto";
import { Type as SensType } from "../../../../../../Virements/Type";
import { PrefinancementService } from "../../../../../../prefinancements/service/PrefinancementService";
import { PrefinancementResponseDTO } from "../../../../../../prefinancements/dtos/PrefinancementResponseDTO";
import { DroitConstateService } from "../../../../../../droitsConstate/service/DroitConstateService";
import { DroitConstateResponseDto } from "../../../../../../droitsConstate/dtos/DroitConstateResponseDto";
import { FactoringService } from "../../../../../../factoring/service/FactoringService";
import { FactoringResponseDto } from "../../../../../../factoring/dtos/FactoringResponseDto";
import { DecompteService } from "../../../../../../decomptes/service/DecompteService";
import { DecompteResponseDto } from "../../../../../../decomptes/dtos/DecompteResponseDto";
import { RestitutionService } from "../../../../../../restitution/service/restitution.service";
import { RestitutionResponseDto } from "../../../../../../restitution/dtos/RestitutionResponseDto";
import { ProvisionService } from "../../../../../../provision/service/provision.service";
import { ProvisionResponseDto } from "../../../../../../provision/dtos/ProvisionResponseDto";
import { ImpayeeService } from "../../../../../../impayees/service/ImpayeeService";
import { ImpayeeResponseDto } from "../../../../../../impayees/dtos/ImpayeeResponseDto";
import { VirementInternationalService } from "../../../../../../virementInternational/service/virement-international.service";
import { VirementInternationalResponseDto } from "../../../../../../virementInternational/dtos/VirementInternationalResponseDto";
import { MiseADispositionService } from "../../../../../../Mise-à-disposition/service/MiseADispositionService";
import { MiseADispositionResponseDto } from "../../../../../../Mise-à-disposition/dtos/MiseADispositionResponseDto";
import { AgiosService } from "../../../../../../agios/service/agios.service";
import { AgiosResponseDto } from "../../../../../../agios/dtos/AgiosResponseDto";
import { CommissionService } from "../../../../../../commission/service/commission.service";
import { CommissionResponseDto } from "../../../../../../commission/dtos/commission-response.dto";
import { FraisBancaireService } from "../../../../../../frais-bancaire/service/FraisBancaireService";
import { FraisBancaireResponseDto } from "../../../../../../frais-bancaire/dtos/FraisBancaireResponseDto";
import { PrelevementService } from "../../../../../../prelevements/service/prelevement.service";
import { PrelevementResponseDto } from "../../../../../../prelevements/dtos/PrelevementResponseDto";
import { ImpotEtTaxeService } from "../../../../../../impotEtTaxes/service/ImpotEtTaxeService";
import { ImpotEtTaxeResponseDto } from "../../../../../../impotEtTaxes/dtos/ImpotEtTaxeResponseDto";
import { OperationErroneService } from "../../../../../../operationErronées/service/operation-errone.service";
import { OperationErroneResponseDto } from "../../../../../../operationErronées/dtos/OperationErroneResponseDto";
import { parseLocalDate } from '../../../../../../global/types/date.utils';
import { RecapSourceDto } from "../../dtos/RecapSourceDto";

/** Options du dropdown de filtre de sens — `null` = « Tous », aucun filtre appliqué. */
interface SensOption {
  label: string;
  value: SensOperation | null;
}

/**
 * Un groupe d'opérations sélectionnées partageant (source, sens) — l'unité d'appel de
 * POST /rapprochement/process, qui n'accepte qu'un seul type et un seul sens par requête
 * (voir BatchValidationRequestDto). Sert aussi à détecter une sélection hétérogène :
 * {@code lotGroups.length > 1} signifie que la sélection mélange plusieurs sources/sens, auquel
 * cas « Valider en lot » refuse d'ouvrir le dialogue (voir {@code selectionEstHomogene}).
 * Revirement du 19/09/2026 : la version précédente découpait silencieusement la sélection en
 * plusieurs appels séquentiels (un par groupe) — jugé confus/risqué (décision explicite de
 * HMAYDA), remplacé par un refus explicite tant que la sélection n'est pas homogène.
 */
interface LotGroup {
  source: TransactionSource;
  sens: SensOperation;
  ids: number[];
  montantTotal: number;
}

/**
 * Champs communs qu'a besoin le dialogue générique de validation unitaire, extraits du DTO de
 * détail spécifique à chaque source (`EcheanceLeasingResponse`, `EcheanceCMTResponseDto`, ...)
 * par `UnitValidationConfig.toCommonDetail`.
 */
interface UnitValidationDetail {
  dateEcheance: string;
  observation: string;
  libelle: string;
  montantTTC: number;
  /** Libellé de l'identifiant métier affiché dans le message de confirmation (ex. "Contrat N°"). */
  recapLabel: string;
  recapValue: string | number;
}

/**
 * Décrit tout ce que le dialogue générique de validation unitaire a besoin de savoir sur une
 * source pour fonctionner : comment récupérer le détail, en extraire les champs communs, la
 * politique TVA, et comment construire + envoyer la requête de validation spécifique à cette
 * source. Ajouter une source ne demande qu'une entrée dans `unitValidationConfigs`, plus le
 * détail spécifique à cette source (`TDetail`) — jamais un nouveau dialogue.
 *
 * `any` par défaut sur `TDetail` : la Map `unitValidationConfigs` est volontairement hétérogène
 * (une config par source, chacune avec son propre DTO de détail) : chaque config reste fortement
 * typée en son sein (voir les entrées Leasing/CMT ci-dessous), seule la signature de la Map les
 * réunit sous un type commun.
 */
interface UnitValidationConfig<TDetail = any> {
  fetchDetail: (id: number) => Observable<TDetail>;
  toCommonDetail: (detail: TDetail) => UnitValidationDetail;
  /** Valeur initiale du champ TVA — éditable pour toutes les sources câblées à ce jour. */
  tvaInitialValue: SaisieTva;
  /** Prévu pour une source dont la TVA devrait rester verrouillée — aucune à ce jour. */
  tvaLocked: boolean;
  confirmHeader: string;
  successDetail: string;
  fetchErrorDetail: string;
  /**
   * Route la requête vers la bonne source (et son sens/banqueId le cas échéant) — depuis la
   * fusion du 19/09/2026 de la validation unitaire dans BatchValidationRequestDto
   * (architecture.md, Règle 1), tout le reste (ids, dates, TVA, observation, libellé) est commun
   * à toutes les sources et construit une seule fois par `submitUnitValidation`.
   */
  toRoutingFields: (detail: TDetail) => Pick<BatchValidationRequestDto, 'transactionSource' | 'sens' | 'banqueId'>;
}

/** Clé d'une carte : une source précise (il n'y a plus de carte « toutes les opérations »). */
type RecapCardKey = TransactionSource;

/** Données d'affichage d'une carte du groupement par source. */
interface RecapCardView {
  key: RecapCardKey;
  label: string;
  icon: string;
  count: number;
  debit: number;
  credit: number;
  echues: number;
}

/** Totaux tous sources confondues — alimentent uniquement les indicateurs globaux de la page. */
interface RecapGlobalView {
  count: number;
  debit: number;
  credit: number;
  echues: number;
}

@Component({
  selector: 'app-recapitulatif',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    TooltipModule,
    ToastModule,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    MultiSelectModule,
    InputTextModule,
    InputTextareaModule,
    CalendarModule,
    DropdownModule,
    ButtonModule,
    RippleModule,
    DialogModule,
    DividerModule,
    ConfirmDialogModule,
    SourceBadgeComponent,
    TvaInputComponent,
    FormsModule,
  ],
  templateUrl: './recapitulatif.component.html',
  styleUrl: './recapitulatif.component.scss',
  providers: [MessageService, ConfirmationService, DecimalPipe]
})
export class RecapitulatifComponent implements OnInit, OnDestroy {
  @ViewChild('dt') dt: Table | undefined;

  // ── Groupement par source (cartes) ───────────────────────────────────
  cardsView: RecapCardView[] = [];
  cardsLoading = true;
  /** Carte ouverte : une source précise, ou null (aucune). */
  openCard: RecapCardKey | null = null;
  /** Totaux de toutes les sources — portent les indicateurs globaux de la page (plus de carte dédiée). */
  globalView?: RecapGlobalView;
  /** Icônes des sources absentes de AJOUTER_ICON — purement visuel. */
  private static readonly CARD_ICON: Partial<Record<TransactionSource, string>> = {
    [TransactionSource.LEASING]: 'pi pi-car',
    [TransactionSource.CMT]: 'pi pi-building',
    [TransactionSource.MiseADisposition]: 'pi pi-send',
    [TransactionSource.LCN]: 'pi pi-file',
    [TransactionSource.LCN_DECAISSEMENT]: 'pi pi-file',
    [TransactionSource.LCN_ESCOMPTE]: 'pi pi-file',
    [TransactionSource.AGIOS]: 'pi pi-percentage',
    [TransactionSource.CAUTION]: 'pi pi-lock',
  };

  sourceLabel(source: TransactionSource): string {
    return TransactionSourceLabels[source] ?? source;
  }

  sourceIcon(source: TransactionSource): string {
    return RecapitulatifComponent.CARD_ICON[source]
      ?? RecapitulatifComponent.AJOUTER_ICON[source]
      ?? 'pi pi-folder';
  }

  // Table data state properties
  totalDebit!: number;
  totalCredit!: number;
  recapItems: RecaputilatifItem[] = [];
  totalRecords: number = 0;
  loading: boolean = true;
  exporting: boolean = false;
  banqueIdFromRoute!: number;

  searchForm!: FormGroup;
  transactionSourceOptions: any[] = [];
  readonly sensOptions: SensOption[] = [
    { label: 'Tous', value: null },
    { label: 'Encaissement', value: SensOperation.ENCAISSEMENT },
    { label: 'Décaissement', value: SensOperation.DECAISSEMENT },
  ];
  readonly SensOperation = SensOperation;
  readonly TransactionSource = TransactionSource;

  /** Options du filtre « Motif » de Commission — restaure le filtre de l'ancien écran dédié
   * (21/09/2026), affiché seulement quand COMMISSION fait partie des sources sélectionnées. */
  readonly commissionOperationOptions = Object.values(TransactionSource)
    .map(source => ({ label: TransactionSourceLabels[source] ?? source, value: source }))
    .sort((a, b) => a.label.localeCompare(b.label, 'fr'));

  /** Options du filtre « Motif » d'Impôt/Taxe — restaure le filtre de l'ancien écran dédié
   * (21/09/2026), affiché seulement quand IMPOT_TAXE fait partie des sources sélectionnées. */
  readonly motifOptions = Object.values(Motif)
    .map(motif => ({ label: MotifLabels[motif] ?? motif, value: motif }))
    .sort((a, b) => a.label.localeCompare(b.label, 'fr'));

  /** Explication du numéro de pièce — toutes les sources n'en portent pas. */
  readonly numeroHelp =
    'Numéro de la pièce chez sa source : chèque, LCN, virement, mise à disposition, '
    + 'impayé, numéro de marché (préfinancement, factoring, droit constaté, décompte) '
    + 'ou numéro de contrat (leasing). Les autres sources n\'en ont pas.';

  /** Explication de « date de référence » — partagée par les deux bornes du filtre. */
  readonly dateReferenceHelp =
    'Chaque opération est filtrée sur sa propre date de référence : la date d\'échéance '
    + 'pour le leasing, le CMT et les LCN, la date d\'émission pour toutes les autres sources.';

  /**
   * Vrai tant que l'utilisateur n'a pas cliqué d'en-tête : le tableau est alors trié sur
   * `dateReference`, qui n'est pas une colonne affichée — aucune flèche de tri ne peut donc
   * l'indiquer. On l'annonce en clair plutôt que de laisser croire à un ordre arbitraire.
   */
  sortParDefaut = true;

  /** Panneau des bornes de date, replie par defaut pour laisser la place au tableau. */
  showDatePanel = false;

  private subscriptions: Subscription[] = [];

  private readonly recapService = inject(RecapService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly validationService = inject(ValidationService);
  private readonly bankStateService = inject(BankStateService);
  private readonly echeanceLeasingService = inject(EcheanceLeasingService);
  private readonly echeanceCmtService = inject(EcheanceCmtService);
  private readonly chequeService = inject(ChequeService);
  private readonly lcnService = inject(LcnService);
  private readonly virementService = inject(VirementService);
  private readonly prefinancementService = inject(PrefinancementService);
  private readonly droitConstateService = inject(DroitConstateService);
  private readonly factoringService = inject(FactoringService);
  private readonly decompteService = inject(DecompteService);
  private readonly restitutionService = inject(RestitutionService);
  private readonly provisionService = inject(ProvisionService);
  private readonly impayeeService = inject(ImpayeeService);
  private readonly virementInternationalService = inject(VirementInternationalService);
  private readonly miseADispositionService = inject(MiseADispositionService);
  private readonly agiosService = inject(AgiosService);
  private readonly commissionService = inject(CommissionService);
  private readonly fraisBancaireService = inject(FraisBancaireService);
  private readonly prelevementService = inject(PrelevementService);
  private readonly impotEtTaxeService = inject(ImpotEtTaxeService);
  private readonly operationErroneService = inject(OperationErroneService);
  private readonly datePipe = inject(DatePipe);
  private readonly decimalPipe = inject(DecimalPipe);

  // ==================== Sélection multi-lignes & validation en lot ====================

  /**
   * Clé composite (source, entityId) — `entityId` seul n'est unique que combiné à sa source
   * (deux sources différentes peuvent partager le même id), contrairement aux écrans à source
   * unique qui utilisent l'id brut.
   */
  selectionState = new TableSelectionState<RecaputilatifItem, string>(item => item.compositeKey);

  get pageSelection(): RecaputilatifItem[] {
    return this.selectionState.getPageSelection(this.recapItems);
  }

  onSelectionChange(selection: RecaputilatifItem[]): void {
    this.selectionState.onPageSelectionChange(this.recapItems, selection);
  }

  get selectedCount(): number {
    return this.selectionState.count;
  }

  get montantTotalSelection(): number {
    return this.selectionState.sumBy(item => Number(item.debit ?? item.credit ?? 0));
  }

  clearSelection(): void {
    this.selectionState.clear();
  }

  /** Regroupe la sélection par (source, sens) — un groupe = un appel POST /process. */
  get lotGroups(): LotGroup[] {
    const groups = new Map<string, LotGroup>();
    for (const item of this.selectionState.selectedItems) {
      const key = `${item.source}__${item.sens}`;
      let group = groups.get(key);
      if (!group) {
        group = { source: item.source, sens: item.sens, ids: [], montantTotal: 0 };
        groups.set(key, group);
      }
      group.ids.push(item.id);
      group.montantTotal += Number(item.debit ?? item.credit ?? 0);
    }
    return Array.from(groups.values());
  }

  displayLotModal = false;
  lotForm!: FormGroup;
  isLotSubmitting = false;
  readonly messageErreurTva = messageErreurTva;

  /**
   * Faux dès que la sélection mélange plusieurs sources ou plusieurs sens. La validation en lot
   * n'applique qu'un seul jeu de champs communs (TVA, dates) à tout le lot : l'appliquer à des
   * opérations de nature différente n'a pas de sens métier. Décision du 19/09/2026 : remplace le
   * découpage automatique en plusieurs appels /process par un refus explicite — l'utilisateur
   * doit sélectionner une seule combinaison (source, sens) à la fois.
   */
  get selectionEstHomogene(): boolean {
    return this.lotGroups.length <= 1;
  }

  openLotModal(): void {
    if (!this.selectionEstHomogene) {
      this.messageService.add({
        severity: 'error',
        summary: 'Sélection hétérogène',
        detail: 'La validation en lot ne peut porter que sur des opérations d\'un même type et '
          + 'd\'un même sens. Videz la sélection et recommencez avec une seule combinaison '
          + 'type/sens à la fois.',
        life: 8000,
      });
      return;
    }
    this.lotForm = this.fb.group({
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      tva: [saisieTvaParDefaut()],
    });
    this.displayLotModal = true;
  }

  isLotInvalid(controlName: string): boolean {
    const control = this.lotForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  closeLotModal(): void {
    this.displayLotModal = false;
  }

  labelSource(source: TransactionSource): string {
    return TransactionSourceLabels[source] ?? source;
  }

  confirmLotValidation(): void {
    if (this.lotForm.invalid) {
      this.lotForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires et corriger les erreurs.',
      });
      return;
    }

    // Garantie par selectionEstHomogene (vérifiée à l'ouverture du dialogue) : au plus un groupe.
    const group = this.lotGroups[0];
    if (!group) {
      this.closeLotModal();
      return;
    }

    const formValue = this.lotForm.getRawValue();
    const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
    const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

    const recapMessage = `
          Êtes-vous sûr de vouloir valider ces ${group.ids.length} opération(s) de type
          <strong>${this.labelSource(group.source)} (${group.sens})</strong> ?
          <br><br>
          <strong>Montant total :</strong> ${this.decimalPipe.transform(group.montantTotal, '1.2-2', 'fr-FR')} MAD
          <br><br>
          <strong>Détails du rapprochement (appliqués à toutes les opérations sélectionnées) :</strong>
          <ul>
              <li><strong>Date de valeur :</strong> ${dateValeurFormatted || '-'}</li>
              <li><strong>Date d'opération :</strong> ${dateOperationFormatted || '-'}</li>
              <li><strong>TVA :</strong> ${decrireSaisieTva(formValue.tva)}</li>
          </ul>
          <br>
          Le libellé et l'observation d'origine de chaque opération sont conservés.
    `;

    this.confirmationService.confirm({
      message: recapMessage,
      header: 'Confirmation de validation en lot',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Confirmer',
      rejectLabel: 'Annuler',
      accept: () => this.submitLot(group),
    });
  }

  /** Un seul appel POST /process — la sélection est garantie homogène (voir openLotModal). */
  private submitLot(group: LotGroup): void {
    const rapprochementId = this.bankStateService.getRapprochementId();
    const formValue = this.lotForm.getRawValue();
    const saisieTva: SaisieTva = formValue.tva!;
    const dateValeur = this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string;
    const dateOperation = this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string;

    const request: BatchValidationRequestDto = {
      transactionSource: group.source,
      sens: group.sens,
      banqueId: this.banqueIdFromRoute,
      ids: group.ids,
      dateValeur,
      dateOperation,
      tauxTva: saisieTva.tauxTva,
      rapprochementId: rapprochementId!,
    };

    this.isLotSubmitting = true;

    this.subscriptions.push(
      this.validationService.valider(request).subscribe({
        next: (response: BatchValidationResponseDto) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: `${response.nombreOperations} opération(s) validée(s) avec succès.`,
          });
          this.isLotSubmitting = false;
          this.selectionState.clear();
          this.closeLotModal();
          this.onFilterChange();
          this.bankStateService.refreshBankInfo(this.banqueIdFromRoute);
          this.bankStateService.notifyValidation();
        },
        error: (err) => {
          this.isLotSubmitting = false;
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: err?.error?.message || 'La validation en lot a échoué.',
          });
        }
      })
    );
  }

  // ==================== Méthodes des cartes (groupement par source) ====================

  /** Charge les indicateurs par source avec les filtres du formulaire (sans restriction de source). */
  loadSourceCards(): void {
    if (!this.banqueIdFromRoute) {
      return;
    }
    this.cardsLoading = true;
    this.subscriptions.push(
      this.recapService.getRecapitulatifParSource(this.banqueIdFromRoute, this.buildFilterDto())
        .subscribe({
          next: (cards: RecapSourceDto[]) => {
            const total = cards.reduce(
              (acc, c) => ({
                count: acc.count + c.operationsCount,
                debit: acc.debit + c.totalDebit,
                credit: acc.credit + c.totalCredit,
                echues: acc.echues + c.echuesCount,
              }),
              { count: 0, debit: 0, credit: 0, echues: 0 });
            this.globalView = cards.length === 0 ? undefined : total;
            this.cardsView = cards.map((c): RecapCardView => ({
              key: c.source,
              label: this.sourceLabel(c.source),
              icon: this.sourceIcon(c.source),
              count: c.operationsCount,
              debit: c.totalDebit,
              credit: c.totalCredit,
              echues: c.echuesCount,
            }));
            this.cardsLoading = false;
            // La source ouverte n'a plus d'encours (tout validé, ou filtres) : on referme la carte.
            if (this.openCard && !this.cardsView.some(c => c.key === this.openCard)) {
              this.closeCard();
            }
          },
          error: () => {
            this.cardsLoading = false;
            this.globalView = undefined;
          }
        })
    );
  }

  /** Ouvre une carte : le tableau existant s'y affiche, restreint à sa source. */
  toggleCard(key: RecapCardKey): void {
    if (this.openCard === key) {
      return;
    }
    this.selectionState.clear();
    this.recapItems = [];
    this.totalRecords = 0;
    this.totalDebit = 0;
    this.totalCredit = 0;
    this.loading = true;
    this.sortParDefaut = false;
    this.openCard = key;
  }

  closeCard(): void {
    this.selectionState.clear();
    this.openCard = null;
  }

  /** Ouverture au clavier (Entrée / Espace) uniquement quand le focus est sur l'en-tête lui-même. */
  onCardKey(event: Event, key: RecapCardKey): void {
    if (event.target !== event.currentTarget) {
      return;
    }
    event.preventDefault();
    this.toggleCard(key);
  }

  /** Vrai si la source de la carte ouverte a un formulaire de création dédié. */
  canAjouterPour(key: RecapCardKey | null): boolean {
    return !!key && !!RecapitulatifComponent.CREATE_ROUTE[key];
  }

  /** Bouton « Ajouter » propre à la carte ouverte — ouvre le formulaire de CETTE source. */
  ajouterPour(key: RecapCardKey | null): void {
    if (!key) {
      return;
    }
    const route = RecapitulatifComponent.CREATE_ROUTE[key];
    if (route) {
      this.router.navigate(route);
    }
  }

  // ==================== Validation unitaire ====================

  private readonly unitValidationConfigs: Map<TransactionSource, UnitValidationConfig<any>> = new Map([
    [TransactionSource.LEASING, {
      fetchDetail: (id: number) => this.echeanceLeasingService.getEcheanceLeasingById(id),
      toCommonDetail: (d: EcheanceLeasingResponse): UnitValidationDetail => ({
        dateEcheance: d.dateEcheance,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montantTTC,
        recapLabel: 'Contrat N°',
        recapValue: d.numeroContrat,
      }),
      tvaInitialValue: { tauxTva: 20, montantTva: null },
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement',
      successDetail: 'Échéance rapprochée avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de cette échéance de leasing.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.LEASING }),
    }],
    [TransactionSource.CMT, {
      fetchDetail: (id: number) => this.echeanceCmtService.getEcheanceCMTById(id),
      toCommonDetail: (d: EcheanceCMTResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEcheance,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montantTtc,
        recapLabel: 'Désignation du crédit',
        recapValue: d.credit.designation,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement CMT',
      successDetail: 'Échéance CMT rapprochée avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de cette échéance CMT.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.CMT }),
    }],
    [TransactionSource.CHEQUE, {
      fetchDetail: (id: number) => this.chequeService.getChequeById(id),
      toCommonDetail: (d: ChequeResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'N° Chèque',
        recapValue: d.numero,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Chèque',
      successDetail: 'Chèque rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce chèque.',
      toRoutingFields: (d: ChequeResponseDto) => ({
        transactionSource: TransactionSource.CHEQUE,
        sens: d.type as unknown as SensOperation,
      }),
    }],
    [TransactionSource.LCN_DECAISSEMENT, {
      fetchDetail: (id: number) => this.lcnService.getLcnById(id),
      toCommonDetail: (d: LcnResponseDTO): UnitValidationDetail => ({
        dateEcheance: d.dateEcheance,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'N° LCN',
        recapValue: d.numero,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement LCN',
      successDetail: 'LCN rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce LCN.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.LCN_DECAISSEMENT }),
    }],
    [TransactionSource.LCN_ENCAISSEMENT, {
      fetchDetail: (id: number) => this.lcnService.getLcnById(id),
      toCommonDetail: (d: LcnResponseDTO): UnitValidationDetail => ({
        dateEcheance: d.dateEcheance,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'N° LCN',
        recapValue: d.numero,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement LCN',
      successDetail: 'LCN rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce LCN.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.LCN_ENCAISSEMENT }),
    }],
    [TransactionSource.LCN_ESCOMPTE, {
      fetchDetail: (id: number) => this.lcnService.getLcnById(id),
      toCommonDetail: (d: LcnResponseDTO): UnitValidationDetail => ({
        dateEcheance: d.dateEcheance,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'N° LCN',
        recapValue: d.numero,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement LCN',
      successDetail: 'LCN rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce LCN.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.LCN_ESCOMPTE }),
    }],
    [TransactionSource.Virement, {
      fetchDetail: (id: number) => this.virementService.getVirementById(id),
      toCommonDetail: (d: VirementResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'N° Virement',
        recapValue: d.numero,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Virement',
      successDetail: 'Virement rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce virement.',
      toRoutingFields: (d: VirementResponseDto) => ({
        transactionSource: TransactionSource.Virement,
        sens: d.type as unknown as SensOperation,
      }),
    }],
    [TransactionSource.PREFINANCEMENT, {
      fetchDetail: (id: number) => this.prefinancementService.getPrefinancementById(id),
      toCommonDetail: (d: PrefinancementResponseDTO): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'N° de Marché',
        recapValue: d.marcheNumero,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Préfinancement',
      successDetail: 'Préfinancement validé et rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce préfinancement.',
      toRoutingFields: (d: PrefinancementResponseDTO) => ({
        transactionSource: TransactionSource.PREFINANCEMENT,
        banqueId: d.banqueId,
      }),
    }],
    [TransactionSource.FACTORING, {
      fetchDetail: (id: number) => this.factoringService.getFactoringById(id),
      toCommonDetail: (d: FactoringResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'N° de Marché',
        recapValue: d.marcheNumero,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Factoring',
      successDetail: 'Factoring validé et rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce factoring.',
      toRoutingFields: (d: FactoringResponseDto) => ({
        transactionSource: TransactionSource.FACTORING,
        banqueId: d.banqueId,
      }),
    }],
    [TransactionSource.DROIT_CONSTATE, {
      fetchDetail: (id: number) => this.droitConstateService.getDroitConstateById(id),
      toCommonDetail: (d: DroitConstateResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'N° de Marché',
        recapValue: d.marcheNumero,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Droit Constaté',
      successDetail: 'Droit constaté validé et rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce droit constaté.',
      toRoutingFields: (d: DroitConstateResponseDto) => ({
        transactionSource: TransactionSource.DROIT_CONSTATE,
        banqueId: d.banqueId,
      }),
    }],
    [TransactionSource.DECOMPTES, {
      fetchDetail: (id: number) => this.decompteService.getDecompteById(id),
      toCommonDetail: (d: DecompteResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'N° de Marché',
        recapValue: d.marcheNumero,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Décompte',
      successDetail: 'Décompte validé et rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce décompte.',
      toRoutingFields: (d: DecompteResponseDto) => ({
        transactionSource: TransactionSource.DECOMPTES,
        banqueId: d.banqueEncaissementId,
      }),
    }],
    [TransactionSource.RESTITUTION, {
      fetchDetail: (id: number) => this.restitutionService.getById(id),
      toCommonDetail: (d: RestitutionResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'ID Restitution',
        recapValue: d.id,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Restitution',
      successDetail: 'Restitution validée avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de cette restitution.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.RESTITUTION }),
    }],
    [TransactionSource.PROVISION, {
      fetchDetail: (id: number) => this.provisionService.getById(id),
      toCommonDetail: (d: ProvisionResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'ID Provision',
        recapValue: d.id,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Provision',
      successDetail: 'Provision validée avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de cette provision.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.PROVISION }),
    }],
    [TransactionSource.IMPAYEE, {
      fetchDetail: (id: number) => this.impayeeService.getImpayeeById(id),
      toCommonDetail: (d: ImpayeeResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'N°',
        recapValue: d.numero ?? d.id,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Impayée',
      successDetail: 'Impayée validée avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de cette impayée.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.IMPAYEE }),
    }],
    [TransactionSource.VIREMENT_INTERNATIONAL, {
      fetchDetail: (id: number) => this.virementInternationalService.getVirementInternationalById(id),
      toCommonDetail: (d: VirementInternationalResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'Bénéficiaire',
        recapValue: d.beneficiaire,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Virement international',
      successDetail: 'Virement international rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce virement international.',
      toRoutingFields: (d: VirementInternationalResponseDto) => ({
        transactionSource: TransactionSource.VIREMENT_INTERNATIONAL,
        sens: d.type as unknown as SensOperation,
      }),
    }],
    [TransactionSource.MiseADisposition, {
      fetchDetail: (id: number) => this.miseADispositionService.getMiseAdispositionById(id),
      toCommonDetail: (d: MiseADispositionResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: d.type === SensType.DECAISSEMENT ? 'N° Opération' : 'Émetteur',
        recapValue: d.type === SensType.DECAISSEMENT ? d.numero : d.emetteur,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Mise à disposition',
      successDetail: 'Mise à disposition rapprochée avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de cette mise à disposition.',
      toRoutingFields: (d: MiseADispositionResponseDto) => ({
        transactionSource: TransactionSource.MiseADisposition,
        sens: d.type as unknown as SensOperation,
      }),
    }],
    [TransactionSource.AGIOS, {
      fetchDetail: (id: number) => this.agiosService.getById(id),
      toCommonDetail: (d: AgiosResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'ID Agios',
        recapValue: d.id,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Agios',
      successDetail: 'Agios rapprochés avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de cet agios.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.AGIOS }),
    }],
    [TransactionSource.COMMISSION, {
      fetchDetail: (id: number) => this.commissionService.getCommissionById(id),
      toCommonDetail: (d: CommissionResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'ID Commission',
        recapValue: d.id,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Commission',
      successDetail: 'Commission rapprochée avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de cette commission.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.COMMISSION }),
    }],
    [TransactionSource.FRAIS_BANCAIRE, {
      fetchDetail: (id: number) => this.fraisBancaireService.getById(id),
      toCommonDetail: (d: FraisBancaireResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant as unknown as number,
        recapLabel: 'ID Frais',
        recapValue: d.id,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Frais bancaire',
      successDetail: 'Frais bancaire rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce frais bancaire.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.FRAIS_BANCAIRE }),
    }],
    [TransactionSource.PRELEVEMENT, {
      fetchDetail: (id: number) => this.prelevementService.getById(id),
      toCommonDetail: (d: PrelevementResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant as unknown as number,
        recapLabel: 'ID Prélèvement',
        recapValue: d.id,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Prélèvement',
      successDetail: 'Prélèvement rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce prélèvement.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.PRELEVEMENT }),
    }],
    [TransactionSource.IMPOT_TAXE, {
      fetchDetail: (id: number) => this.impotEtTaxeService.getImpotEtTaxeById(id),
      toCommonDetail: (d: ImpotEtTaxeResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'ID Impôt/Taxe',
        recapValue: d.id,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Impôt/Taxe',
      successDetail: 'Impôt/Taxe rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de cet impôt/taxe.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.IMPOT_TAXE }),
    }],
    [TransactionSource.OPERATION_ERRONE, {
      fetchDetail: (id: number) => this.operationErroneService.getById(id),
      toCommonDetail: (d: OperationErroneResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: 'ID Opération',
        recapValue: d.id,
      }),
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Opération erronée',
      successDetail: 'Opération erronée rapprochée avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de cette opération erronée.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.OPERATION_ERRONE }),
    }],
  ]);

  displayUnitValidationModal = false;
  unitValidationForm!: FormGroup;
  unitValidationDetail: UnitValidationDetail | null = null;
  isUnitValidationLoading = false;
  isUnitValidationSubmitting = false;

  private unitValidationRawDetail: any = null;
  private activeUnitValidationConfig: UnitValidationConfig<any> | null = null;

  get montantMaxTvaUnitValidation(): number | null {
    return this.unitValidationDetail?.montantTTC ?? null;
  }

  canValidateUnitairement(item: RecaputilatifItem): boolean {
    return this.unitValidationConfigs.has(item.source);
  }

  // ==================== Ajouter / Éditer ====================

  private static readonly CREATE_EDIT_ELIGIBILITY: Partial<Record<TransactionSource, 'always' | 'encaissement-only'>> = {
    [TransactionSource.Virement]: 'encaissement-only',
    [TransactionSource.CHEQUE]: 'encaissement-only',
    [TransactionSource.LCN_ENCAISSEMENT]: 'always',
    [TransactionSource.LCN_ESCOMPTE]: 'always',
    [TransactionSource.VIREMENT_INTERNATIONAL]: 'always',
    [TransactionSource.PREFINANCEMENT]: 'always',
    [TransactionSource.DROIT_CONSTATE]: 'always',
    [TransactionSource.FACTORING]: 'always',
    [TransactionSource.DECOMPTES]: 'always',
    [TransactionSource.RESTITUTION]: 'always',
    [TransactionSource.IMPAYEE]: 'always',
    [TransactionSource.PROVISION]: 'always',
    [TransactionSource.COMMISSION]: 'always',
    [TransactionSource.PRELEVEMENT]: 'always',
    [TransactionSource.FRAIS_BANCAIRE]: 'always',
    [TransactionSource.OPERATION_ERRONE]: 'always',
    [TransactionSource.IMPOT_TAXE]: 'always',
    [TransactionSource.LEASING]: 'always',
    [TransactionSource.CMT]: 'always',
  };

  private static readonly EDIT_REQUIRES_SYSTEM_ADMIN: Partial<Record<TransactionSource, true>> = {
    [TransactionSource.LEASING]: true,
    [TransactionSource.CMT]: true,
  };

  private static readonly EDIT_ROUTE: Partial<Record<TransactionSource, (id: number) => any[]>> = {
    [TransactionSource.Virement]: id => ['/portail/Virements/edit', id],
    [TransactionSource.CHEQUE]: id => ['/portail/Cheques/edit', id],
    [TransactionSource.LCN_ENCAISSEMENT]: id => ['/portail/LCN/edit', id],
    [TransactionSource.LCN_ESCOMPTE]: id => ['/portail/LCN/edit', id],
    [TransactionSource.VIREMENT_INTERNATIONAL]: id => ['/portail/virements-internationaux/edit', id],
    [TransactionSource.LEASING]: id => ['/portail/leasing/echeances/edit', id],
    [TransactionSource.CMT]: id => ['/portail/cmt/echeances/edit', id],
    [TransactionSource.PREFINANCEMENT]: id => ['/portail/préfinancements/edit', id],
    [TransactionSource.DROIT_CONSTATE]: id => ['/portail/droits-constate/edit', id],
    [TransactionSource.FACTORING]: id => ['/portail/factoring/edit', id],
    [TransactionSource.DECOMPTES]: id => ['/portail/decomptes/edit', id],
    [TransactionSource.RESTITUTION]: id => ['/portail/restitutions/edit', id],
    [TransactionSource.IMPAYEE]: id => ['/portail/impayees/edit', id],
    [TransactionSource.PROVISION]: id => ['/portail/provisions/edit', id],
    [TransactionSource.COMMISSION]: id => ['/portail/commissions/edit', id],
    [TransactionSource.PRELEVEMENT]: id => ['/portail/prelevements/edit', id],
    [TransactionSource.FRAIS_BANCAIRE]: id => ['/portail/frais-bancaire/edit', id],
    [TransactionSource.OPERATION_ERRONE]: id => ['/portail/operations/errones/edit', id],
    [TransactionSource.IMPOT_TAXE]: id => ['/portail/impots-taxes/edit', id],
  };

  private static readonly CREATE_ROUTE: Partial<Record<TransactionSource, any[]>> = {
    [TransactionSource.Virement]: ['/portail/Virements/new'],
    [TransactionSource.CHEQUE]: ['/portail/Cheques/new'],
    [TransactionSource.LCN_ENCAISSEMENT]: ['/portail/LCN/new'],
    // Même écran LCN que l'encaissement (cf. EDIT_ROUTE qui partage '/portail/LCN/edit').
    [TransactionSource.LCN]: ['/portail/LCN/new'],
    [TransactionSource.LCN_DECAISSEMENT]: ['/portail/LCN/new'],
    [TransactionSource.LCN_ESCOMPTE]: ['/portail/LCN/new'],
    [TransactionSource.VIREMENT_INTERNATIONAL]: ['/portail/virements-internationaux/new'],
    [TransactionSource.PREFINANCEMENT]: ['/portail/préfinancements/new'],
    [TransactionSource.DROIT_CONSTATE]: ['/portail/droits-constate/new'],
    [TransactionSource.FACTORING]: ['/portail/factoring/new'],
    [TransactionSource.DECOMPTES]: ['/portail/decomptes/new'],
    [TransactionSource.RESTITUTION]: ['/portail/restitutions/new'],
    [TransactionSource.IMPAYEE]: ['/portail/impayees/new'],
    [TransactionSource.PROVISION]: ['/portail/provisions/new'],
    [TransactionSource.COMMISSION]: ['/portail/commissions/new'],
    [TransactionSource.PRELEVEMENT]: ['/portail/prelevements/new'],
    [TransactionSource.FRAIS_BANCAIRE]: ['/portail/frais-bancaire/new'],
    [TransactionSource.OPERATION_ERRONE]: ['/portail/operations/errones/new'],
    [TransactionSource.IMPOT_TAXE]: ['/portail/impots-taxes/new'],
    // TODO: route supposée — à vérifier dans le routing de l'application.
    [TransactionSource.AGIOS]: ['/portail/agios/new'],
  };

  canEditFromRecap(item: RecaputilatifItem): boolean {
    const regle = RecapitulatifComponent.CREATE_EDIT_ELIGIBILITY[item.source];
    if (!regle) return false;
    if (RecapitulatifComponent.EDIT_REQUIRES_SYSTEM_ADMIN[item.source] && !this.authService.isSystemAdmin()) {
      return false;
    }
    return regle === 'always' || item.sens === SensOperation.ENCAISSEMENT;
  }

  editerItem(item: RecaputilatifItem): void {
    const buildRoute = RecapitulatifComponent.EDIT_ROUTE[item.source];
    if (!buildRoute) return;
    this.router.navigate(buildRoute(item.id));
  }

  private static readonly AJOUTER_ICON: Partial<Record<TransactionSource, string>> = {
    [TransactionSource.Virement]: 'pi pi-sync',
    [TransactionSource.CHEQUE]: 'pi pi-money-bill',
    [TransactionSource.LCN_ENCAISSEMENT]: 'pi pi-file',
    [TransactionSource.VIREMENT_INTERNATIONAL]: 'pi pi-globe',
    [TransactionSource.PREFINANCEMENT]: 'pi pi-briefcase',
    [TransactionSource.DROIT_CONSTATE]: 'pi pi-verified',
    [TransactionSource.FACTORING]: 'pi pi-briefcase',
    [TransactionSource.DECOMPTES]: 'pi pi-briefcase',
    [TransactionSource.RESTITUTION]: 'pi pi-replay',
    [TransactionSource.IMPAYEE]: 'pi pi-exclamation-triangle',
    [TransactionSource.PROVISION]: 'pi pi-shield',
    [TransactionSource.COMMISSION]: 'pi pi-percentage',
    [TransactionSource.PRELEVEMENT]: 'pi pi-credit-card',
    [TransactionSource.FRAIS_BANCAIRE]: 'pi pi-wallet',
    [TransactionSource.OPERATION_ERRONE]: 'pi pi-exclamation-circle',
    [TransactionSource.IMPOT_TAXE]: 'pi pi-book',
  };

  /** Sources masquées dans la modale « Ajouter » (pas de création depuis le récapitulatif). */
  private static readonly AJOUTER_EXCLUDED: ReadonlySet<TransactionSource> = new Set<TransactionSource>([
    TransactionSource.LEASING,
    TransactionSource.CMT,
    TransactionSource.MiseADisposition,
    TransactionSource.CAUTION,
  ]);

  /** Minuscules + sans accents : « Chèque » se trouve en tapant « cheque ». */
  private static normalize(text: string): string {
    return text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  }

  /**
   * Modale « Ajouter » : TOUTES les sources de l'énumération, pas seulement celles qui ont une route
   * de création. Une source sans entrée dans CREATE_ROUTE reste visible mais non cliquable
   * (`route: null`) — il suffit d'ajouter sa route dans CREATE_ROUTE pour l'activer, sans toucher ici.
   */
  readonly ajouterItems: {
    source: TransactionSource;
    label: string;
    icon: string;
    route: any[] | null;
    searchText: string;
  }[] = (Object.values(TransactionSource) as TransactionSource[])
    .filter(source => !RecapitulatifComponent.AJOUTER_EXCLUDED.has(source))
    .map(source => {
      const label = TransactionSourceLabels[source] ?? source;
      return {
        source,
        label,
        icon: this.sourceIcon(source),
        route: RecapitulatifComponent.CREATE_ROUTE[source] ?? null,
        searchText: RecapitulatifComponent.normalize(`${label} ${source}`),
      };
    })
    .sort((a, b) => a.label.localeCompare(b.label, 'fr'));

  displayAjouterModal = false;
  ajouterSearchTerm = '';

  @ViewChild('ajouterSearchInput') private ajouterSearchInputRef?: ElementRef<HTMLInputElement>;

  get filteredAjouterItems() {
    const terme = RecapitulatifComponent.normalize(this.ajouterSearchTerm.trim());
    if (!terme) return this.ajouterItems;
    return this.ajouterItems.filter(item => item.searchText.includes(terme));
  }

  openAjouterModal(): void {
    this.ajouterSearchTerm = '';
    this.displayAjouterModal = true;
  }

  closeAjouterModal(): void {
    this.displayAjouterModal = false;
  }

  onAjouterModalShow(): void {
    this.ajouterSearchInputRef?.nativeElement.focus();
  }

  choisirAjouter(item: { route: any[] | null }): void {
    if (!item.route) {
      return;
    }
    this.closeAjouterModal();
    this.router.navigate(item.route);
  }

  /** Entrée dans le champ de recherche : ouvre la première source créable de la liste filtrée. */
  onAjouterEnter(): void {
    const premiere = this.filteredAjouterItems.find(item => item.route);
    if (premiere) {
      this.choisirAjouter(premiere);
    }
  }

  openUnitValidation(item: RecaputilatifItem): void {
    const config = this.unitValidationConfigs.get(item.source);
    if (!config) {
      return;
    }

    this.activeUnitValidationConfig = config;
    this.isUnitValidationLoading = true;
    this.displayUnitValidationModal = true;
    this.unitValidationDetail = null;
    this.unitValidationRawDetail = null;

    this.subscriptions.push(
      config.fetchDetail(item.id).subscribe({
        next: (rawDetail) => {
          this.unitValidationRawDetail = rawDetail;
          const detail = config.toCommonDetail(rawDetail);
          this.unitValidationDetail = detail;
          this.unitValidationForm = this.fb.group({
            dateEmission: [{ value: parseLocalDate(detail.dateEcheance), disabled: true }, Validators.required],
            dateValeur: [null, Validators.required],
            dateOperation: [null, Validators.required],
            observation: [detail.observation, Validators.required],
            libelle: [detail.libelle, Validators.required],
            tva: [config.tvaLocked
              ? { value: config.tvaInitialValue, disabled: true }
              : config.tvaInitialValue],
          });
          this.isUnitValidationLoading = false;
        },
        error: () => {
          this.isUnitValidationLoading = false;
          this.displayUnitValidationModal = false;
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: config.fetchErrorDetail,
          });
        }
      })
    );
  }

  closeUnitValidationModal(): void {
    this.displayUnitValidationModal = false;
    this.unitValidationDetail = null;
    this.unitValidationRawDetail = null;
    this.activeUnitValidationConfig = null;
  }

  isUnitValidationInvalid(controlName: string): boolean {
    const control = this.unitValidationForm?.get(controlName);
    if (control && control.disabled) {
      return false;
    }
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  confirmUnitValidation(): void {
    if (this.unitValidationForm.invalid || !this.unitValidationDetail || !this.activeUnitValidationConfig) {
      this.unitValidationForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires et corriger les erreurs.',
      });
      return;
    }

    const formValue = this.unitValidationForm.getRawValue();
    const detail = this.unitValidationDetail;
    const config = this.activeUnitValidationConfig;
    const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
    const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
    const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

    const recapMessage = `
          Êtes-vous sûr de vouloir rapprocher cette échéance ?
          <br><br>
          <strong>${detail.recapLabel}:</strong> ${detail.recapValue}
          <br>
          <strong>Montant TTC:</strong> ${this.decimalPipe.transform(detail.montantTTC, '1.2-2', 'fr-FR')} MAD
          <br><br>
          <strong>Détails du Rapprochement:</strong>
          <ul>
              <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
              <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
              <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
              <li><strong>Libellé:</strong> ${formValue.libelle}</li>
              <li><strong>Observation:</strong> ${formValue.observation}</li>
              <li><strong>TVA Déductible:</strong> ${decrireSaisieTva(formValue.tva)}</li>
          </ul>
    `;

    this.confirmationService.confirm({
      message: recapMessage,
      header: config.confirmHeader,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Confirmer',
      rejectLabel: 'Annuler',
      accept: () => this.submitUnitValidation(),
    });
  }

  private submitUnitValidation(): void {
    if (!this.unitValidationRawDetail || !this.activeUnitValidationConfig) return;

    const formValue = this.unitValidationForm.getRawValue();
    const rapprochementId = this.bankStateService.getRapprochementId();
    const config = this.activeUnitValidationConfig;
    const saisieTva: SaisieTva = formValue.tva;

    const request: BatchValidationRequestDto = {
      ...config.toRoutingFields(this.unitValidationRawDetail),
      ids: [this.unitValidationRawDetail.id],
      dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
      dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
      tauxTva: saisieTva.tauxTva,
      montantTva: saisieTva.montantTva,
      observation: formValue.observation,
      libelle: formValue.libelle,
      rapprochementId: rapprochementId!,
    };

    this.isUnitValidationSubmitting = true;

    this.subscriptions.push(
      this.validationService.valider(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: config.successDetail,
          });
          this.isUnitValidationSubmitting = false;
          this.closeUnitValidationModal();
          this.onFilterChange();
          this.bankStateService.refreshBankInfo(this.banqueIdFromRoute);
          this.bankStateService.notifyValidation();
        },
        error: () => {
          this.isUnitValidationSubmitting = false;
        }
      })
    );
  }

  // ==================== Lifecycle & Filtres ====================

  ngOnInit(): void {
    this.initSearchForm();
    this.populateTransactionSourceOptions();

    this.subscriptions.push(
      this.route.parent!.paramMap.subscribe(params => {
        const id = params.get('banqueId');
        if (id) {
          this.banqueIdFromRoute = +id;
          this.closeCard();
          this.loadSourceCards();
        }
      })
    );
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      transactionSources: [[]],
      libelleOuObservation: [''],
      numero: [''],
      sens: [null],
      dateReferenceDepuis: [null],
      dateReferenceJusqua: [null],
      montant: [null],
      commissionOperation: [null],
      impotEtTaxeMotif: [null]
    });

    this.subscriptions.push(
      this.searchForm.valueChanges
        .pipe(debounceTime(300), distinctUntilChanged())
        .subscribe(() => this.onFilterChange())
    );
  }

  populateTransactionSourceOptions(): void {
    this.transactionSourceOptions = Object.values(TransactionSource)
      .map(source => ({
        label: TransactionSourceLabels[source] ?? source,
        value: source
      }))
      .sort((a, b) => a.label.localeCompare(b.label, 'fr'));
  }

  get showCommissionMotifFilter(): boolean {
    const sources: TransactionSource[] = this.searchForm?.get('transactionSources')?.value ?? [];
    return sources.includes(TransactionSource.COMMISSION);
  }

  get showImpotTaxeMotifFilter(): boolean {
    const sources: TransactionSource[] = this.searchForm?.get('transactionSources')?.value ?? [];
    return sources.includes(TransactionSource.IMPOT_TAXE);
  }

  onFilterChange(): void {
    this.loadSourceCards();
    if (this.dt) {
      this.dt.first = 0;
      this.loadRecapitulatif(this.dt.createLazyLoadMetadata());
    }
  }

  loadRecapitulatif(event: TableLazyLoadEvent): void {
    if (!this.banqueIdFromRoute) {
      return;
    }

    this.loading = true;

    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sort = event.sortField && event.sortOrder
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateReference,desc';
    this.sortParDefaut = !event.sortField;

    const searchDto = this.buildSearchDto();

    this.subscriptions.push(
      this.recapService.getRecapitulatif(this.banqueIdFromRoute, searchDto, page, size, sort)
        .subscribe({
          next: (response: AgregatedRecapDto) => {
            this.recapItems = response.page.content.map(item => ({
              ...item,
              compositeKey: `${item.source}:${item.id}`,
            }));
            this.totalRecords = response.page.page.totalElements;
            this.loading = false;
            this.totalCredit = response.totalCredit;
            this.totalDebit = response.totalDebit;
          },
          error: () => {
            this.loading = false;
          }
        })
    );
  }

  exportPdf(): void {
    if (!this.banqueIdFromRoute) {
      return;
    }
    this.exporting = true;
    const searchDto = this.buildSearchDto();

    this.recapService.exportRecapitulatifPdf(this.banqueIdFromRoute, searchDto).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `recapitulatif_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  clearSearch(): void {
    this.searchForm.reset({
      transactionSources: [],
      libelleOuObservation: '',
      numero: '',
      sens: null,
      dateReferenceDepuis: null,
      dateReferenceJusqua: null,
      montant: null,
      commissionOperation: null,
      impotEtTaxeMotif: null
    });
  }

  hasActiveFilters(): boolean {
    if (!this.searchForm) {
      return false;
    }
    const value = this.searchForm.value;
    return !!(value.libelleOuObservation || value.numero || value.sens || value.montant
      || value.dateReferenceDepuis || value.dateReferenceJusqua
      || value.commissionOperation || value.impotEtTaxeMotif
      || (value.transactionSources && value.transactionSources.length > 0));
  }

  get activeFilterChips(): string[] {
    if (!this.searchForm) {
      return [];
    }
    const value = this.searchForm.value;
    const chips: string[] = [];

    if (value.sens) {
      const option = this.sensOptions.find(o => o.value === value.sens);
      chips.push('Sens : ' + (option ? option.label : value.sens));
    }
    if (value.transactionSources && value.transactionSources.length > 0) {
      chips.push(value.transactionSources.length === 1
        ? 'Type : ' + (TransactionSourceLabels[value.transactionSources[0] as TransactionSource]
        ?? value.transactionSources[0])
        : 'Types : ' + value.transactionSources.length);
    }
    if (value.libelleOuObservation) {
      chips.push('Libellé / Observation : ' + value.libelleOuObservation);
    }
    if (value.numero) {
      chips.push('N° : ' + value.numero);
    }
    if (value.montant !== null && value.montant !== undefined && value.montant !== '') {
      chips.push('Montant : ' + value.montant);
    }
    if (value.commissionOperation) {
      const option = this.commissionOperationOptions.find(o => o.value === value.commissionOperation);
      chips.push('Motif Commission : ' + (option ? option.label : value.commissionOperation));
    }
    if (value.impotEtTaxeMotif) {
      const option = this.motifOptions.find(o => o.value === value.impotEtTaxeMotif);
      chips.push('Motif Impôt/Taxe : ' + (option ? option.label : value.impotEtTaxeMotif));
    }
    if (this.activeDateFilterCount > 0) {
      chips.push('Période : ' + this.activeDateFilterCount
        + (this.activeDateFilterCount > 1 ? ' bornes' : ' borne'));
    }
    return chips;
  }

  get activeFilterCount(): number {
    return this.activeFilterChips.length;
  }

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    if (!this.searchForm) {
      return 0;
    }
    const value = this.searchForm.value;
    return (value.dateReferenceDepuis ? 1 : 0) + (value.dateReferenceJusqua ? 1 : 0);
  }

  /** Critères du formulaire seuls — alimentent les cartes (aucune restriction de source). */
  private buildFilterDto(): RecapitulatifSearchDto {
    return {
      transactionSources: this.searchForm.get('transactionSources')?.value,
      libelleOuObservation: this.searchForm.get('libelleOuObservation')?.value,
      numero: this.searchForm.get('numero')?.value,
      sens: this.searchForm.get('sens')?.value,
      dateReferenceDepuis: this.searchForm.get('dateReferenceDepuis')?.value,
      dateReferenceJusqua: this.searchForm.get('dateReferenceJusqua')?.value,
      montant: this.searchForm.get('montant')?.value,
      commissionOperation: this.searchForm.get('commissionOperation')?.value,
      impotEtTaxeMotif: this.searchForm.get('impotEtTaxeMotif')?.value
    };
  }

  /**
   * Critères du tableau et de l'export : ceux du formulaire, restreints à la source de la carte
   * ouverte. Partagé entre chargement et export — l'export PDF reflète donc la carte ouverte.
   */
  private buildSearchDto(): RecapitulatifSearchDto {
    const dto = this.buildFilterDto();
    if (this.openCard) {
      return { ...dto, transactionSources: [this.openCard] };
    }
    return dto;
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
}
