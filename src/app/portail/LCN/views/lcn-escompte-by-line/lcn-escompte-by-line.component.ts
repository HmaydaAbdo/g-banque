import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';
import { MessageService, MenuItem } from 'primeng/api';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule } from '@angular/forms';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { LigneFonctionnelleService } from '../../../lignes-fonctionnelles/services/LigneFonctionnelleService';
import { AgregatedSuivieLcnLigneResponseDto } from '../../dtos/AgregatedSuivieLcnLigneResponseDto';
import { LcnResponseDTO } from '../../dtos/LcnResponseDTO';
import { Type } from '../../../Virements/Type';
import { NatureLcn } from '../../dtos/NatureLcn';

@Component({
  selector: 'app-lcn-escompte-by-line',
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
    DecimalPipe,
    DatePipe,
    RouterLink
  ],
  templateUrl: './lcn-escompte-by-line.component.html',
  styleUrl: './lcn-escompte-by-line.component.scss',
  providers: [MessageService]
})
export class LcnEscompteByLineComponent implements OnInit {

  @ViewChild('dt') dt: any;

  aggregatedData?: AgregatedSuivieLcnLigneResponseDto;
  lcnList: LcnResponseDTO[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  lineId!: number;
  lineType: 'general' | 'specific' = 'specific';

  items: MenuItem[] = [];

  // Exposer les enums pour le template
  Type = Type;

  constructor(
    private ligneFonctionnelleService: LigneFonctionnelleService,
    private messageService: MessageService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Récupération de l'ID de la ligne depuis l'URL
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

  /**
   * Initialise le fil d'Ariane (breadcrumb)
   */
  private updateBreadcrumb(): void {
    const lineTypeLabel = this.lineType === 'general' ? 'Générale' : 'Spécifique';

    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      {
        icon: 'pi pi-list',
        route: '/portail/LCN/encaissements',
        label: 'LCN Encaissements'
      },
      {
        icon: 'pi pi-chart-line',
        label: `Suivi Escompte Commercial - Ligne Fonctionnelle  ${lineTypeLabel}`,
      }
    ];
  }

  /**
   * Charge les LCN ESCOMPTE de la ligne fonctionnelle avec pagination
   * @param event Événement de lazy loading de PrimeNG
   */
  loadLcnLines(event: TableLazyLoadEvent): void {
    if (!this.lineId) {
      return;
    }

    this.loading = true;

    const page = (event.first ?? 0) / (event.rows ?? 10);
    const size = event.rows ?? 10;
    const sortField = event.sortField || 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.ligneFonctionnelleService.getLcnByLineId(this.lineId, page, size, sort).subscribe({
      next: (response: AgregatedSuivieLcnLigneResponseDto) => {
        this.aggregatedData = response;
        this.lcnList = response.page.content;
        this.totalRecords = response.page.page.totalElements;
        this.loading = false;

        console.log(`Chargé ${this.lcnList.length} LCN pour un total de ${response.total} DH`);
      },
      error: (error) => {
        console.error('Erreur lors du chargement des LCN:', error);
        this.loading = false;
      }
    });
  }



}
