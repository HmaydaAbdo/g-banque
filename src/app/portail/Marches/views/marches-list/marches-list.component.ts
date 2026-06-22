import {Component, OnInit, ViewChild} from '@angular/core';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {ImageModule} from 'primeng/image';
import {ToastModule} from 'primeng/toast';
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {ToolbarModule} from 'primeng/toolbar';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {debounceTime, distinctUntilChanged} from 'rxjs/operators';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {BreadcrumbModule} from 'primeng/breadcrumb';

// Import DTOs, service, and models for Marche
import {MarcheResponseDTO} from '../../dtos/marche-response.dto';
import {MarcheSearchDTO} from '../../dtos/marche-search.dto';
import {MarcheService} from "../../service/marche-service.service";
import {PageResponse} from "../../../global/types/PageResponse";
import {DropdownModule} from "primeng/dropdown";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {AuthService} from "../../../../auth/services/auth.service";
import {Role} from "../../../../auth/enum/Role";


@Component({
  selector: 'app-marches-list',
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
    RouterLinkActive,
    DropdownModule
  ],
  templateUrl: './marches-list.component.html',
  styleUrls: ['./marches-list.component.scss'],
  providers: [MessageService, ConfirmationService, DecimalPipe, DatePipe]
})
export class MarchesListComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  marches!: MarcheResponseDTO[];
  items: MenuItem[] | undefined;
  totalRecords: number = 0;
  loading: boolean = false;
  first: number | undefined = 0;
  rows: number | undefined = 10;
  sortField: string | string[] = 'id';
  sortOrder: number = 1;
  searchForm!: FormGroup;

  banques: { id: number; nom: string }[] = [];
  loadingBanques: boolean = false;
  nantiOptions = [
    { label: 'Tous', value: null },
    { label: 'Nanti', value: true },
    { label: 'Non Nanti', value: false }
  ];


  constructor(
    private marcheService: MarcheService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private banqueService: BanqueService,
    private authService: AuthService,
    private router:Router,
    private fb: FormBuilder
  ) { }

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' }, { icon: 'pi pi-file-o', route: '/portail/marches',label: 'Liste des marchés' }];
    this.initSearchForm();
    this.searchForm.valueChanges
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
      nom: [''],
      numero: [''],
      maitreDouvrage: [''],
      banqueId: [null],
      isNanti: [null]
    });
  }

  loadMarches(event: TableLazyLoadEvent = { first: this.first, rows: this.rows, sortField: this.sortField, sortOrder: this.sortOrder, filters: {} }): void {
    this.loading = true;
    const first = event.first !== undefined ? event.first : this.first;
    const rows = event.rows !== undefined ? event.rows : this.rows;
    const sortField = event.sortField || this.sortField;
    const sortOrder = event.sortOrder === 1 ? 1 : -1;

    const page = (first as number) / (rows as number);

    const actualSortField = Array.isArray(sortField) ? sortField[0] : sortField;
    const sort = `${actualSortField},${sortOrder === 1 ? 'asc' : 'desc'}`;

    const searchCriteria: MarcheSearchDTO = this.searchForm.value;

    this.marcheService.getAllMarches(searchCriteria, page, rows as number, sort)
      .subscribe({
        next: (response: PageResponse<MarcheResponseDTO>) => {
          this.marches = response.content;
          this.totalRecords = response.page.totalElements;
          this.loading = false;
        },
        error: (error) => {
          console.error('Erreur lors du chargement des marchés:', error);
          this.loading = false;
        }
      });
  }


  loadBanques(event: any) {
    this.loadingBanques = true;

    const page = event.first / event.rows || 0;
    const size = event.rows || 10;

    const criteria = { nom: event.filter }; // filtre par nom si tapé

    this.banqueService.getAllBanques(criteria, page, size).subscribe({
      next: (data) => {
        this.banques = data.content.map(b => ({ id: b.id, nom: b.nom }));
        this.loadingBanques = false;
      },
      error: () => {
        this.loadingBanques = false;
      }
    });
  }

  onSearch(): void {
    this.first = 0;
    if (this.dt) {
      this.dt.reset();
    } else {
      this.loadMarches({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      } as TableLazyLoadEvent);
    }
  }

  clearSearch(): void {
    this.searchForm.reset();
    this.first = 0;
    if (this.dt) {
      this.dt.clear();
      this.loadMarches(this.dt.createLazyLoadMetadata());
    } else {
      this.loadMarches({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      } as TableLazyLoadEvent);
    }
  }

  deleteMarche(marche: MarcheResponseDTO): void {
    const confirmationMessageHtml = `
    <div class="text-center">
        <i class="pi pi-exclamation-triangle" style="margin-right: 5px;"></i>
        <p>Êtes-vous sûr de vouloir supprimer le marché "<strong>${marche.nom}</strong>" ?</p>
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
        this.marcheService.deleteMarche(marche.id!)
          .subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Le marché a été supprimé avec succès.', life: 3000 });
              if (this.dt) {
                this.loadMarches(this.dt.createLazyLoadMetadata());
              } else {
                this.loadMarches();
              }
              this.loading = false;
            },
            error: (error) => {
              console.error('Erreur lors de la suppression du marché:', error);
              this.loading = false;
            }
          });
      }
    });
  }

  viewSpecificLines(marcheId: number): void {
    this.router.navigate(['/portail/lignes-fonctionnelles-specifiques/marche', marcheId]);
  }

  isAdmin():boolean{
    return this.authService.hasRole(Role.ADMIN)
  }
}
