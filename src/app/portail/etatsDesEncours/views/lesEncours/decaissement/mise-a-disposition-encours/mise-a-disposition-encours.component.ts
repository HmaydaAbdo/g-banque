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
import {MiseADispositionResponseDto} from "../../../../../Mise-à-disposition/dtos/MiseADispositionResponseDto";
import {MiseADispositionService} from "../../../../../Mise-à-disposition/service/MiseADispositionService";
import {PageResponse} from "../../../../../global/types/PageResponse";

@Component({
  selector: 'app-mise-a-disposition-encours',
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
  templateUrl: './mise-a-disposition-encours.component.html',
  styleUrl: './mise-a-disposition-encours.component.scss',
  providers: [MessageService],
})
export class MiseADispositionEncoursComponent implements OnInit {
  misesADisposition: MiseADispositionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours: number = 0;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;

  constructor(
    private miseADispositionService: MiseADispositionService,
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
        this.loadOutstandingMisesADisposition();
      }
    });
  }

  /**
   * Loads the outstanding mise à dispositions for the bank.
   * @param event The lazy load event from the PrimeNG table.
   */
  loadOutstandingMisesADisposition(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.miseADispositionService
      .getOutstandingMisesADispositionByBank(this.banqueIdFromRoute, page, size, sort)
      .subscribe({
        next: (response: PageResponse<MiseADispositionResponseDto>) => {
          this.misesADisposition = response.content;
          this.totalRecords = response.page.totalElements;
          this.loading = false;
          // Calculate the total outstanding amount
          this.totalEncours = this.misesADisposition.reduce(
            (sum, miseADisposition) => sum + miseADisposition.montant,
            0
          );
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/YYYY');
        },
        error: (error: Error) => {
          this.loading = false;
          console.error(
            'Erreur lors du chargement des mises à disposition en cours:',
            error
          );
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des mises à disposition en cours.',
          });
        },
      });
  }
}
