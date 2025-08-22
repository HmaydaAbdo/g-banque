import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { PaginatorModule } from 'primeng/paginator';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { Table } from 'primeng/table';
import { MessageService } from 'primeng/api';
import { Subscription } from 'rxjs';
import { DecimalPipe } from '@angular/common';
import {RecaputilatifDecaissementItem} from "../../dtos/RecaputilatifDecaissementItem";
import {RecapDecaissementService} from "../../services/recap-decaissement.service";
import {PageResponse} from "../../../../../../../global/types/PageResponse";

@Component({
  selector: 'app-recap-decaissement',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    CardModule,
    PaginatorModule,
    TooltipModule,
    ToastModule,
    DatePipe,
    DecimalPipe,
  ],
  templateUrl: './recap-decaissement.component.html',
  styleUrl: './recap-decaissement.component.scss',
  providers: [MessageService]
})
export class RecapDecaissementComponent implements OnInit, OnDestroy {

  // Table data state properties
  recapDecaissements: RecaputilatifDecaissementItem[] = [];
  totalRecords: number = 0;
  loading: boolean = true;

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];

  constructor(
    private recapitulatifDecaissementService: RecapDecaissementService,
    private messageService: MessageService
  ) { }

  ngOnInit(): void {
    // Initial load will be triggered by the p-table's onLazyLoad event
  }

  /**
   * Loads the disbursement data based on the lazy load event from the PrimeNG table.
   * This method handles pagination and sorting.
   * @param event The lazy load event object from the p-table.
   */
  loadDecaissements(event: TableLazyLoadEvent): void {
    this.loading = true;

    // Get page, size, and sort from the event. Use a default sort if none is provided.
    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sort = event.sortField && event.sortOrder
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,asc';

    // Call the service to fetch data from the back-end.
    this.subscriptions.push(
      this.recapitulatifDecaissementService.getRecapitulatifDecaissement(page, size, sort)
        .subscribe({
          next: (response: PageResponse<RecaputilatifDecaissementItem>) => {
            this.recapDecaissements = response.content;
            this.totalRecords = response.page.totalElements;
            this.loading = false;
          },
          error: (error) => {
            console.error('Error fetching disbursements:', error);
            this.loading = false;
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Échec du chargement des décaissements.'
            });
          }
        })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

}
