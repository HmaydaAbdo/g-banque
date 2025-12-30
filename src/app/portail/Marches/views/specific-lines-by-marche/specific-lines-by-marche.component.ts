import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import {ActivatedRoute, Router, RouterLink, RouterLinkActive} from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { MenuItem, MessageService } from 'primeng/api';
import { ToastModule } from "primeng/toast";
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';

import { SpecificFunctionalLigneService } from "../../../specific-Functional-Lignes/service/SpecificFunctionalLigneService";
import { MarcheService } from "../../service/marche-service.service";
import { MarcheResponseDTO } from "../../dtos/marche-response.dto";
import { LigneFonctionnelleSpecifiqueResponse } from "../../../specific-Functional-Lignes/dtos/LigneFonctionnelleSpecifiqueResponse";
import { AuthService } from "../../../../auth/services/auth.service";
import { Role } from "../../../../auth/enum/Role";

@Component({
  selector: 'app-specific-lines-by-marche',
  standalone: true,
  imports: [
    CommonModule, TableModule, BreadcrumbModule, ButtonModule, ToastModule,
    TooltipModule, TagModule, DecimalPipe, DatePipe, RouterLink, RouterLinkActive
  ],
  providers: [MessageService],
  templateUrl: './specific-lines-by-marche.component.html',
  styleUrls: ['./specific-lines-by-marche.component.scss']
})
export class SpecificLinesByMarcheComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  marcheId!: number;
  marche?: MarcheResponseDTO;
  specificLines: LigneFonctionnelleSpecifiqueResponse[] = [];
  totalRecords = 0;
  loading = true;
  items: MenuItem[] = [];

  constructor(
    private route: ActivatedRoute,
    private specificFunctionalLigneService: SpecificFunctionalLigneService,
    private marcheService: MarcheService,
    private messageService: MessageService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.marcheId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadMarcheInfo();
    this.initBreadcrumb();
  }

  private initBreadcrumb(): void {
    this.items = [
      { icon: 'pi pi-home', label: 'Accueil', route: '/portail/accueil' },
      { icon: 'pi pi-list', label: 'Marchés', route: '/portail/marches' },
      { icon: 'pi pi-eye', label: 'Lignes Spécifiques' }
    ];
  }

  loadMarcheInfo(): void {
    this.marcheService.getMarcheById(this.marcheId).subscribe({
      next: (marche) => this.marche = marche,
      error: () => this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Impossible de charger les informations du marché.'
      })
    });
  }

  loadSpecificLines(event: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField as string || 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.specificFunctionalLigneService.getLinesByMarche(this.marcheId, page, size, sort).subscribe({
      next: (res) => {
        this.specificLines = res.content;
        this.totalRecords = res.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des lignes spécifiques.'
        });
      }
    });
  }

  getSoldeClass(solde: number | null): string {
    if (solde === null || solde === undefined) return '';
    return solde < 0 ? 'text-red-600 font-bold' : 'text-green-600 font-bold';
  }

  redirectToLineDetails(lineId: number, label: string): void {
    // Normalisation pour éviter les erreurs de casse ou d'espaces
    const nature = label?.trim();

    switch (nature) {
      case 'Factoring':
        this.router.navigate(['/portail/factoring-encours', lineId]);
        break;

      case 'Préfinancement':
        this.router.navigate(['/portail/prefi-encours', lineId]);
        break;

      case 'Droit constaté':
        this.router.navigate(['/portail/dc-encours', lineId]);
        break;

      default:
        // Message d'avertissement en français
        this.messageService.add
        ({ severity: 'warning',
          summary: 'Aucun écran de suivi n\'est configuré',
          detail:    `Aucun écran de suivi n'est configuré pour la nature : "${nature}"`+'Action impossible'
        });

        console.warn(`Redirection non gérée pour le label: ${nature}`);
        break;
    }

  }

}
