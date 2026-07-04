import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { MenuItem, Message, MessageService, ConfirmationService } from 'primeng/api';
import { Router, ActivatedRoute, NavigationEnd, Event as RouterEvent } from '@angular/router';
import { Subscription, filter, Observable, BehaviorSubject, combineLatest } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { CommonModule } from '@angular/common';

// PrimeNG Modules
import { StepsModule } from 'primeng/steps';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessagesModule } from 'primeng/messages';

// Service and DTOs
import { LeasingContractWizardService } from '../../services/LeasingContractWizardService';

// Import your step components
import { OrganismeStepComponent } from './wizard/organisme/organisme-step.component';
import { GeneralDetailsStepComponent } from './wizard/GeneralDetail/GeneralDetailsStepComponent';
import { BanqueStepComponent } from './wizard/BanqueOfDebit/BanqueStepComponent';
import { ContractTypeStepComponent } from './wizard/contract-type/contract-type-step.component';
import { PhasesStepComponent } from './wizard/Phases/PhasesStepComponent';
import { RecapStepComponent } from "./wizard/recap/RecapStepComponent";



// Define an interface for shared methods across step components for type safety
interface WizardStepComponent {
  stepValid: Observable<boolean>;
  setGlobalValidationMessages?(messages: string[]): void;
  markFormAsTouched?(): void;
}

@Component({
  selector: 'app-contrat-leasing-creation',
  templateUrl: './contrat-leasing-creation.component.html',
  styleUrls: ['./contrat-leasing-creation.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    StepsModule,
    CardModule,
    ButtonModule,
    ToastModule,
    ConfirmDialogModule,
    MessagesModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class ContratLeasingCreationComponent implements OnInit, OnDestroy {

  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly wizardService = inject(LeasingContractWizardService);

  items: MenuItem[] = [];
  activeIndex: number = 0;
  currentStepValid: boolean = false;
  isCurrentStepGloballyValid: boolean = false;

  private currentActiveChildComponent: WizardStepComponent | null = null;

  private _globalMessages = new BehaviorSubject<Message[]>([]);
  globalMessages$: Observable<Message[]> = this._globalMessages.asObservable();

  private allGlobalErrors: string[] = [];

  private routerSubscription!: Subscription;
  private currentStepValiditySubscription!: Subscription | null;
  private globalErrorsSubscription!: Subscription;
  private combinedValiditySubscription!: Subscription;

  private stepComponentMap: { [key: string]: any } = {
    'organisme': OrganismeStepComponent,
    'general-details': GeneralDetailsStepComponent,
    'banque': BanqueStepComponent,
    'type': ContractTypeStepComponent,
    'phases': PhasesStepComponent,
    'recap': RecapStepComponent
  };

  ngOnInit(): void {
    this.initSteps();
    this.subscribeToRouterEvents();

    // Subscribe to global validation errors from the service
    this.globalErrorsSubscription = this.wizardService.globalValidationErrors$.pipe(
      map(errors => {
        this.allGlobalErrors = errors;
        return errors.map(errorDetail => ({
          severity: 'error',
          summary: 'Erreur de Validation Globale',
          detail: errorDetail
        }));
      })
    ).subscribe(messages => {
      this._globalMessages.next(messages);
      this.distributeGlobalErrorsToActiveChild();
    });
  }

  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
    if (this.currentStepValiditySubscription) {
      this.currentStepValiditySubscription.unsubscribe();
      this.currentStepValiditySubscription = null;
    }
    if (this.globalErrorsSubscription) {
      this.globalErrorsSubscription.unsubscribe();
    }
    if (this.combinedValiditySubscription) {
      this.combinedValiditySubscription.unsubscribe();
    }
  }

  initSteps(): void {
    this.items = [
      { label: 'Organisme', routerLink: 'organisme' },
      { label: 'Détails Généraux', routerLink: 'general-details' },
      { label: 'Banque de prélèvement', routerLink: 'banque' },
      { label: 'Type de Contrat', routerLink: 'type' },
      { label: 'Phases', routerLink: 'phases' },
      { label: 'Récapitulatif', routerLink: 'recap' }
    ];
  }

  subscribeToRouterEvents(): void {
    this.routerSubscription = this.router.events.pipe(
      filter((event: RouterEvent) => event instanceof NavigationEnd)
    ).subscribe(() => {
      const currentRoute = this.activatedRoute.firstChild;
      if (currentRoute) {
        const path = currentRoute.snapshot.url[0]?.path;
        const index = this.items.findIndex(item => item.routerLink === path);
        if (index !== -1) {
          Promise.resolve().then(() => {
            this.activeIndex = index;
          });
        }
      }
    });
  }

  // This method is now primarily for clicks on the p-steps itself, not the 'Suivant' button.
  onStepChange(event: number): void {
    // If a user clicks on a step in p-steps and it's not the last step
    if (event < this.items.length) {
      // If the current step isn't valid, prevent forward navigation via clicks on the steps.
      if (!this.isCurrentStepGloballyValid && event > this.activeIndex) {
        this.currentActiveChildComponent?.markFormAsTouched?.();
        this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez corriger les erreurs de l\'étape actuelle.' });
        this.distributeGlobalErrorsToActiveChild();
        return;
      }
      this.router.navigate([this.items[event].routerLink], { relativeTo: this.activatedRoute });
    }
    // Clicking on the last step in p-steps only navigates there; submission is handled by the dedicated button.
  }

  onActivate(componentRef: any): void {
    if (this.currentStepValiditySubscription) {
      this.currentStepValiditySubscription.unsubscribe();
      this.currentStepValiditySubscription = null;
    }
    if (this.combinedValiditySubscription) {
      this.combinedValiditySubscription.unsubscribe();
    }

    const pathFromComponent = Object.keys(this.stepComponentMap).find(key =>
      this.stepComponentMap[key] === componentRef.constructor
    );

    if (!pathFromComponent) {
      this.currentActiveChildComponent = null;
      this.currentStepValid = false;
      this.isCurrentStepGloballyValid = false;
      return;
    }

    Promise.resolve().then(() => {
      const newActiveIndex = this.items.findIndex(item => item.routerLink === pathFromComponent);
      if (newActiveIndex !== -1 && newActiveIndex !== this.activeIndex) {
        this.activeIndex = newActiveIndex;
      }
    });

    this.currentActiveChildComponent = componentRef as WizardStepComponent;

    this.currentStepValiditySubscription = this.currentActiveChildComponent.stepValid.pipe(
      startWith(false)
    ).subscribe((isValid: boolean) => {
      this.currentStepValid = isValid;
    });

    // Combine local step validity with global errors for the current step
    this.combinedValiditySubscription = combineLatest([
      this.currentActiveChildComponent.stepValid.pipe(startWith(false)), // Local validity
      this.wizardService.globalValidationErrors$.pipe(startWith([])) // Global errors from service (raw strings)
    ]).subscribe(([localValid, globalErrors]) => {
      const currentStepRouterLink = this.items[this.activeIndex]?.routerLink;
      // Filter global errors to find only those relevant to the *current* step's prefix
      const errorsForCurrentStep = globalErrors.filter(msg => msg.startsWith(`${currentStepRouterLink}:`));
      this.isCurrentStepGloballyValid = localValid && errorsForCurrentStep.length === 0;

    });

    this.distributeGlobalErrorsToActiveChild();


    // Special handling for RecapStepComponent (always considered valid for navigation)
    if (componentRef instanceof RecapStepComponent) {
      this.currentStepValid = true;
      this.isCurrentStepGloballyValid = true; // Recap step is always valid for 'Suivant' button logic (now handled by isSubmitEnabled for submit)
    }
  }

  private distributeGlobalErrorsToActiveChild(): void {
    if (this.currentActiveChildComponent?.setGlobalValidationMessages) {
      const currentStepRouterLink = this.items[this.activeIndex]?.routerLink;
      if (currentStepRouterLink) {
        // Filter `allGlobalErrors` based on the current step's routerLink
        const messagesForCurrentStep = this.allGlobalErrors // Use the raw `allGlobalErrors` here
          .filter(msg => msg.startsWith(`${currentStepRouterLink}:`))
          .map(msg => msg.split(':')[1]?.trim() || msg);
        this.currentActiveChildComponent.setGlobalValidationMessages(messagesForCurrentStep);
      } else {
        // If currentStepRouterLink is undefined (e.g., initial state), clear messages
        this.currentActiveChildComponent.setGlobalValidationMessages([]);
      }
    }
  }

  // This method is now only for the "Précédent" and the "Suivant" (Next) buttons
  navigateToStep(direction: 'prev' | 'next'): void {
    if (direction === 'next') {
      this.currentActiveChildComponent?.markFormAsTouched?.();
      this.distributeGlobalErrorsToActiveChild();

      if (!this.isCurrentStepGloballyValid) {
        this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez corriger les erreurs de l\'étape actuelle.' });
        return;
      }

      let nextIndex = this.activeIndex + 1;
      if (nextIndex < this.items.length) { // Only navigate if not the last step
        this.router.navigate([this.items[nextIndex].routerLink], { relativeTo: this.activatedRoute });
      }
      // No `submitContract()` call here. The dedicated "Soumettre" button handles it.
    } else { // Navigating 'prev'
      let prevIndex = this.activeIndex - 1;
      if (prevIndex >= 0) {
        this.router.navigate([this.items[prevIndex].routerLink], { relativeTo: this.activatedRoute });
      }
    }
  }

  /**
   * Determines if the "Soumettre" button should be enabled.
   * It is enabled only if there are no global validation errors for the entire contract.
   */
  isSubmitEnabled(): boolean {
    // `this.allGlobalErrors` is kept up-to-date by the `globalErrorsSubscription`
    // If this array is empty, the entire contract is considered valid for submission.
    return this.allGlobalErrors.length === 0;
  }


  submitContract(): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir soumettre ce contrat de leasing ?',
      header: 'Confirmation de Soumission',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        // 6. If confirmed, call the wizard service to submit to the backend.
        this.wizardService.submitContractToBackend().subscribe({
          next: (response) => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Contrat soumis avec succès!' });
            this.wizardService.resetContract(); // Clear wizard state
            this.router.navigate(['portail/leasing/contratsLeasing']);
          }
        });
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Soumission annulée.' });
      }
    });
  }
}
