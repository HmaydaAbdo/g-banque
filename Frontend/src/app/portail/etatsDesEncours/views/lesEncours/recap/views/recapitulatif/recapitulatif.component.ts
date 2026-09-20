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

  /** Barre de filtres repliee par defaut : le tableau prend toute la hauteur au chargement. */
  showFilters = false;

  /** Panneau des bornes de date, replie par defaut pour laisser la place au tableau. */
  showDatePanel = false;

  @ViewChild('dt') dt: Table | undefined;

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

  // ==================== Validation unitaire ====================
  // Un seul dialogue générique (voir UnitValidationConfig plus haut), piloté par la Map
  // ci-dessous : les 22 sources y sont câblées. Décision du 14/09/2026 (remplace les deux
  // dialogues Leasing/CMT distincts du pilote initial) : la quasi-totalité du formulaire (dates,
  // libellé, observation, TVA) est identique d'une source à l'autre ; seules la forme du DTO de
  // détail et le routage (source/sens/banqueId) varient — capturés ici plutôt que dupliqués dans
  // un dialogue par source. Depuis la fusion du 19/09/2026 de la validation unitaire dans
  // BatchValidationRequestDto (architecture.md, Règle 1), chaque config ne construit même plus sa
  // propre requête : `toRoutingFields` ne fournit que le routage, `submitUnitValidation`
  // construit la requête complète une seule fois pour toutes les sources.

  /**
   * Une config par source câblée. `any` assumé au niveau de la Map (voir UnitValidationConfig) —
   * chaque entrée reste fortement typée sur son propre DTO de détail.
   */
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
      // TVA éditable : 20% par défaut, mais l'utilisateur peut choisir « aucun » (0%) ou saisir
      // un montant libre via le dropdown TAUX_TVA (décision HMAYDA du 14/09/2026, qui remplace
      // le verrouillage à 20% du pilote initial).
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
      // TVA libre/optionnelle, comme sur l'ancien écran dédié CMT (défaut 0%, taux OU montant).
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
      // TVA libre/optionnelle, comme sur les 2 écrans dédiés Chèque actuels (défaut 0%, taux OU montant).
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Chèque',
      successDetail: 'Chèque rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce chèque.',
      // `sens` reste obligatoire côté backend (homogénéité de la sélection en lot), même si le
      // handler ne le consomme plus depuis la fusion du 19/09/2026 — lu sur `d.type`.
      toRoutingFields: (d: ChequeResponseDto) => ({
        transactionSource: TransactionSource.CHEQUE,
        sens: d.type as unknown as SensOperation,
      }),
    }],
    // LCN : contrairement à Cheque/Virement, le sens (et la nature encaissement/escompte) est déjà
    // porté par la source elle-même (LCN_DECAISSEMENT / LCN_ENCAISSEMENT / LCN_ESCOMPTE, voir
    // v_recapitulatif_encours) — pas de champ `sens` à transmettre, chaque entrée route
    // directement vers son propre TransactionSource.
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
      // TVA libre/optionnelle, comme sur les 2 écrans dédiés LCN actuels (défaut 0%, taux OU montant).
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement LCN',
      successDetail: 'LCN rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce LCN.',
      toRoutingFields: () => ({ transactionSource: TransactionSource.LCN_DECAISSEMENT }),
    }],
    // LCN_ENCAISSEMENT et LCN_ESCOMPTE partagent la même configuration, à la source près.
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
      // TVA libre/optionnelle, comme sur les 2 écrans dédiés Virement actuels (défaut 0%, taux OU montant).
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Virement',
      successDetail: 'Virement rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce virement.',
      // `sens` reste obligatoire côté backend (homogénéité de la sélection en lot) — lu sur `d.type`.
      toRoutingFields: (d: VirementResponseDto) => ({
        transactionSource: TransactionSource.Virement,
        sens: d.type as unknown as SensOperation,
      }),
    }],
    // Préfinancement/Factoring/Droit constaté/Décomptes : sens (remboursement/déblocage) porté par
    // l'entité elle-même (champ `type`, ou fixe pour Décomptes), jamais un choix fait à la
    // validation — aucun `sens` à transmettre ici (contrairement à Cheque/Virement). `banqueId`
    // vient du détail lui-même (`d.banqueId` / `d.banqueEncaissementId` pour Décomptes) : c'est la
    // banque du marché (Préfinancement/Factoring), celle propre à l'entité (Droit constaté) ou la
    // banque d'encaissement (Décomptes), déjà résolue côté backend dans le DTO de détail.
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
    // Restitution/Provision/Impayée : même principe que Préfinancement & co (sens porté par
    // l'entité, aucun `sens`/`banqueId` à transmettre) — la banque est résolue côté backend
    // depuis l'entité elle-même (`Restitution.banque`/`Provision.banque`/`Impayee.banque`), jamais
    // transmise par l'appelant. Pas de numéro de pièce naturel pour Restitution/Provision
    // (contrairement aux sources marché) : recapLabel/recapValue retombent sur l'ID, comme
    // l'affichent déjà leurs écrans dédiés respectifs. Impayée a un `numero`.
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
      // TVA libre/optionnelle, comme sur les 2 écrans dédiés Virement international actuels (défaut 0%, taux OU montant).
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Virement international',
      successDetail: 'Virement international rapproché avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de ce virement international.',
      // `sens` reste obligatoire côté backend (homogénéité de la sélection en lot) — lu sur `d.type`.
      toRoutingFields: (d: VirementInternationalResponseDto) => ({
        transactionSource: TransactionSource.VIREMENT_INTERNATIONAL,
        sens: d.type as unknown as SensOperation,
      }),
    }],
    [TransactionSource.MiseADisposition, {
      fetchDetail: (id: number) => this.miseADispositionService.getMiseAdispositionById(id),
      // Identifiant métier différent selon le sens, comme les 2 écrans dédiés actuels : le
      // décaissement affiche le n° d'opération, l'encaissement l'émetteur.
      toCommonDetail: (d: MiseADispositionResponseDto): UnitValidationDetail => ({
        dateEcheance: d.dateEmission,
        observation: d.observation,
        libelle: d.libelle,
        montantTTC: d.montant,
        recapLabel: d.type === SensType.DECAISSEMENT ? 'N° Opération' : 'Émetteur',
        recapValue: d.type === SensType.DECAISSEMENT ? d.numero : d.emetteur,
      }),
      // TVA libre/optionnelle, comme sur les 2 écrans dédiés Mise à disposition actuels (défaut 0%, taux OU montant).
      tvaInitialValue: saisieTvaParDefaut(),
      tvaLocked: false,
      confirmHeader: 'Confirmation de Rapprochement Mise à disposition',
      successDetail: 'Mise à disposition rapprochée avec succès.',
      fetchErrorDetail: 'Impossible de récupérer le détail de cette mise à disposition.',
      // `sens` reste obligatoire côté backend (homogénéité de la sélection en lot) — lu sur `d.type`
      // (le même champ sert aussi à choisir le libellé d'affichage ci-dessus, à but différent).
      toRoutingFields: (d: MiseADispositionResponseDto) => ({
        transactionSource: TransactionSource.MiseADisposition,
        sens: d.type as unknown as SensOperation,
      }),
    }],
    // Agios/Commission/Frais bancaire/Prélèvement/Impôt-Taxe/Opération erronée : toujours un
    // décaissement (sens fixe), aucun `sens`/`banqueId` à transmettre — même principe que
    // Restitution/Provision/Impayée. Aucune de ces 6 sources n'a d'identifiant métier naturel
    // distinct de son id (comme leurs écrans dédiés actuels, dont la boîte de confirmation
    // retombe elle aussi sur l'id) : recapLabel/recapValue retombent sur l'id.
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

  /** DTO brut spécifique à la source active — passé tel quel à `config.toRoutingFields`. */
  private unitValidationRawDetail: any = null;
  private activeUnitValidationConfig: UnitValidationConfig<any> | null = null;

  get montantMaxTvaUnitValidation(): number | null {
    return this.unitValidationDetail?.montantTTC ?? null;
  }

  /** Vrai si la source de cette ligne a une config enregistrée — pilote le bouton « Valider ». */
  canValidateUnitairement(item: RecaputilatifItem): boolean {
    return this.unitValidationConfigs.has(item.source);
  }

  // ==================== Ajouter / Éditer (matrice HMAYDA du 20/09/2026) ====================
  // Avant la migration vers ce récap unifié, Ajouter/Éditer vivaient dans les anciens écrans
  // dédiés par (source, sens) et redirigeaient déjà vers une page de formulaire séparée (jamais
  // une modale, sauf Leasing/CMT dont l'édition d'échéance est un dialogue sur l'écran de tableau
  // d'amortissement — hors périmètre ici, ces deux sources ne sont pas éligibles). On réutilise
  // ces mêmes pages telles quelles : ce récap ne fait que router vers elles, jamais reconstruire
  // un formulaire. Le retour au récap après un Ajouter/Éditer réussi n'est pas géré ici non plus —
  // chaque formulaire appelle déjà `Location.back()` en cas de succès (pattern préexistant sur
  // ~20 des 21 formulaires), donc pousser une entrée d'historique via `router.navigate` suffit.
  //
  // Seules les sources listées ci-dessous sont éligibles à Ajouter/Éditer depuis le récap ('always'
  // = les deux sens, 'encaissement-only' = seul l'encaissement l'est) ; toute source absente de
  // cette table (MiseADisposition, Agios, LCN_DECAISSEMENT, etc.) n'affiche ni Ajouter ni Éditer,
  // exactement comme dans les anciens écrans — Exporter et Valider restent disponibles pour toutes
  // les sources, la matrice ne les restreint jamais.
  // LCN_ESCOMPTE réutilise exactement le même formulaire/id-space que LCN_ENCAISSEMENT
  // (LcnFormComponent, un seul contrôle `natureLcn` Encaissement/Escompte) : aucune restriction
  // de sens n'a de sens ici, donc 'always'.
  // Leasing/CMT (21/09/2026, demande HMAYDA) : Éditer seulement, pas Ajouter (aucun endpoint de
  // création d'une échéance seule n'existe — l'échéancier n'est généré qu'à la création du contrat
  // entier) — ces deux sources ne sont donc volontairement PAS ajoutées à `CREATE_ROUTE` plus bas.
  // L'édition elle-même reste réservée aux administrateurs système (voir `canEditFromRecap`), comme
  // l'était l'ancien dialogue sur l'écran de tableau d'amortissement.
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

  /** Sources dont l'édition est réservée aux administrateurs système, en plus de la matrice
   * ci-dessus — reprend exactement la restriction déjà posée par le backend (`PUT
   * /echeances-leasing|cmt/{id}` est `ADMIN_ONLY`) et par l'ancien dialogue d'édition. */
  private static readonly EDIT_REQUIRES_SYSTEM_ADMIN: Partial<Record<TransactionSource, true>> = {
    [TransactionSource.LEASING]: true,
    [TransactionSource.CMT]: true,
  };

  /** Route d'édition (avec id) par source — mêmes pages que les anciens écrans dédiés. */
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

  /** Route de création (sans id) par source — le sens, quand il est un choix, se fait dans le formulaire.
   * LCN_ESCOMPTE n'a volontairement pas sa propre entrée : elle pointerait vers exactement la même
   * route que LCN_ENCAISSEMENT (`LcnFormComponent`, contrôle `natureLcn`) — une seule carte « LCN »
   * dans la modale Ajouter suffit, la nature se choisissant dans le formulaire (demande HMAYDA,
   * 21/09/2026 : « both lcn point to same route so let's just have one card »). */
  private static readonly CREATE_ROUTE: Partial<Record<TransactionSource, any[]>> = {
    [TransactionSource.Virement]: ['/portail/Virements/new'],
    [TransactionSource.CHEQUE]: ['/portail/Cheques/new'],
    [TransactionSource.LCN_ENCAISSEMENT]: ['/portail/LCN/new'],
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
  };

  /** Vrai si cette ligne peut être éditée depuis le récap — pilote le bouton « Éditer ». */
  canEditFromRecap(item: RecaputilatifItem): boolean {
    const regle = RecapitulatifComponent.CREATE_EDIT_ELIGIBILITY[item.source];
    if (!regle) return false;
    if (RecapitulatifComponent.EDIT_REQUIRES_SYSTEM_ADMIN[item.source] && !this.authService.isSystemAdmin()) {
      return false;
    }
    return regle === 'always' || item.sens === SensOperation.ENCAISSEMENT;
  }

  /** Redirige vers la page d'édition dédiée de la source — le retour au récap est géré par le
   * `Location.back()` déjà appelé par ce formulaire en cas de succès. */
  editerItem(item: RecaputilatifItem): void {
    const buildRoute = RecapitulatifComponent.EDIT_ROUTE[item.source];
    if (!buildRoute) return;
    this.router.navigate(buildRoute(item.id));
  }

/** Icône par source pour la modale « Ajouter » — purement visuel, aucune valeur métier. */
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

  /**
   * Libellés raccourcis pour la modale « Ajouter » uniquement — le libellé complet
   * (`TransactionSourceLabels`) reste utilisé partout ailleurs (tableau, badges, filtres) où il
   * distingue Encaissement/Décaissement/Escompte. LCN Encaissement est raccourci en "LCN" ici : sa
   * carte est la seule entrée LCN de la modale (couvre aussi la création d'un LCN Escompte, la
   * nature se choisissant dans le formulaire), donc pas d'ambiguïté possible.
   */
  private static readonly AJOUTER_LABEL_OVERRIDE: Partial<Record<TransactionSource, string>> = {
    [TransactionSource.LCN_ENCAISSEMENT]: 'LCN',
  };

  /** Une entrée « Ajouter » par source éligible — liste fixe, calculée une seule fois. */
  readonly ajouterItems: { source: TransactionSource; label: string; icon: string; route: any[] }[] =
    (Object.keys(RecapitulatifComponent.CREATE_ROUTE) as TransactionSource[]).map(source => ({
      source,
      label: RecapitulatifComponent.AJOUTER_LABEL_OVERRIDE[source] ?? TransactionSourceLabels[source] ?? source,
      icon: RecapitulatifComponent.AJOUTER_ICON[source] ?? 'pi pi-plus-circle',
      route: RecapitulatifComponent.CREATE_ROUTE[source]!,
    }));

  displayAjouterModal = false;
  ajouterSearchTerm = '';

  @ViewChild('ajouterSearchInput') private ajouterSearchInputRef?: ElementRef<HTMLInputElement>;

  /** Filtre insensible à la casse sur le libellé — pour retrouver vite une source parmi les 16 éligibles. */
  get filteredAjouterItems() {
    const terme = this.ajouterSearchTerm.trim().toLowerCase();
    if (!terme) return this.ajouterItems;
    return this.ajouterItems.filter(item => item.label.toLowerCase().includes(terme));
  }

  openAjouterModal(): void {
    this.ajouterSearchTerm = '';
    this.displayAjouterModal = true;
  }

  closeAjouterModal(): void {
    this.displayAjouterModal = false;
  }

  /** Focus le champ de recherche dès l'ouverture — modale pensée pour aller vite au clavier. */
  onAjouterModalShow(): void {
    this.ajouterSearchInputRef?.nativeElement.focus();
  }

  choisirAjouter(item: { route: any[] }): void {
    this.closeAjouterModal();
    this.router.navigate(item.route);
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

    // BatchValidationRequestDto avec ids à un seul élément — voir architecture.md, Règle 1 :
    // valider une opération n'est qu'un cas particulier de valider un lot de taille 1. dateEmission
    // n'est volontairement pas transmise : elle est verrouillée sur l'entité (voir son control
    // `disabled` dans openUnitValidation), le fallback backend sur l'entité produit déjà le même résultat.
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

  // ==================== Chargement / filtres (inchangé) ====================

  ngOnInit(): void {
    this.initSearchForm();
    this.populateTransactionSourceOptions();

    this.subscriptions.push(
      this.route.parent!.paramMap.subscribe(params => {
        const id = params.get('banqueId');
        if (id) {
          this.banqueIdFromRoute = +id;
          if (this.dt) {
            this.dt.first = 0;
            this.loadRecapitulatif(this.dt.createLazyLoadMetadata());
          }
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

  /** Vrai quand COMMISSION fait partie des sources sélectionnées — pilote l'affichage du
   * filtre « Motif Commission » (21/09/2026, restaure le filtre de l'ancien écran dédié). */
  get showCommissionMotifFilter(): boolean {
    const sources: TransactionSource[] = this.searchForm?.get('transactionSources')?.value ?? [];
    return sources.includes(TransactionSource.COMMISSION);
  }

  /** Vrai quand IMPOT_TAXE fait partie des sources sélectionnées — pilote l'affichage du
   * filtre « Motif Impôt/Taxe » (21/09/2026, restaure le filtre de l'ancien écran dédié). */
  get showImpotTaxeMotifFilter(): boolean {
    const sources: TransactionSource[] = this.searchForm?.get('transactionSources')?.value ?? [];
    return sources.includes(TransactionSource.IMPOT_TAXE);
  }

  onFilterChange(): void {
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
            // `compositeKey` (source:id) calculée ici, jamais renvoyée par le backend — voir
            // Javadoc du champ dans RecaputilatifItem.ts pour la raison (dataKey PrimeNG stable).
            this.recapItems = response.page.content.map(item => ({
              ...item,
              compositeKey: `${item.source}:${item.id}`,
            }));
            this.totalRecords = response.page.page.totalElements;
            this.loading = false;
            this.totalCredit = response.totalCredit;
            this.totalDebit = response.totalDebit;
          },
          error: (error) => {
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

  /** Remet tous les critères à leur valeur neutre et recharge la première page. */
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

  /**
   * Vrai dès qu'un critère est renseigné — pilote l'état du bouton « Effacer » et
   * le message affiché quand le tableau est vide (aucun résultat vs aucune donnée).
   */
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

  /** Ouvre / referme la barre de filtres. */
  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  /**
   * Résumé des critères actifs, affiché en pastilles quand la barre est repliée —
   * pour qu'un filtre en vigueur reste visible sans avoir à déplier.
   */
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

  /** Nombre de critères actifs — badge du bouton « Filtres ». */
  get activeFilterCount(): number {
    return this.activeFilterChips.length;
  }

  /** Bascule le panneau des bornes de date de référence. */
  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  /** Nombre de bornes de date renseignées — affiché sur le bouton « Période ». */
  get activeDateFilterCount(): number {
    if (!this.searchForm) {
      return 0;
    }
    const value = this.searchForm.value;
    return (value.dateReferenceDepuis ? 1 : 0) + (value.dateReferenceJusqua ? 1 : 0);
  }

  /** Construit le DTO de recherche à partir du formulaire — partagé entre chargement et export. */
  private buildSearchDto(): RecapitulatifSearchDto {
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

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
}
