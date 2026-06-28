import { Component, OnInit, OnDestroy, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrganismeLeasingService } from '../../services/organisme-leasing.service';
import { OrganismeLeasingResponse } from '../../dtos/OrganismeLeasingResponse';
import { OrganismeLeasingSearch } from '../../dtos/OrganismeLeasingSearch';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ConfirmationService, MessageService, ConfirmEventType } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CardModule } from 'primeng/card';
import { finalize, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { PageResponse } from "../../../../global/types/PageResponse";
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { RippleModule } from "primeng/ripple";
import { TooltipModule } from "primeng/tooltip";
import { Router } from '@angular/router';
import { AuthService } from '../../../../../auth/services/auth.service';

@Component({
  selector: 'app-organisme-leasing-list',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    ToastModule,
    ConfirmDialogModule,
    CardModule,
    FormsModule,
    RippleModule,
    TooltipModule,
    ReactiveFormsModule
  ],
  templateUrl: './organisme-leasing-list.component.html',
  styleUrls: ['./organisme-leasing-list.component.scss'],
  providers: [
    MessageService,
    ConfirmationService
  ]
})
export class OrganismeLeasingListComponent implements OnInit, OnDestroy {

  @ViewChild('dt') dt: Table | undefined;

  private readonly organismeLeasingService = inject(OrganismeLeasingService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  organismeLeasings: OrganismeLeasingResponse[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  first: number = 0;
  rows: number | undefined = 3;
  sortField: string | string[] = 'id';
  sortOrder: number = 1;

  searchForm!: FormGroup;

  private subscriptions: Subscription = new Subscription();

  ngOnInit(): void {
    this.initSearchForm();

    this.subscriptions.add(
      this.searchForm.get('nom')!.valueChanges
        .pipe(
          debounceTime(400),
          distinctUntilChanged()
        )
        .subscribe(() => {
          this.onSearch();
        })
    );

    this.loadOrganismeLeasings({
      first: this.first,
      rows: this.rows,
      sortField: this.sortField,
      sortOrder: this.sortOrder,
      filters: {}
    });
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      nom: ['']
    });
  }

  loadOrganismeLeasings(event: TableLazyLoadEvent): void {
    this.loading = true;

    this.first = event.first !== undefined ? event.first : this.first;
    this.rows = (event.rows !== undefined ? event.rows : this.rows) as number;
    this.sortField = event.sortField || 'id';
    this.sortOrder = event.sortOrder || 1;

    const page = this.first / (this.rows as number);
    const sort = this.sortField ? `${this.sortField},${this.sortOrder === 1 ? 'asc' : 'desc'}` : undefined;

    const searchCriteria: OrganismeLeasingSearch = {
      nom: this.searchForm.get('nom')?.value || ''
    };

    this.subscriptions.add(
      this.organismeLeasingService.searchOrganismeLeasings(
        searchCriteria,
        page,
        (this.rows as number),
        sort
      ).pipe(
        finalize(() => this.loading = false)
      ).subscribe({
        next: (pageResponse: PageResponse<OrganismeLeasingResponse>) => {
          this.organismeLeasings = pageResponse.content;
          this.totalRecords = pageResponse.page.totalElements;
        }
      })
    );
  }

  onSearch(): void {
    this.first = 0;
    if (this.dt) {
      this.loadOrganismeLeasings(this.dt.createLazyLoadMetadata());
    } else {
      this.loadOrganismeLeasings({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      });
    }
  }

  openNewOrganismeLeasingPage(): void {
    this.router.navigate(['/portail/leasing/organismesLeasing/new']);
  }

  editOrganismeLeasing(organisme: OrganismeLeasingResponse): void {
    this.router.navigate(['/portail/leasing/organismesLeasing/edit', organisme.id]);
  }

  confirmDeleteOrganismeLeasing(id: number, nom: string): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer l'organisme de leasing "${nom}" ?`,
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.deleteOrganismeLeasing(id);
      },
      reject: (type: ConfirmEventType) => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Suppression annulée.' });
      }
    });
  }

  deleteOrganismeLeasing(id: number): void {
    this.loading = true;
    this.subscriptions.add(
      this.organismeLeasingService.deleteOrganismeLeasing(id).pipe(
        finalize(() => this.loading = false)
      ).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Organisme de leasing supprimé avec succès.' });
          this.refreshList();
        }
      })
    );
  }

  refreshList(): void {
    if (this.dt) {
      this.loadOrganismeLeasings(this.dt.createLazyLoadMetadata());
    } else {
      this.loadOrganismeLeasings({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      });
    }
  }

  isSystemAdmin(): boolean {
    return this.authService.isSystemAdmin();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }
}
