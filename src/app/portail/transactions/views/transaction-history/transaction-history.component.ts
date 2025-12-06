import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe, CurrencyPipe } from '@angular/common'; // Ajout de CurrencyPipe
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { PaginatorModule } from 'primeng/paginator';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
// IMPORTANT: ConfirmationService importé
import {MenuItem, MessageService, ConfirmationService} from 'primeng/api';
import { DropdownModule } from 'primeng/dropdown';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { TagModule } from 'primeng/tag';
// IMPORTANT: ConfirmDialogModule et TooltipModule
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';


// Import your DTOs and services
import { TransactionResponseDto } from "../../dtos/TransactionResponseDto";
import { BanqueResponse } from "../../../comptes-bancaire/dtos/BanqueResponse";
import { TransactionService } from "../../service/TransactionService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { TransactionSource } from "../../dtos/TransactionSource";
import { TransactionSearchDto } from "../../dtos/TransactionSearchDto";
import { PageResponse } from "../../../global/types/PageResponse";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {RouterLinkActive, Router} from "@angular/router"; // Ajout de Router


@Component({
  selector: 'app-service',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    CardModule,
    ButtonModule,
    ToastModule,
    ToolbarModule,
    PaginatorModule,
    InputTextModule,
    FormsModule,
    ReactiveFormsModule,
    DatePipe,
    CurrencyPipe, // Ajout du pipe de devise
    DropdownModule,
    InputGroupModule,
    InputGroupAddonModule,
    TagModule,
    BreadcrumbModule,
    RouterLinkActive,
    ConfirmDialogModule, // IMPORTANT
    TooltipModule // Ajouté pour le tooltip sur les icônes
  ],
  templateUrl: './transaction-history.component.html',
  styleUrl: './transaction-history.component.scss',
  providers: [MessageService, ConfirmationService] // IMPORTANT
})
export class TransactionHistoryComponent implements OnInit, OnDestroy {

  @ViewChild('dt') dt!: Table;

  transactions: TransactionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  lastLazyLoadEvent: TableLazyLoadEvent | undefined; // Pour recharger les données après suppression

  searchForm!: FormGroup;
  transactionSources: string[] = [];
  banquesOptions: { label: string, value: number | null }[] = [];

  items: MenuItem[] | undefined;
  private subscriptions: Subscription[] = [];

  constructor(
    private transactionService: TransactionService,
    private banqueService: BanqueService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService, // Injecté
    private router: Router // Injecté
  ) { }

  ngOnInit(): void {
    this.initSearchForm();
    this.loadBanques();
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/historique-des-transactions', label:"Historique des transactions" },
    ]
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * Initializes the reactive form and sets up a single subscription for all filter changes.
   */
  initSearchForm(): void {
    this.searchForm = this.fb.group({
      libelle: [''],
      banqueId: [null],
      transactionSource: [null]
    });

    this.transactionSources = Object.values(TransactionSource);

    // Consolidate all filter subscriptions into a single one
    const formValueChangesSub = this.searchForm.valueChanges
      .pipe(
        debounceTime(300),
        // Use a more robust distinctUntilChanged check for form objects
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
      )
      .subscribe(() => {
        this.onSearch();
      });

    this.subscriptions.push(formValueChangesSub);
  }

  /**
   * Loads the list of banks for the filter dropdown.
   */
  loadBanques(): void {
    this.subscriptions.push(
      this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc')
        .subscribe({
          next: (pageResponse: PageResponse<BanqueResponse>) => {
            this.banquesOptions = [
              { label: 'Toutes les banques', value: null },
              ...pageResponse.content.map(banque => ({ label: banque.nom, value: banque.id! }))
            ];
          },
          error: (err) => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des banques pour le filtre.', life: 3000 });
            console.error('Error loading banks for filter:', err);
          }
        })
    );
  }

  /**
   * This method is the single source of truth for loading data from the backend.
   * It's called by the p-table's (onLazyLoad) event.
   */
  loadTransactions(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.lastLazyLoadEvent = event; // Sauvegarde l'événement pour un rechargement futur

    // Get the current search criteria from the reactive form
    const searchDto: TransactionSearchDto = this.searchForm.value;

    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sort = event.sortField && event.sortOrder
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.subscriptions.push(
      this.transactionService.getTransactionHistory(searchDto, page, size, sort)
        .subscribe({
          next: (response: PageResponse<TransactionResponseDto>) => {
            this.transactions = response.content;
            this.totalRecords = response.page.totalElements;
            this.loading = false;
          },
          error: (error) => {
            this.loading = false;
            console.error('Erreur lors du chargement des transactions:', error);
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Échec du chargement de l\'historique des transactions.'
            });
          }
        })
    );
  }

  /**
   * Triggers a new search by resetting the table's state and manually
   * calling the data loading method with the new metadata.
   */
  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      // Get the metadata from the table and explicitly call the load method
      this.loadTransactions(this.dt.createLazyLoadMetadata());
    }
  }

  /**
   * Clears all filter criteria and triggers a new search.
   */
  clearSearch(): void {
    this.searchForm.reset({
      libelle: '',
      banqueId: null,
      transactionSource: null
    });
    // This will trigger the form's valueChanges subscription, which in turn calls onSearch().
  }


  /**
   * Ouvre la boîte de dialogue de confirmation avant la suppression.
   */
  confirmDeleteTransaction(id: number): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer cette transaction? Cette action est irréversible.',
      header: 'Confirmation de suppression',
      icon: 'pi pi-info-circle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: 'p-button-danger p-button-text',
      rejectButtonStyleClass: 'p-button-text p-button-text',
      acceptIcon: 'none',
      rejectIcon: 'none',
      accept: () => {
        this.deleteTransaction(id);
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'La suppression a été annulée.' });
      }
    });
  }

  /**
   * Exécute la suppression de la transaction via le service.
   */
  deleteTransaction(id: number): void {
    this.loading = true;
    this.subscriptions.push(
      this.transactionService.deleteTransaction(id).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: `Transaction ID ${id} supprimée avec succès.`,
            life: 3000
          });
          // Recharger les transactions après la suppression
          if (this.lastLazyLoadEvent) {
            this.loadTransactions(this.lastLazyLoadEvent);
          } else {
            // Si l'événement n'est pas disponible, forcer un rechargement par défaut
            this.dt.first = 0;
            this.loadTransactions(this.dt.createLazyLoadMetadata());
          }
        },
        error: (error) => {
          this.loading = false;
          const errorMessage = error.error?.message || 'Échec de la suppression de la transaction.';
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: errorMessage
          });
          console.error('Erreur de suppression:', error);
        }
      })
    );
  }
}
