import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, SelectItem, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { CalendarModule } from 'primeng/calendar';
import { InputNumberModule } from 'primeng/inputnumber';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputSwitchModule } from 'primeng/inputswitch';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { RippleModule } from 'primeng/ripple';
import { RouterLinkActive } from '@angular/router';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { Subscription } from 'rxjs';

import { VirementInternationalResponseDto } from '../../dtos/VirementInternationalResponseDto';
import { VirementInternationalService } from '../../service/virement-international.service';
import { Type } from '../../../Virements/Type';
import { VirementInternationalSearchDto } from '../../dtos/VirementInternationalSearchDto';
import { PageResponse } from '../../../global/types/PageResponse';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { AuthService } from '../../../../auth/services/auth.service';
import { Role } from '../../../../auth/enum/Role';

@Component({
  selector: 'app-virement-international-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    TableModule,
    ButtonModule,
    CardModule,
    InputTextModule,
    DropdownModule,
    CalendarModule,
    InputNumberModule,
    TagModule,
    ToastModule,
    ConfirmDialogModule,
    InputSwitchModule,
    BreadcrumbModule,
    RippleModule,
    RouterLinkActive,
    ToolbarModule,
    TooltipModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './virement-international-list.component.html'
})
export class VirementInternationalListComponent implements OnInit, OnDestroy {

  virements: VirementInternationalResponseDto[] = [];
  totalRecords = 0;
  loading = false;
  isAdmin = false;
  searchForm!: FormGroup;
  items: MenuItem[] | undefined;

  banqueOptions: SelectItem[] = [];
  statusOptions: SelectItem[] = [
    { label: 'Tous les virements', value: null },
    { label: 'Les virements en cours', value: true },
    { label: 'Les virements archivés', value: false }
  ];
  typeOptions: SelectItem[] = [
    { label: 'Tous les types', value: null },
    { label: 'Encaissement', value: Type.ENCAISSEMENT },
    { label: 'Décaissement', value: Type.DECAISSEMENT }
  ];

  @ViewChild('dt') dt: Table | undefined;

  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'dateEmission',
    sortOrder: -1,
    filters: {}
  };

  private subscriptions: Subscription[] = [];

  constructor(
    private virementInternationalService: VirementInternationalService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private banqueService: BanqueService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/virements-internationaux', label: 'Les Virements Internationaux' }
    ];
    this.isAdmin = this.authService.hasRole(Role.ADMIN);
    this.initSearchForm();
    this.loadBanques();
    this.loadVirements(this.lastLazyLoadEvent);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * Initializes the search form with all filter controls.
   */
  initSearchForm(): void {
    this.searchForm = this.fb.group({
      beneficiaire: [''],
      emetteur: [''],
      montant: [null],
      dateEmission: [null],
      type: [null],
      active: [null],
      banqueId: [null]
    });

    const searchSubscription = this.searchForm.valueChanges.subscribe(() => this.onSearch());
    this.subscriptions.push(searchSubscription);
  }

  /**
   * Loads virements based on lazy load event and form filters.
   * @param event The PrimeNG lazy load event.
   */
  loadVirements(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort = 'dateEmission,desc';
    if (event.sortField && event.sortOrder) {
      sort = `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: VirementInternationalSearchDto = this.searchForm.getRawValue();

    this.virementInternationalService
      .getVirementsInternationaux(searchCriteria, currentPage, pageSize, sort)
      .subscribe({
        next: (page: PageResponse<VirementInternationalResponseDto>) => {
          this.virements = page.content;
          this.totalRecords = page.page.totalElements;
          this.loading = false;
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors du chargement des virements internationaux:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des virements internationaux.'
          });
        }
      });
  }

  /**
   * Triggers a new search from the first page.
   */
  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadVirements(this.dt.createLazyLoadMetadata());
    }
  }

  /**
   * Loads the list of banks for the dropdown filter.
   */
  loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000)
      .subscribe({
        next: (response: PageResponse<BanqueResponse>) => {
          this.banqueOptions = response.content.map(banque => ({
            label: banque.nom,
            value: banque.id
          }));
          this.banqueOptions.unshift({ label: 'Toutes les banques', value: null });
        },
        error: (error) => {
          console.error('Error loading bank options:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to load banks.'
          });
        }
      });
  }

  /**
   * Clears all search filters and reloads the table.
   */
  clearSearch(): void {
    this.searchForm.reset();
    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadVirements(this.dt.createLazyLoadMetadata());
    }
  }

  /**
   * Confirms and toggles the status of a virement.
   * @param virement The virement to toggle.
   */
  confirmToggleStatus(virement: VirementInternationalResponseDto): void {
    const confirmationMessage = `Êtes-vous sûr de vouloir ${virement.active ? 'désactiver' : 'activer'} le virement ${virement.id}?`;
    this.confirmationService.confirm({
      message: confirmationMessage,
      header: `Confirmation de ${virement.active ? 'désactivation' : 'activation'}`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: virement.active ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.toggleVirementStatus(virement.id);
      },
      reject: () => {
        this.loadVirements(this.lastLazyLoadEvent)
      }
    });
  }

  toggleVirementStatus(id: number): void {
    this.virementInternationalService.toggleVirementInternationalStatus(id).subscribe({
      next: (updatedVirement) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Virement ${updatedVirement.id} ${updatedVirement.active ? 'activé' : 'désactivé'} avec succès.`
        });
        // Reload data from the server to reflect the change
        if (this.dt) {
          this.loadVirements(this.dt.createLazyLoadMetadata());
        }
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du virement:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec de la mise à jour du virement.'
        });
      }
    });
  }

  deleteVirement(virement: VirementInternationalResponseDto): void {
    const confirmationMessageHtml = `
    <div class="text-center">
        <i class="pi pi-exclamation-triangle" style="margin-right: 5px;"></i>
        <p>Êtes-vous sûr de vouloir supprimer ce virement "<strong>${virement.id}</strong>" ?</p>
        <p style="color: #ef4444; font-weight: bold;">Cette action est irréversible.</p>
    </div>
    `;

    this.confirmationService.confirm({
      message: confirmationMessageHtml,
      header: 'Confirmation de Suppression',
      icon: 'none',
      acceptLabel: 'Confirmer',
      rejectLabel: 'Annuler',
      accept: () => {
        this.loading = true;
        this.virementInternationalService.deleteVirementInternational(virement.id)
          .subscribe({
            next: () => {
              this.messageService.add({
                severity: 'success',
                summary: 'Succès',
                detail: 'Le virement a été supprimé avec succès.',
                life: 3000
              });
              this.loadVirements(this.lastLazyLoadEvent);
              this.loading = false;
            },
            error: (error) => {
              console.error('Erreur lors de la suppression du virement:', error);
              const errorMessage = error.error.detail || 'Échec de la suppression du virement.';
              this.messageService.add({
                severity: 'error',
                summary: 'Erreur',
                detail: errorMessage,
                life: 3000
              });
              this.loading = false;
            }
          });
      }
    });
  }
}
