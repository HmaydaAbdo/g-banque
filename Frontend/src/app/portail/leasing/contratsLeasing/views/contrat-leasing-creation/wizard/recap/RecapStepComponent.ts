// src/app/portail/leasing/contratsLeasing/views/contrat-leasing-creation/wizard/recap/RecapStepComponent.ts

import { Component, OnInit, OnDestroy, Output, EventEmitter, ChangeDetectionStrategy, Input, inject } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { Subscription, forkJoin, of, Subject, combineLatest } from 'rxjs';
import { switchMap, map, catchError, takeUntil, distinctUntilChanged } from 'rxjs/operators';

// PrimeNG Modules
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { Message, MessageService } from 'primeng/api';
import { MessagesModule } from 'primeng/messages';

// DTOs and Services
import { LeasingContractWizardService } from '../../../../services/LeasingContractWizardService';
import { BanqueService } from "../../../../../../comptes-bancaire/services/BanqueService";
import { OrganismeLeasingService } from "../../../../../organismesLeasing/services/organisme-leasing.service";
import { Decimal } from 'decimal.js';
import { ContratLeasingRequestDTO } from "../../../../dtos/contrat-leasing";
import { TypeContratLeasing } from '../../../../dtos/type-contrat-leasing';
import { parseLocalDate } from '../../../../../../global/types/date.utils';

interface RecapContractDisplayData extends ContratLeasingRequestDTO {
  nomBanqueDePrelevement?: string;
  nomOrganismeLeasing?: string;
  totalMensualitesCalculated?: Decimal; // Sum of (duree * mensualite) only
  expectedTotalAmountService?: Decimal | null; // L'Indemnité from service
  coutFinancement?: Decimal | null; // endettement - prix d'achat
  dateAvance?: Date | null;
  datePremiereMensualite?: Date | null;
  dateValeurResiduelle?: Date | null;
  dateDebut: Date | null;
}

@Component({
  selector: 'app-recap-step',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    TagModule,
    MessagesModule
  ],
  templateUrl: './RecapStepComponent.html',
  styleUrls: ['./RecapStepComponent.scss'],
  providers: [DecimalPipe, DatePipe, MessageService]
})
export class RecapStepComponent implements OnInit, OnDestroy {

  @Output() stepValid = new EventEmitter<boolean>();

  @Input() globalValidationMessages: string[] = [];

  contractData: RecapContractDisplayData | null = null;
  formattedGlobalMessages: Message[] = [];

  isContractValid: boolean = false;

  private subscriptions: Subscription[] = [];
  private componentDestroyed$ = new Subject<void>();

  public TypeContratLeasing = TypeContratLeasing;

  private readonly wizardService = inject(LeasingContractWizardService);
  private readonly banqueService = inject(BanqueService);
  private readonly organismeLeasingService = inject(OrganismeLeasingService);
  private readonly decimalPipe = inject(DecimalPipe);
  private readonly datePipe = inject(DatePipe);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {

    this.stepValid.emit(true);

    const combinedData$ = combineLatest([
      this.wizardService.contract$,
      this.wizardService.globalValidationErrors$,
      this.wizardService.expectedTotalAmount$ // NEW: Subscribe to expectedTotalAmount from service
    ]).pipe(
      takeUntil(this.componentDestroyed$),
      // Corrected distinctUntilChanged comparator for all combined streams
      distinctUntilChanged((prevTuple, currTuple) => {
        const [prevContract, prevErrors, prevExpectedTotal] = prevTuple;
        const [currContract, currErrors, currExpectedTotal] = currTuple;

        const contractChanged = JSON.stringify(prevContract) !== JSON.stringify(currContract);
        const errorsChanged = JSON.stringify(prevErrors) !== JSON.stringify(currErrors);
        const expectedTotalChanged = prevExpectedTotal?.eq(currExpectedTotal || new Decimal(0)) === false; // Compare Decimals

        return !contractChanged && !errorsChanged && !expectedTotalChanged;
      }),
      switchMap(([contract, errors, expectedTotalAmountFromService]) => {

        this.formattedGlobalMessages = errors.map((errorDetail: string) => ({
          severity: 'error',
          summary: 'Erreur de Validation',
          detail: errorDetail
        }));

        this.isContractValid = errors.length === 0;

        const bankId = contract.banqueDePrelevementId;
        const organismeId = contract.organismeLeasingId;
        // montantAchatTTC and valeurResiduelle are not used directly for the total here
        // as expectedTotalAmountFromService already calculates L'Indemnité correctly.

        const phases = contract.phases || [];

        let totalMensualitesCalculated = new Decimal(0);
        for (const phase of phases) {
          // Ensure phase.mensualiteTTC is a Decimal instance before using it
          const mensualite = phase.mensualiteTTC instanceof Decimal ? phase.mensualiteTTC : new Decimal(phase.mensualiteTTC || 0);
          const duree = new Decimal(phase.dureeMois || 0);
          totalMensualitesCalculated = totalMensualitesCalculated.plus(mensualite.times(duree));
        }
        // REMOVED: const expectedTotalAmount = montantAchatTTC.minus(valeurResiduelle); // This was incorrect for L'Indemnité

        const bankName$ = bankId ? this.banqueService.getBanqueById(bankId).pipe(
          map(bank => bank.nom),
          catchError(() => of('Nom de banque indisponible'))
        ) : of(undefined);

        const organismeName$ = organismeId ? this.organismeLeasingService.getOrganismeLeasingById(organismeId).pipe(
          map(organisme => organisme.nom),
          catchError(() => of('Nom d\'organisme indisponible'))
        ) : of(undefined);

        return forkJoin([bankName$, organismeName$]).pipe(
          map(([nomBanque, nomOrganisme]) => {
            const achat = contract.montantAchatTTC ? new Decimal(contract.montantAchatTTC) : null;
            const debut = contract.dateDebut ? new Date(contract.dateDebut) : null;
            const periode = contract.periodeEnMois;
            const recapData: RecapContractDisplayData = {
              ...contract,
              coutFinancement: achat && expectedTotalAmountFromService ? expectedTotalAmountFromService.minus(achat) : null,
              dateAvance: contract.avanceTTC ? debut : null,
              datePremiereMensualite: debut ? this.ajouterMois(debut, contract.avanceTTC ? 1 : 0) : null,
              dateValeurResiduelle: debut && periode ? this.ajouterMois(debut, periode) : null,
              nomBanqueDePrelevement: nomBanque,
              nomOrganismeLeasing: nomOrganisme,
              totalMensualitesCalculated: totalMensualitesCalculated,
              expectedTotalAmountService: expectedTotalAmountFromService,
              dateDebut: contract.dateDebut
            };
            return recapData;
          })
        );
      }),
      catchError(() => of(null))
    );

    this.subscriptions.push(
      combinedData$.subscribe((data) => {
        this.contractData = data;
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.componentDestroyed$.next();
    this.componentDestroyed$.complete();
  }

  /** Même règle que le serveur (LocalDate.plusMonths) : le jour est ramené à la fin du mois si besoin. */
  private ajouterMois(date: Date, mois: number): Date {
    const cible = new Date(date.getFullYear(), date.getMonth() + mois, 1);
    const dernierJour = new Date(cible.getFullYear(), cible.getMonth() + 1, 0).getDate();
    cible.setDate(Math.min(date.getDate(), dernierJour));
    return cible;
  }

  getContractTypeDisplayName(type: TypeContratLeasing | null | undefined): string {
    if (type === null || type === undefined) return 'N/A';
    switch (type) {
      case TypeContratLeasing.LINEAIRE: return 'Linéaire';
      case TypeContratLeasing.PROGRESSIF: return 'Progressif';
      case TypeContratLeasing.DEGRESSIF: return 'Dégressif';
      default: return 'Inconnu';
    }
  }

  formatCurrency(value: Decimal | number | null | undefined): string {
    if (value === null || value === undefined) return 'N/A';
    const numValue = value instanceof Decimal ? value.toNumber() : value;
    return this.decimalPipe.transform(numValue, '1.2-2') + ' MAD';
  }

  formatDate(date: Date | string | null | undefined): string {
    if (!date) return 'N/A';
    const dateObj = typeof date === 'string' ? parseLocalDate(date) : date;
    return this.datePipe.transform(dateObj, 'dd/MM/yyyy') || 'N/A';
  }



  markFormAsTouched?(): void {
    // No operation needed for this display-only component.
  }
}
