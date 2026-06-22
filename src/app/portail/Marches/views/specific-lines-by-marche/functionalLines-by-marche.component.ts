import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
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
import { LigneFonctionnelleResponse } from "../../../lignes-fonctionnelles/dtos/LigneFonctionnelleResponse";
import { LigneFonctionnelleService } from "../../../lignes-fonctionnelles/services/LigneFonctionnelleService";
import {LineNavigationService, LineType} from "../../../lignes-fonctionnelles/services/LineNavigationService";

@Component({
  selector: 'app-functional-lines-by-marche',
  standalone: true,
  imports: [
    CommonModule, TableModule, TabViewModule, BreadcrumbModule, ButtonModule,
    ToastModule, TooltipModule, TagModule, DecimalPipe, DatePipe,
    RouterLink, RouterLinkActive
  ],
  templateUrl: './functionalLines-by-marche.component.html',
  styleUrls: ['./functionalLines-by-marche.component.scss']
})
export class FunctionalLinesByMarcheComponent implements OnInit {
  @ViewChild('dtSpecific') tableSpecific!: Table;
  @ViewChild('dtGeneral') tableGeneral!: Table;

  marcheId!: number;
  marche?: MarcheResponseDTO;

  // Données pour les lignes spécifiques
  specificLines: LigneFonctionnelleSpecifiqueResponse[] = [];
  totalRecordsSpecific = 0;
  loadingSpecific = true;

  // Données pour les lignes générales
  generalLines: LigneFonctionnelleResponse[] = [];
  totalRecordsGeneral = 0;
  loadingGeneral = true;

  items: MenuItem[] = [];
  activeTabIndex = 0;

  // Exposer LineType pour le template
  readonly LineType = LineType;

  constructor(
    private route: ActivatedRoute,
    private specificFunctionalLigneService: SpecificFunctionalLigneService,
    private ligneFonctionnelleService: LigneFonctionnelleService,
    private marcheService: MarcheService,
    private messageService: MessageService,
    private authService: AuthService,
    private lineNavigationService: LineNavigationService,
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
      { icon: 'pi pi-eye', label: 'Lignes Fonctionnelles' }
    ];
  }

  loadMarcheInfo(): void {
    this.marcheService.getMarcheById(this.marcheId).subscribe({
      next: (marche) => this.marche = marche,
      error: (err) => console.error('Erreur lors du chargement du marché:', err)
    });
  }

  // ==========================================================================
  // CHARGEMENT DES LIGNES SPÉCIFIQUES
  // ==========================================================================

  loadSpecificLines(event: TableLazyLoadEvent): void {
    this.loadingSpecific = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField as string || 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.specificFunctionalLigneService.getLinesByMarche(this.marcheId, page, size, sort).subscribe({
      next: (res) => {
        this.specificLines = res.content;
        this.totalRecordsSpecific = res.page.totalElements;
        this.loadingSpecific = false;
      },
      error: () => {
        this.loadingSpecific = false;
      }
    });
  }

  // ==========================================================================
  // CHARGEMENT DES LIGNES GÉNÉRALES
  // ==========================================================================

  loadGeneralLines(event: TableLazyLoadEvent): void {
    this.loadingGeneral = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField as string || 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;


    this.ligneFonctionnelleService.getLignesFonctionnellesByMarcheId(
      this.marcheId,
      page,
      size,
      sort
    ).subscribe({
      next: (res) => {
        this.generalLines = res.content;
        this.totalRecordsGeneral = res.page.totalElements;
        this.loadingGeneral = false;
      },
      error: () => {
        this.loadingGeneral = false;
      }
    });
  }

  // ==========================================================================
  // HELPERS
  // ==========================================================================

  getSoldeClass(solde: number | null): string {
    if (solde === null || solde === undefined) return '';
    return solde < 0 ? 'text-red-600 font-bold' : 'text-green-600 font-bold';
  }

  /**
   * Redirige vers l'écran de détails en utilisant le LineNavigationService
   * @param lineId - ID de la ligne
   * @param natureLabel - Label de la nature (ex: "Factoring", "Préfinancement")
   * @param lineType - Type de ligne (SPECIFIC ou GENERAL)
   */
  redirectToLineDetails(lineId: number, natureLabel: string, lineType: LineType): void {
    this.lineNavigationService.redirectToLineDetails(lineId, natureLabel, lineType);
  }

  /**
   * Alternative : Redirection automatique en passant l'objet ligne complet
   * Détecte automatiquement le type de ligne
   * @param ligne - Objet ligne (spécifique ou générale)
   */
  redirectToLineDetailsAuto(ligne: LigneFonctionnelleSpecifiqueResponse | LigneFonctionnelleResponse): void {
    this.lineNavigationService.redirectToLineDetailsAuto(ligne);
  }

  /**
   * Vérifie si une nature de ligne a un écran de suivi configuré
   * @param natureLabel - Label de la nature
   */
  hasTrackingScreen(natureLabel: string): boolean {
    return this.lineNavigationService.hasTrackingScreen(natureLabel);
  }
}
