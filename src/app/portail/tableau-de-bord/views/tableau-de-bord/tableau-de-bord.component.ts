import {Component, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import {TooltipModule} from "primeng/tooltip";
import {CardModule} from "primeng/card";
import {ActivatedRoute, RouterLink, RouterLinkActive} from "@angular/router";
import {KpiCard} from "../../models/KpiCard";
import {MenuItem} from "primeng/api";
import {BreadcrumbModule} from "primeng/breadcrumb";



@Component({
  selector: 'app-tableau-de-bord',
  standalone: true,
  imports: [CommonModule, TooltipModule, CardModule, RouterLink, RouterLinkActive, BreadcrumbModule],
  templateUrl: './tableau-de-bord.component.html',
  styleUrl: './tableau-de-bord.component.scss'
})
export class TableauDeBordComponent implements OnInit {

  protected activatedRoute: ActivatedRoute=inject(ActivatedRoute);

  kpis: KpiCard[] = [];
  items: MenuItem[] | undefined;

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" }, { label: 'Tableau de bord' , icon:'pi pi-chart-bar',route: '/portail/tableau-de-bord'}, ]
    this.kpis = [
      {
        icon: 'pi pi-chart-line',
        label: 'Évolution Taux & Plafond',
        description: 'Suivez les changements des taux et plafonds pour une ligne fonctionnelle et une banque spécifiques.',
        route: '/portail/evolution-taux-plafond',
        iconColor: 'var(--orange-500)'
      },
      {
        icon: 'pi pi-chart-bar',
        label: 'Solde par Banque',
        description: 'Visualisez les soldes de vos comptes répartis par établissement bancaire.',
        route: '/portail/solde-par-banque',
        iconColor: 'var(--primary-color)'
      },

       {

        icon: 'pi pi-euro',
        label: 'Trésorerie Globale',
        description: 'Vue d\'ensemble de la trésorerie consolidée de toutes les banques.',
        route: '/portail/tresorerie-globale',
        iconColor: 'var(--green-500)'

       }
    ];
  }

}
