import {Component, OnInit, ViewChild} from '@angular/core';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {ToastModule} from 'primeng/toast';
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {catchError, debounceTime, distinctUntilChanged, finalize} from 'rxjs/operators';
import { Router} from '@angular/router';
import {BreadcrumbModule} from 'primeng/breadcrumb';
import {DropdownModule} from "primeng/dropdown";
import {RapprochementResponse} from "../../dtos/RapprochementResponse";
import {RapprochementService} from "../../service/rapprochement-service.service";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {RapprochementSearchDto} from "../../dtos/RapprochementSearchDto";
import {PageResponse} from "../../../global/types/PageResponse";
import {throwError} from "rxjs";



@Component({
  selector: 'app-historique-des-rapprochement',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    ToastModule,
    ConfirmDialogModule,
    TagModule,
    TooltipModule,
    BreadcrumbModule,
    DropdownModule
  ],
  templateUrl: './historique-des-rapprochement.component.html',
  styleUrls: ['./historique-des-rapprochement.component.scss'],
  providers: [MessageService, ConfirmationService, DecimalPipe, DatePipe]
})
export class HistoriqueDesRapprochementComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  rapprochements!: RapprochementResponse[];
  items!: MenuItem[] ;
  totalRecords!: number
  loading: boolean = false;
  first: number | undefined = 0;
  rows: number | undefined = 10;
  sortField: string | string[] = 'dateRapprochement';
  sortOrder: number = -1;
  searchForm!: FormGroup;

  banques: { id: number; nom: string }[] = [];
  loadingBanques: boolean = false;

  loadingReport: { [key: number]: boolean } = {};
  constructor(
    private rapprochementService: RapprochementService,
    private messageService: MessageService,
    private banqueService: BanqueService,
    private fb: FormBuilder,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-history', label: 'Historique des Rapprochements',route: '/portail/rapprochement/historique' }
    ];
    this.initSearchForm();
    this.searchForm.valueChanges
      .pipe(
        debounceTime(800),
        distinctUntilChanged()
      )
      .subscribe(() => {
        this.onSearch();
      });

     this.loadBanques({ first: 0, rows: 10 });
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      dateRapprochement: ['']
    });
  }

  loadRapprochements(event: TableLazyLoadEvent = { first: this.first, rows: this.rows, sortField: this.sortField, sortOrder: this.sortOrder, filters: {} }): void {
    this.loading = true;
    const first = event.first !== undefined ? event.first : this.first;
    const rows = event.rows !== undefined ? event.rows : this.rows;
    const sortField = event.sortField || this.sortField;
    const sortOrder = event.sortOrder === 1 ? 1 : -1;

    const page = (first as number) / (rows as number);

    const actualSortField = Array.isArray(sortField) ? sortField[0] : sortField;
    const sort = `${actualSortField},${sortOrder === 1 ? 'asc' : 'desc'}`;

    // Récupérer les critères de recherche du formulaire
    const searchCriteria: RapprochementSearchDto = this.searchForm.value;

    this.rapprochementService.searchRapprochements(searchCriteria, page, rows as number)
      .subscribe({
        next: (response: PageResponse<RapprochementResponse>) => {
          this.rapprochements = response.content;
          this.totalRecords = response.page.totalElements;
          this.loading = false;
        },
        error: (error) => {
          console.error('Erreur lors du chargement des rapprochements:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement de l\'historique des rapprochements. ' + (error.error?.message || ''),
            life: 3000
          });
          this.loading = false;
        }
      });
  }


  loadBanques(event: any) {
    this.loadingBanques = true;

    const page = event.first / event.rows || 0;
    const size = event.rows || 10000;

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
      // Déclenche un nouveau lazy load avec les nouveaux filtres et réinitialise la pagination
      this.dt.reset();
    } else {
      this.loadRapprochements();
    }
  }

  clearSearch(): void {
    this.searchForm.reset({
      banqueId: null,
      dateRapprochement: ''
    });
    this.first = 0;
    if (this.dt) {
      // Réinitialise la table et recharge
      this.dt.clear();
      this.loadRapprochements(this.dt.createLazyLoadMetadata());
    } else {
      this.loadRapprochements();
    }
  }

  /**
   * Navigue vers la page de détails d'un rapprochement (où les transactions sont paginées)
   */
  viewRapprochementDetails(rapprochementId: number): void {
    this.router.navigate(['/portail/rapprochement/details', rapprochementId]);
  }

  downloadRapprochementReport(rapprochement: RapprochementResponse): void {
    if (!rapprochement.id) {
      this.messageService.add({severity: 'warn', summary: 'Alerte', detail: 'ID du rapprochement manquant.', life: 3000});
      return;
    }

    // Active l'indicateur de chargement spécifique
    this.loadingReport[rapprochement.id] = true;

    this.rapprochementService.downloadReport(rapprochement.id)
      .pipe(
        // Gère l'erreur
        catchError(error => {
          console.error('Erreur lors du téléchargement du rapport:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur de Téléchargement',
            detail: 'Échec de la génération ou du téléchargement du rapport.',
            life: 5000
          });
          // Propage l'erreur pour la gestion du finalize
          return throwError(() => new Error('Download failed'));
        }),
        // Désactive le chargement quel que soit le résultat (succès ou erreur)
        finalize(() => {
          this.loadingReport[rapprochement.id] = false;
        })
      )
      .subscribe((response: Blob) => {
        // Création de l'objet Blob pour le téléchargement
        const fileURL = URL.createObjectURL(response);
        const link = document.createElement('a');

        link.href = fileURL;
        link.download = `Rapport_Rapprochement_${rapprochement.banqueNom}_${rapprochement.dateRapprochement}.pdf`;

        // Simule le clic pour démarrer le téléchargement
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(fileURL); // Libère la mémoire

        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Le rapport PDF a été généré et téléchargé.',
          life: 3000
        });
      });
  }
}
