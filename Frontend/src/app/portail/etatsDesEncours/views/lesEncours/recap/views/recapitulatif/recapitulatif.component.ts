import { Component, inject, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { Table } from 'primeng/table';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Observable, Subscription } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
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
import { LeasingTransactionRequest } from "../../../../../dtos/leasing/LeasingTransactionRequest";
import { EcheanceCmtService } from "../../../../../../cmt/services/EcheanceCmtService";
import { EcheanceCMTResponseDto } from "../../../../../../cmt/dtos/EcheanceCMTResponseDto";
import { CmtTransactionRequest } from "../../../../../dtos/cmt/CmtTransactionRequest";
import { ChequeService } from "../../../../../../cheques/services/ChequeService";
import { ChequeResponseDto } from "../../../../../../cheques/dtos/ChequeResponseDto";
import { ChequeTransactionRequest } from "../../../../../dtos/cheques/ChequeTransactionRequest";
import { LcnService } from "../../../../../../LCN/service/LcnService";
import { LcnResponseDTO } from "../../../../../../LCN/dtos/LcnResponseDTO";
import { LcnTransactionRequest } from "../../../../../dtos/lcn/LcnTransactionRequest";
import { VirementService } from "../../../../../../Virements/service/VirementService";
import { VirementResponseDto } from "../../../../../../Virements/dtos/VirementResponseDto";
import { VirementTransactionRequestDto } from "../../../../../dtos/virements/VirementTransactionRequestDto";
import { Type as SensType } from "../../../../../../Virements/Type";
import { PrefinancementService } from "../../../../../../prefinancements/service/PrefinancementService";
import { PrefinancementResponseDTO } from "../../../../../../prefinancements/dtos/PrefinancementResponseDTO";
import { PrefinancementTransactionRequest } from "../../../../../dtos/prefinancement/PrefinancementTransactionRequest";
import { DroitConstateService } from "../../../../../../droitsConstate/service/DroitConstateService";
import { DroitConstateResponseDto } from "../../../../../../droitsConstate/dtos/DroitConstateResponseDto";
import { DroitConstateTransactionRequest } from "../../../../../dtos/droits_constate/DroitConstateTransactionRequest";
import { FactoringService } from "../../../../../../factoring/service/FactoringService";
import { FactoringResponseDto } from "../../../../../../factoring/dtos/FactoringResponseDto";
import { FactoringTransactionRequest } from "../../../../../dtos/factoring/FactoringTransactionRequest";
import { DecompteService } from "../../../../../../decomptes/service/DecompteService";
import { DecompteResponseDto } from "../../../../../../decomptes/dtos/DecompteResponseDto";
import { DecompteTransactionRequest } from "../../../../../dtos/decomptes/DecompteTransactionRequest";
import { RestitutionService } from "../../../../../../restitution/service/restitution.service";
import { RestitutionResponseDto } from "../../../../../../restitution/dtos/RestitutionResponseDto";
import { RestitutionValidationRequest } from "../../../../../dtos/restitution/RestitutionValidationRequest";
import { ProvisionService } from "../../../../../../provision/service/provision.service";
import { ProvisionResponseDto } from "../../../../../../provision/dtos/ProvisionResponseDto";
import { ProvisionValidationRequest } from "../../../../../dtos/provision/ProvisionValidationRequest";
import { ImpayeeService } from "../../../../../../impayees/service/ImpayeeService";
import { ImpayeeResponseDto } from "../../../../../../impayees/dtos/ImpayeeResponseDto";
import { ImpayeeValidationRequest } from "../../../../../dtos/impayee/Impayeevalidationrequest";
import { VirementInternationalService } from "../../../../../../virementInternational/service/virement-international.service";
import { VirementInternationalResponseDto } from "../../../../../../virementInternational/dtos/VirementInternationalResponseDto";
import { VirementInternationalTransactionRequestDto } from "../../../../../dtos/virementsInternationaux/VirementInternationalTransactionRequestDto";
import { MiseADispositionService } from "../../../../../../Mise-à-disposition/service/MiseADispositionService";
import { MiseADispositionResponseDto } from "../../../../../../Mise-à-disposition/dtos/MiseADispositionResponseDto";
import { MiseAdispositionTransactionRequestDto } from "../../../../../dtos/mise_a_disposition/MiseAdispositionTransactionRequestDto";
import { AgiosService } from "../../../../../../agios/service/agios.service";
import { AgiosResponseDto } from "../../../../../../agios/dtos/AgiosResponseDto";
import { AgiosValidationRequest } from "../../../../../dtos/agios/agios-validation-request.dto";
import { CommissionService } from "../../../../../../commission/service/commission.service";
import { CommissionResponseDto } from "../../../../../../commission/dtos/commission-response.dto";
import { CommissionValidationDto } from "../../../../../dtos/commission/CommissionValidationDto";
import { FraisBancaireService } from "../../../../../../frais-bancaire/service/FraisBancaireService";
import { FraisBancaireResponseDto } from "../../../../../../frais-bancaire/dtos/FraisBancaireResponseDto";
import { FraisBancaireValidationRequest } from "../../../../../dtos/frais-bancaire/frais-bancaire-validation-request.dto";
import { PrelevementService } from "../../../../../../prelevements/service/prelevement.service";
import { PrelevementResponseDto } from "../../../../../../prelevements/dtos/PrelevementResponseDto";
import { PrelevementValidationRequestDto } from "../../../../../dtos/prelevement/prelevement-validation-request.dto";
import { ImpotEtTaxeService } from "../../../../../../impotEtTaxes/service/ImpotEtTaxeService";
import { ImpotEtTaxeResponseDto } from "../../../../../../impotEtTaxes/dtos/ImpotEtTaxeResponseDto";
import { ImpotEtTaxeValidationRequest } from "../../../../../dtos/impotsEtTaxes/ImpotEtTaxeValidationRequest";
import { OperationErroneService } from "../../../../../../operationErronées/service/operation-errone.service";
import { OperationErroneResponseDto } from "../../../../../../operationErronées/dtos/OperationErroneResponseDto";
import { OperationErroneValidationRequest } from "../../../../../dtos/operation-erronés/OperationErroneValidationRequest";
import { parseLocalDate } from '../../../../../../global/types/date.utils';

/** Options du dropdown de filtre de sens — `null` = « Tous », aucun filtre appliqué. */
interface SensOption {
  label: string;
  value: SensOperation | null;
}

/**
 * Un groupe d'opérations sélectionnées partageant (source, sens) — l'unité d'appel de
 * POST /rapprochement/process/lot, qui n'accepte qu'un seul type et un seul sens par requête
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
  buildAndSubmit: (detail: TDetail, formValue: any, rapprochementId: number) => Observable<unknown>;
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

  /** Regroupe la sélection par (source, sens) — un groupe = un appel POST /process/lot. */
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
   * découpage automatique en plusieurs appels /process/lot par un refus explicite — l'utilisateur
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

  /** Un seul appel POST /process/lot — la sélection est garantie homogène (voir openLotModal). */
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
      this.validationService.validerEnLot(request).subscribe({
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
  // ci-dessous : Leasing et CMT sont câblés (les 22 autres écrans "Encours" restent l'unique
  // moyen de validation ligne par ligne pour leur source, jusqu'à leur propre migration).
  // Décision du 14/09/2026 (remplace les deux dialogues Leasing/CMT distincts du pilote initial) :
  // la quasi-totalité du formulaire (dates, libellé, observation, TVA) est identique d'une
  // source à l'autre ; seules la forme du DTO de détail, celle de la requête de validation et
  // la politique TVA varient — capturées ici plutôt que dupliquées dans un dialogue par source.

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
      buildAndSubmit: (d: EcheanceLeasingResponse, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: LeasingTransactionRequest = {
          contratLeasingId: d.contratLeasingId,
          echeanceId: d.id,
          transactionSource: TransactionSource.LEASING,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.echeanceLeasingToRapprochemnt(request);
      },
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
      buildAndSubmit: (d: EcheanceCMTResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: CmtTransactionRequest = {
          creditMoyenTermeId: d.credit.id,
          echeanceId: d.id,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.echeanceCmtToRapprochement(request);
      },
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
      // Un seul endpoint depuis la fusion du 19/09/2026 (dateEmission reste verrouillée ici, comme
      // Leasing/CMT : décision HMAYDA du 19/09/2026, voir REFACTORING-VALIDATION-UNIFIEE.md).
      buildAndSubmit: (d: ChequeResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: ChequeTransactionRequest = {
          idCheque: d.id,
          transactionSource: TransactionSource.CHEQUE,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.chequeToRapprochement(request);
      },
    }],
    // LCN : contrairement à Cheque/Virement, le sens (et la nature encaissement/escompte) est déjà
    // porté par la source elle-même (LCN_DECAISSEMENT / LCN_ENCAISSEMENT / LCN_ESCOMPTE, voir
    // v_recapitulatif_encours) — chaque entrée appelle directement le bon endpoint, aucun test
    // runtime sur le détail nécessaire (contrairement à Cheque/Virement qui n'ont qu'une seule
    // TransactionSource et lisent `d.type`).
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
      buildAndSubmit: (d: LcnResponseDTO, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: LcnTransactionRequest = {
          id: d.id,
          transactionSource: TransactionSource.LCN_DECAISSEMENT,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.lcnToRapprochement(request);
      },
    }],
    // LCN_ENCAISSEMENT et LCN_ESCOMPTE partagent la même configuration — un seul endpoint
    // backend depuis la fusion du 19/09/2026, appelé indifféremment par les trois entrées LCN.
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
      buildAndSubmit: (d: LcnResponseDTO, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: LcnTransactionRequest = {
          id: d.id,
          transactionSource: TransactionSource.LCN_ENCAISSEMENT,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.lcnToRapprochement(request);
      },
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
      buildAndSubmit: (d: LcnResponseDTO, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: LcnTransactionRequest = {
          id: d.id,
          transactionSource: TransactionSource.LCN_ESCOMPTE,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.lcnToRapprochement(request);
      },
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
      // Un seul endpoint depuis la fusion du 19/09/2026 — le sens (`Virement.type`) est lu sur
      // l'entité côté backend, plus besoin de brancher ici.
      buildAndSubmit: (d: VirementResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: VirementTransactionRequestDto = {
          id: d.id,
          transactionSource: TransactionSource.Virement,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.virementToRapprochement(request);
      },
    }],
    // Préfinancement/Factoring/Droit constaté/Décomptes : sens (remboursement/déblocage) porté par
    // l'entité elle-même (champ `type`, ou fixe pour Décomptes), jamais un choix fait à la
    // validation — un seul endpoint par source, aucun branchement runtime nécessaire ici
    // (contrairement à Cheque/Virement). `banqueId` vient du détail lui-même (`d.banqueId` /
    // `d.banqueEncaissementId` pour Décomptes), pas de la route : c'est la banque du marché
    // (Préfinancement/Factoring), celle propre à l'entité (Droit constaté) ou la banque
    // d'encaissement (Décomptes), déjà résolue côté backend dans le DTO de détail.
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
      buildAndSubmit: (d: PrefinancementResponseDTO, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: PrefinancementTransactionRequest = {
          prefinancementId: d.id,
          banqueId: d.banqueId,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.prefinancementToRapprochement(request);
      },
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
      buildAndSubmit: (d: FactoringResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: FactoringTransactionRequest = {
          factoringId: d.id,
          banqueId: d.banqueId,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.factoringToRapprochement(request);
      },
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
      buildAndSubmit: (d: DroitConstateResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: DroitConstateTransactionRequest = {
          droitConstateId: d.id,
          banqueId: d.banqueId,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.droitConstateToRapprochement(request);
      },
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
      buildAndSubmit: (d: DecompteResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: DecompteTransactionRequest = {
          decompteId: d.id,
          banqueId: d.banqueEncaissementId,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.decompteToRapprochement(request);
      },
    }],
    // Restitution/Provision/Impayée : même principe que Préfinancement & co (sens porté par
    // l'entité, un seul endpoint, aucun branchement runtime) mais SANS banqueId — la banque est
    // résolue côté backend depuis l'entité elle-même (`Restitution.banque`/`Provision.banque`/
    // `Impayee.banque`), jamais transmise par l'appelant. Pas de numéro de pièce naturel pour
    // Restitution/Provision (contrairement aux sources marché) : recapLabel/recapValue retombent
    // sur l'ID, comme l'affichent déjà leurs écrans dédiés respectifs. Impayée a un `numero`.
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
      buildAndSubmit: (d: RestitutionResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: RestitutionValidationRequest = {
          idRestitution: d.id,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.restitutionToRapprochement(request);
      },
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
      buildAndSubmit: (d: ProvisionResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: ProvisionValidationRequest = {
          idProvision: d.id,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.provisionToRapprochement(request);
      },
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
      buildAndSubmit: (d: ImpayeeResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: ImpayeeValidationRequest = {
          idImpayee: d.id,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.impayeeToRapprochement(request);
      },
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
      // Un seul endpoint depuis la fusion du 19/09/2026 — le sens (`VirementInternational.type`)
      // est lu sur l'entité côté backend, plus besoin de brancher ici.
      buildAndSubmit: (d: VirementInternationalResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: VirementInternationalTransactionRequestDto = {
          id: d.id,
          transactionSource: TransactionSource.VIREMENT_INTERNATIONAL,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.virementInternationalToRapprochement(request);
      },
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
      // Un seul endpoint depuis la fusion du 19/09/2026 — le sens (`MiseADisposition.type`) est lu
      // sur l'entité côté backend, plus besoin de brancher ici (le `d.type` ci-dessus dans
      // toCommonDetail reste utile : c'est un choix d'affichage, pas d'endpoint).
      buildAndSubmit: (d: MiseADispositionResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: MiseAdispositionTransactionRequestDto = {
          id: d.id,
          transactionSource: TransactionSource.MiseADisposition,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.miseAdispoToRapprochement(request);
      },
    }],
    // Agios/Commission/Frais bancaire/Prélèvement/Impôt-Taxe/Opération erronée : toujours un
    // décaissement (sens fixe), un seul endpoint par source, aucun branchement runtime nécessaire
    // ici — même principe que Restitution/Provision/Impayée. Aucune de ces 6 sources n'a
    // d'identifiant métier naturel distinct de son id (comme leurs écrans dédiés actuels, dont la
    // boîte de confirmation retombe elle aussi sur l'id) : recapLabel/recapValue retombent sur l'id.
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
      buildAndSubmit: (d: AgiosResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: AgiosValidationRequest = {
          idAgio: d.id,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.agiosToRapprochement(request);
      },
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
      buildAndSubmit: (d: CommissionResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: CommissionValidationDto = {
          idCommission: d.id,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.commissionToRapprochement(request);
      },
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
      buildAndSubmit: (d: FraisBancaireResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: FraisBancaireValidationRequest = {
          idFraisBancaire: d.id,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.fraisBancaireToRapprochement(request);
      },
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
      buildAndSubmit: (d: PrelevementResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: PrelevementValidationRequestDto = {
          idPrelevement: d.id,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.prelevementToRapprochement(request);
      },
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
      buildAndSubmit: (d: ImpotEtTaxeResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: ImpotEtTaxeValidationRequest = {
          idImpotEtTaxe: d.id,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.impotEtTaxeToRapprochement(request);
      },
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
      buildAndSubmit: (d: OperationErroneResponseDto, formValue: any, rapprochementId: number) => {
        const saisieTva: SaisieTva = formValue.tva;
        const request: OperationErroneValidationRequest = {
          idOperationErrone: d.id,
          dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
          dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
          dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
          observation: formValue.observation,
          tauxTva: saisieTva.tauxTva,
          montantTva: saisieTva.montantTva,
          libelle: formValue.libelle,
          rapprochementId,
        };
        return this.validationService.operationErroneToRapprochement(request);
      },
    }],
  ]);

  displayUnitValidationModal = false;
  unitValidationForm!: FormGroup;
  unitValidationDetail: UnitValidationDetail | null = null;
  isUnitValidationLoading = false;
  isUnitValidationSubmitting = false;

  /** DTO brut spécifique à la source active — passé tel quel à `buildAndSubmit`. */
  private unitValidationRawDetail: any = null;
  private activeUnitValidationConfig: UnitValidationConfig<any> | null = null;

  get montantMaxTvaUnitValidation(): number | null {
    return this.unitValidationDetail?.montantTTC ?? null;
  }

  /** Vrai si la source de cette ligne a une config enregistrée — pilote le bouton « Valider ». */
  canValidateUnitairement(item: RecaputilatifItem): boolean {
    return this.unitValidationConfigs.has(item.source);
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

    this.isUnitValidationSubmitting = true;

    this.subscriptions.push(
      config.buildAndSubmit(this.unitValidationRawDetail, formValue, rapprochementId!).subscribe({
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
      libelle: [''],
      observation: [''],
      numero: [''],
      sens: [null],
      dateReferenceDepuis: [null],
      dateReferenceJusqua: [null],
      montant: [null]
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
      libelle: '',
      observation: '',
      numero: '',
      sens: null,
      dateReferenceDepuis: null,
      dateReferenceJusqua: null,
      montant: null
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
    return !!(value.libelle || value.observation || value.numero || value.sens || value.montant
      || value.dateReferenceDepuis || value.dateReferenceJusqua
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
    if (value.libelle) {
      chips.push('Libellé : ' + value.libelle);
    }
    if (value.observation) {
      chips.push('Observation : ' + value.observation);
    }
    if (value.numero) {
      chips.push('N° : ' + value.numero);
    }
    if (value.montant !== null && value.montant !== undefined && value.montant !== '') {
      chips.push('Montant : ' + value.montant);
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
      libelle: this.searchForm.get('libelle')?.value,
      observation: this.searchForm.get('observation')?.value,
      numero: this.searchForm.get('numero')?.value,
      sens: this.searchForm.get('sens')?.value,
      dateReferenceDepuis: this.searchForm.get('dateReferenceDepuis')?.value,
      dateReferenceJusqua: this.searchForm.get('dateReferenceJusqua')?.value,
      montant: this.searchForm.get('montant')?.value
    };
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
}
