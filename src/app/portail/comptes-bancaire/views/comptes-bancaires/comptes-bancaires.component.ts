import { Component, ViewChild, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ImageModule } from 'primeng/image';
import { ToastModule } from 'primeng/toast';
import {MenuItem, MessageService} from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { BanqueResponse } from "../../dtos/BanqueResponse";
import { Devise } from "../../enums/Devise";
import { BanqueService } from "../../services/BanqueService";
import { ToolbarModule } from "primeng/toolbar";
import { TagModule } from "primeng/tag";
import { TooltipModule } from "primeng/tooltip";
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {RouterLink, RouterLinkActive} from '@angular/router';
import {PageResponse} from "../../../global/types/PageResponse";
import {Role} from "../../../../auth/enum/Role";
import {AuthService} from "../../../../auth/services/auth.service";
import {BanqueSearchCriteria} from "../../dtos/BanqueSearchCriteria";
import {BreadcrumbModule} from "primeng/breadcrumb";

@Component({
  selector: 'app-comptes-bancaires',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    ImageModule,
    ToastModule,
    ConfirmDialogModule,
    ToolbarModule,
    TagModule,
    TooltipModule,
    RouterLink,
    BreadcrumbModule,
    RouterLinkActive
  ],
  templateUrl: './comptes-bancaires.component.html',
  styleUrl: './comptes-bancaires.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class ComptesBancairesComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  banques!: BanqueResponse[];
  items: MenuItem[] | undefined;
  totalRecords: number = 0;
  loading: boolean = false;
  first: number | undefined = 0;
  rows: number | undefined  = 3;
  sortField: string | string[] = 'id';
  sortOrder: number = 1;
  searchForm!: FormGroup;

  constructor(
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private fb: FormBuilder,
    private authService: AuthService,
  ) { }

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },{ icon: 'pi pi-wallet', route: '/portail/comptes-bancaires', label:"liste des compte bancaires" } ]
    this.initSearchForm();
    this.searchForm.get('nom')!.valueChanges
      .pipe(
        debounceTime(800),
        distinctUntilChanged()
      )
      .subscribe(() => {
        this.onSearch();
      });
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      nom: ['']
    });
  }

  loadBanques(event: TableLazyLoadEvent = { first: this.first, rows: this.rows, sortField: this.sortField, sortOrder: this.sortOrder, filters: {} }): void {
    this.loading = true;
    const first = event.first !== undefined ? event.first : this.first;
    const rows = event.rows !== undefined ? event.rows : this.rows;
    const sortField = event.sortField || this.sortField;
    const sortOrder = event.sortOrder === 1 ? 1 : -1;

    const page = ( first as number ) / (rows as number);

    // Handle sortField potentially being an array
    const actualSortField = Array.isArray(sortField) ? sortField[0] : sortField;
    const sort = `${actualSortField},${sortOrder === 1 ? 'asc' : 'desc'}`;

    const searchCriteria: BanqueSearchCriteria = {
      nom: this.searchForm.get('nom')?.value || ''
    };

    this.banqueService.getAllBanques(searchCriteria, page, rows as number, sort)
      .subscribe({
        next: (response: PageResponse<BanqueResponse>) => {
          this.banques = response.content;
          this.totalRecords = response.totalElements;
          this.loading = false;

        },
        error: (error) => {
          console.error('Erreur lors du chargement des banques:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des banques. ' + (error.error?.message || ''),
            life: 3000
          });
          this.loading = false;
        }
      });
  }

  onSearch(): void {
    this.first = 0;
    if (this.dt) {
      this.loadBanques(this.dt.createLazyLoadMetadata());
    } else {
      this.loadBanques({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      } as TableLazyLoadEvent);
    }
  }

  clearSearch(): void {
    this.searchForm.reset({ nom: '' });
    this.first = 0;

    if (this.dt) {
      this.dt.clear();
      this.loadBanques(this.dt.createLazyLoadMetadata());
    } else {
      this.loadBanques({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      } as TableLazyLoadEvent);
    }
  }

  deleteBanque(banque: BanqueResponse): void {
    const confirmationMessageHtml = `
    <div class="text-center ">
        <i class="pi pi-exclamation-triangle" style="margin-right: 5px;"></i>
        <p>Êtes-vous sûr de vouloir supprimer la banque "<strong>${banque.nom}</strong>" ?</p>
        <p style="color: #ef4444; font-weight: bold;">
                Toutes les lignes fonctionnelles (découverts, lignes de crédit, etc.) associées à cette banque seront également supprimées.
        </p>
        <p>Cette action est irréversible.</p>
    </div>

    `;

    this.confirmationService.confirm({
      message: confirmationMessageHtml,

      header: 'Confirmation de Suppression',
      icon: 'none',
      accept: () => {
        this.loading = true;
        this.banqueService.deleteBanque(banque.id!)
          .subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Banque et ses lignes fonctionnelles supprimées avec succès.', life: 3000 });
              if (this.dt) {
                this.loadBanques(this.dt.createLazyLoadMetadata());
              } else {
                this.loadBanques();
              }
              this.loading = false;
            },
            error: (error) => {
              console.error('Erreur lors de la suppression de la banque:', error);
              const errorMessage = error.error?.message || 'Échec de la suppression de la banque.';
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              this.loading = false;
            }
          });
      }
    });
  }

  getSeverity(status: string): "success" | "secondary" | "info" | "warning" | "danger" | "contrast" | undefined {
    switch (status) {
      case Devise.EUR:
        return 'info';
      case Devise.USD:
        return 'warning';
      case Devise.MAD:
        return 'success';
      default:
        return 'secondary';
    }
  }

  isAdmin(): boolean {
    return this.authService.hasRole(Role.ADMIN);
  }
}
