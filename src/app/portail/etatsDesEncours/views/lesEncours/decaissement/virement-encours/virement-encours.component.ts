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
import {VirementResponseDto} from "../../../../../Virements/dtos/VirementResponseDto";
import {VirementService} from "../../../../../Virements/service/VirementService";
import {PageResponse} from "../../../../../global/types/PageResponse";



@Component({
  selector: 'app-virement-encours',
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
  templateUrl: './virement-encours.component.html',
  styleUrl: './virement-encours.component.scss',
  providers: [MessageService],
})
export class VirementEncoursComponent implements OnInit {
  virements: VirementResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours: number = 0;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;

  constructor(
    private virementService: VirementService,
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
        this.loadOutstandingVirements();
      }
    });
  }

  /**
   * Loads the outstanding virements for the bank.
   * @param event The lazy load event from the PrimeNG table.
   */
  loadOutstandingVirements(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.virementService
      .getOutstandingVirementsByBank(this.banqueIdFromRoute, page, size, sort)
      .subscribe({
        next: (response: PageResponse<VirementResponseDto>) => {
          this.virements = response.content;
          this.totalRecords = response.page.totalElements;
          this.loading = false;
          // Calculate the total outstanding amount
          this.totalEncours = this.virements.reduce(
            (sum, virement) => sum + virement.montant,
            0
          );
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/YYYY');
        },
        error: (error: Error) => {
          this.loading = false;
          console.error(
            'Erreur lors du chargement des virements en cours:',
            error
          );
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des virements en cours.',
          });
        },
      });
  }
}
