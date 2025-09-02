import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CommonModule, DatePipe} from '@angular/common';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {ToastModule} from 'primeng/toast';
import {TooltipModule} from 'primeng/tooltip';
import {PaginatorModule} from 'primeng/paginator';
import {MessageService} from 'primeng/api';
import {RippleModule} from 'primeng/ripple';
import {LcnResponseDTO} from "../../../../../LCN/dtos/LcnResponseDTO";
import {LcnService} from "../../../../../LCN/service/LcnService";
import {PageResponse} from "../../../../../global/types/PageResponse";



@Component({
  selector: 'app-lcn-encours',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    CardModule,
    ButtonModule,
    ToastModule,
    TooltipModule,
    PaginatorModule,
    DatePipe,
    RippleModule,
  ],
  templateUrl: './lcn-encours.component.html',
  styleUrl: './lcn-encours.component.scss',
  providers: [MessageService],
})
export class LcnEncoursComponent implements OnInit {
  lcns: LcnResponseDTO[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours: number = 0;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;

  constructor(
    private lcnService: LcnService,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private datePipe: DatePipe,
  ) {
  }

  ngOnInit(): void {
    // Get the bank ID from the route's parent parameter
    this.route.parent!.paramMap.subscribe((params) => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueIdFromRoute = +id;
        this.loadOutstandingLcns();
      }
    });
  }

  /**
   * Loads the outstanding LCNs for the bank.
   * @param event The lazy load event from the PrimeNG table.
   */
  loadOutstandingLcns(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEcheance,desc';

    this.lcnService
      .getOutstandingLcnsByBank(this.banqueIdFromRoute, page, size, sort)
      .subscribe({
        next: (response: PageResponse<LcnResponseDTO>) => {
          this.lcns = response.content;
          this.totalRecords = response.page.totalElements;
          this.loading = false;
          // Calculate the total outstanding amount
          this.totalEncours = this.lcns.reduce(
            (sum, lcn) => sum + lcn.montant,
            0
          );
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/YYYY');
        },
        error: (error: Error) => {
          this.loading = false;
          console.error(
            'Erreur lors du chargement des LCNs en cours:',
            error
          );
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des LCNs en cours.',
          });
        },
      });
  }
}
