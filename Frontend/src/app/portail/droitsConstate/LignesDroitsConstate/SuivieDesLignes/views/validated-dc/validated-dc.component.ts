import {Component, OnInit, ViewChild, inject} from '@angular/core';
import {CommonModule, DecimalPipe} from '@angular/common';
import {ActivatedRoute} from '@angular/router';
import {TableLazyLoadEvent, TableModule} from 'primeng/table';
import {ToastModule} from 'primeng/toast';
import {MessageService} from 'primeng/api';
import {ToolbarModule} from 'primeng/toolbar';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {InputTextModule} from 'primeng/inputtext';
import {FormsModule} from '@angular/forms';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {BreadcrumbModule} from 'primeng/breadcrumb';
import {MenuItem} from 'primeng/api';
import {
  SpecificFunctionalLigneService
} from "../../../../../specific-Functional-Lignes/service/SpecificFunctionalLigneService";
import {AgregatedSuivieDcLigneResponseDto} from "../../Dtos/AgregatedSuivieDcLigneResponseDto";
import {SuivieDcLigneResponseDto} from "../../Dtos/SuivieDcLigneResponseDto";
import {LigneFonctionnelleService} from "../../../../../lignes-fonctionnelles/services/LigneFonctionnelleService";



@Component({
  selector: 'app-validated-dc',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ToastModule,
    ToolbarModule,
    ButtonModule,
    RippleModule,
    InputTextModule,
    FormsModule,
    TagModule,
    TooltipModule,
    BreadcrumbModule,
    DecimalPipe
  ],
  templateUrl: './validated-dc.component.html',
  styleUrl: './validated-dc.component.scss',
  providers: [MessageService]
})
export class ValidatedDcComponent implements OnInit {

  @ViewChild('dt') dt: any;

  aggregatedData!: AgregatedSuivieDcLigneResponseDto;
  dcLines: SuivieDcLigneResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  lineId!: number;
  lineType: 'general' | 'specific' = 'specific';
  items: MenuItem[] | undefined;

  private readonly specificLigneService = inject(SpecificFunctionalLigneService);
  private readonly generalLigneService = inject(LigneFonctionnelleService);
  private readonly messageService = inject(MessageService);
  private readonly route = inject(ActivatedRoute);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('lineId');
      if (idParam === null) {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'ID de ligne manquant dans l\'URL.',
          life: 3000
        });
        return;
      }
      this.lineId = +idParam;
      if (isNaN(this.lineId)) {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'ID de ligne invalide.',
          life: 3000
        });
        return;
      }


      this.route.queryParamMap.subscribe(queryParams => {
        const type = queryParams.get('type');
        this.lineType = (type === 'general') ? 'general' : 'specific';

        this.updateBreadcrumb();
      });

    });
  }

  private updateBreadcrumb(): void {
    const lineTypeLabel = this.lineType === 'general' ? 'Générale' : 'Spécifique';
    this.items = [
      {icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil'},
      {icon: 'pi pi-list', label: `Encours Droits Constatés - Ligne ${lineTypeLabel}`}
    ];
  }

  loadDcLines(event: TableLazyLoadEvent): void {
    if (!this.lineId) {
      return;
    }
    this.loading = true;
    // Use optional chaining and nullish coalescing to handle potential undefined values
    const page = (event.first ?? 0) / (event.rows ?? 10);
    const size = event.rows ?? 10;
    const sortField = event.sortField || 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const service = this.lineType === 'general'
      ? this.generalLigneService
      : this.specificLigneService;


    service.getDroitConstateByLineId(this.lineId, page, size, sort).subscribe({
      next: (response) => {
        this.aggregatedData = response;
        this.dcLines = response.page.content;
        // Correct the path to totalElements
        this.totalRecords = response.page.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }





}
