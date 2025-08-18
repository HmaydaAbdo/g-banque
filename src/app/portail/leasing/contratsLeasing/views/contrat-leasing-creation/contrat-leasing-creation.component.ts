import { Component, OnInit, OnDestroy } from '@angular/core';
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

  items: MenuItem[] = [];
  activeIndex: number = 0;
  currentStepValid: boolean = false; // Local validity of the current child component's form
  isCurrentStepGloballyValid: boolean = false; // Combines local validity and global errors for the current step

  private currentActiveChildComponent: WizardStepComponent | null = null;

  private _globalMessages = new BehaviorSubject<Message[]>([]);
  globalMessages$: Observable<Message[]> = this._globalMessages.asObservable();

  private allGlobalErrors: string[] = []; // Raw global errors from the service (for overall contract validity)

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

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private wizardService: LeasingContractWizardService
  ) {
    // Subscribe to global validation errors from the service
    // This subscription keeps `this.allGlobalErrors` up-to-date with ALL global errors.
    this.globalErrorsSubscription = this.wizardService.globalValidationErrors$.pipe(
      map(errors => {
        // Store the raw errors directly for `isSubmitEnabled` check
        this.allGlobalErrors = errors; // <--- IMPORTANT: Update this here

        // Format for p-messages display
        return errors.map(errorDetail => ({
          severity: 'error',
          summary: 'Erreur de Validation Globale',
          detail: errorDetail
        }));
      })
    ).subscribe(messages => {
      this._globalMessages.next(messages);
      this.distributeGlobalErrorsToActiveChild(); // Distribute to the currently active child if needed
      // `updateCombinedValidity()` is now mostly handled by the `combineLatest` in `onActivate`
    });
  }

  ngOnInit(): void {
    this.initSteps();
    this.subscribeToRouterEvents();
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
            console.log(`ContratLeasingCreationComponent: Active index updated to ${this.activeIndex} based on router event (deferred).`);
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
      console.warn('ContratLeasingCreationComponent: Activated component type not found in map:', componentRef.constructor.name);
      this.currentActiveChildComponent = null;
      this.currentStepValid = false;
      this.isCurrentStepGloballyValid = false;
      return;
    }

    Promise.resolve().then(() => {
      const newActiveIndex = this.items.findIndex(item => item.routerLink === pathFromComponent);
      if (newActiveIndex !== -1 && newActiveIndex !== this.activeIndex) {
        this.activeIndex = newActiveIndex;
        console.log(`ContratLeasingCreationComponent: Active index updated to ${this.activeIndex} based on activated component (deferred).`);
      }
    });

    this.currentActiveChildComponent = componentRef as WizardStepComponent;

    this.currentStepValiditySubscription = this.currentActiveChildComponent.stepValid.pipe(
      startWith(false)
    ).subscribe((isValid: boolean) => {
      this.currentStepValid = isValid;
      console.log(`ContratLeasingCreationComponent: currentStepValid updated to ${isValid} for ${componentRef.constructor.name}`);
      // The combineLatest below will handle isCurrentStepGloballyValid
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

      // Note: `this.allGlobalErrors` is already updated by the `globalErrorsSubscription`
      // so no need to explicitly update it here.

      console.log(`ContratLeasingCreationComponent: isCurrentStepGloballyValid updated to ${this.isCurrentStepGloballyValid} (Local: ${localValid}, Global Errors for step: ${errorsForCurrentStep.length})`);
    });

    this.distributeGlobalErrorsToActiveChild();

    setTimeout(() => {
      this.currentActiveChildComponent?.markFormAsTouched?.();
      console.log(`ContratLeasingCreationComponent: Called markFormAsTouched on ${componentRef.constructor.name}`);
    }, 0);

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
    console.log('ContratLeasingCreationComponent: submitContract');
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
          },
          error: (error) => {
            console.error('Submission error:', error);
            this.messageService.add({ severity: 'error', summary: 'Erreur de soumission', detail: 'Une erreur est survenue lors de la soumission du contrat: ' + (error.message || 'Erreur inconnue') });
          }
        });
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Soumission annulée.' });
      }
    });
  }
}
