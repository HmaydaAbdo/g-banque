import {Component, OnInit, ViewChild} from '@angular/core';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import {ActivatedRoute} from '@angular/router';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {ToastModule} from 'primeng/toast';
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {TagModule} from 'primeng/tag';
import {BreadcrumbModule} from 'primeng/breadcrumb';
import {take} from 'rxjs/operators';
import {RapprochementResponse} from '../../dtos/RapprochementResponse';
import {TransactionResponseDto} from '../../../transactions/dtos/TransactionResponseDto';
import {RapprochementService} from "../../service/rapprochement-service.service";

@Component({
  selector: 'app-details-rapprochement',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ToastModule,
    ConfirmDialogModule,
    CardModule,
    ButtonModule,
    TagModule,
    BreadcrumbModule,
  ],
  templateUrl: './details-rapprochement.component.html',
  styleUrls: ['./details-rapprochement.component.scss'],
  providers: [MessageService, ConfirmationService, DecimalPipe, DatePipe]
})
export class DetailsRapprochementComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  rapprochementId!: number;
  rapprochementDetails: RapprochementResponse | null = null;
  items: MenuItem[] | undefined;

  // Pagination et chargement des transactions
  transactions: TransactionResponseDto[] = [];
  totalTransactionsRecords: number = 0;
  loadingTransactions: boolean = false;
  rows: number = 10;


  constructor(
    private route: ActivatedRoute,
    private rapprochementService: RapprochementService,
    private messageService: MessageService
  ) { }

  ngOnInit(): void {
    this.route.paramMap.pipe(take(1)).subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.rapprochementId = +id;
        this.loadRapprochementDetails();
      } else {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'ID de rapprochement manquant.', life: 5000 });
      }
    });
  }

  /**
   * Charge les détails du rapprochement principal (KPI et première page de transactions).
   */
  loadRapprochementDetails(page: number = 0, size: number = this.rows): void {
    if (!this.rapprochementId) return;

    this.loadingTransactions = true;

    // Le service charge les transactions paginées à l'intérieur du RapprochementResponse
    this.rapprochementService.getRapprochementById(this.rapprochementId, page, size)
      .subscribe({
        next: (data: RapprochementResponse) => {
          this.rapprochementDetails = data;
          this.transactions = data.transactions.content;
          this.totalTransactionsRecords = data.transactions.page.totalElements;
          this.loadingTransactions = false;
          this.updateBreadcrumb();
        },
        error: (err) => {
          console.error('Erreur lors du chargement des détails du rapprochement:', err);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: `Échec du chargement : ${err.error?.message || 'Erreur inconnue'}`,
            life: 5000
          });
          this.loadingTransactions = false;
        }
      });
  }

  /**
   * Gère l'événement de chargement paresseux de la table de transactions.
   */
  loadTransactionsLazy(event: TableLazyLoadEvent): void {
    if (!this.rapprochementId || !this.rapprochementDetails) return;

    const page = event.first! / event.rows!;
    const size = event.rows!;

    // Recharge les détails, ce qui recharge les transactions paginées à l'intérieur
    this.loadRapprochementDetails(page, size);
  }

  /**
   * Met à jour le fil d'Ariane avec les informations spécifiques du rapprochement.
   */
  updateBreadcrumb(): void {
    if (this.rapprochementDetails) {
      this.items = [
        { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
        { icon: 'pi pi-history', route: '/portail/rapprochement/historique', label: 'Historique des rapprochements' },
      ];
    }
  }

  /**
   * Détermine la couleur de la balise TVA Due
   */
  getTvaTagSeverity(tvaDue: number): 'success' | 'danger' | 'info' {
    if (tvaDue > 0) return 'success';
    if (tvaDue < 0) return 'danger';
    return 'info';
  }
}
