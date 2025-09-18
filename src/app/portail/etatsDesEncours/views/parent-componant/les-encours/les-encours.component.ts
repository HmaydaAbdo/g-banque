import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterOutlet, NavigationEnd } from "@angular/router";
import { PanelMenuModule } from "primeng/panelmenu";
import { MenuItem } from 'primeng/api';
import { CommonModule } from '@angular/common';
import { BankDetailsComponent } from "../../shared-componanat/bank-details/bank-details.component";
import { Ripple } from "primeng/ripple";
import { BreadcrumbModule } from "primeng/breadcrumb";
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-les-encours',
  standalone: true,
  imports: [
    RouterOutlet,
    PanelMenuModule,
    CommonModule,
    BankDetailsComponent,
    Ripple,
    BreadcrumbModule
  ],
  templateUrl: './les-encours.component.html',
  styleUrl: './les-encours.component.scss'
})
export class LesEncoursComponent implements OnInit {

  items: MenuItem[] = [];
  breadcrumbItems: MenuItem[] = [];
  banqueId: string | null = null;

  constructor(private route: ActivatedRoute, private router: Router) {}

  private breadcrumbMap: { [key: string]: { label: string, icon: string } } = {
    'rapprochement': { label: 'Rapprochement des encours', icon: 'pi pi-sync' },
    'echeances-leasing': { label: 'Décaissements ==> Échéances Leasing', icon: 'pi pi-list' },
    'echeances-cmt': { label: 'Décaissements ==> Échéances CMT', icon: 'pi pi-list' },
    'cheques': { label: 'Décaissements ==> Chèques', icon: 'pi pi-list' },
    'lcn': { label: 'Décaissements ==> LCN', icon: 'pi pi-list' },
    'virements': { label: 'Décaissements ==> Virements', icon: 'pi pi-list' },
    'mise-a-disposition': { label: 'Décaissements ==> Mise à disposition', icon: 'pi pi-list' },
    'recap-decaissement': { label: 'Récap Décaissements ', icon: 'pi pi-list' }
  };

  ngOnInit() {
    this.breadcrumbItems = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', route: '/portail/etats-des-encours', label: "Suivi des encours" },
      { icon: 'pi pi-sync', route: '', label: "Rapprochement des encours" } // par défaut
    ];

    // Récupérer banqueId et mettre à jour le fil d'Ariane
    // C'est ici que se trouve la correction principale.
    this.route.paramMap.subscribe(params => {
      this.banqueId = params.get('banqueId');
      this.updateMenuItems();
      // Appel de la méthode de mise à jour du fil d'Ariane ici pour garantir que le banqueId est disponible.
      this.updateBreadcrumb();
    });

    // Mettre à jour le breadcrumb quand la route enfant change
    // C'est nécessaire pour les navigations futures à l'intérieur du composant.
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.updateBreadcrumb());
  }

  updateBreadcrumb() {
    const child = this.route.firstChild?.snapshot;
    if (child?.routeConfig?.path && this.banqueId) {
      const segment = child.routeConfig.path;
      const bc = this.breadcrumbMap[segment];
      if (bc) {
        this.breadcrumbItems[2] = {
          icon: bc.icon,
          route: `/portail/etats-des-encours/${this.banqueId}/${segment}`,
          label: bc.label
        };
      }
    }
  }

  updateMenuItems() {
    this.items = [
      {
        label: 'Rapprochement',
        icon: 'pi pi-sync',
        routerLink: ['/portail/etats-des-encours', this.banqueId, 'rapprochement']
      },
      {
        label: 'Récap des décaissements',
        icon: 'pi pi-briefcase',
        routerLink: ['/portail/etats-des-encours', this.banqueId, 'recap-decaissement']
      },
      {
        label: 'Récap des encaissements',
        icon: 'pi pi-briefcase',
        routerLink: ['/portail/etats-des-encours', this.banqueId, 'recap-encaissement']
      },
      {
        label: 'Décaissements',
        icon: 'pi pi-arrow-circle-left',
        items: [
          {
            label: 'Échéances leasing',
            icon: 'pi pi-list',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'echeances-leasing']
          },
          {
            label: 'Échéances CMT',
            icon: 'pi pi-list',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'echeances-cmt']
          },
          {
            label: 'Chèques',
            icon: 'pi pi-list',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'cheques']
          },
          {
            label: 'LCN',
            icon: 'pi pi-list',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'lcn']
          },
          {
            label: 'Virements',
            icon: 'pi pi-list',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'virements']
          },
          {
            label: 'Mise à disposition',
            icon: 'pi pi-list',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'mise-a-disposition']
          },
          {
            label: "Préfinancement",
            icon: 'pi pi-list',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'prefinancement-remboursement']
          },
          {
            label: "Droits Constatés",
            icon: 'pi pi-list',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'dc-remboursement']
          }
        ]
      },
      {
        label: 'Encaissements',
        icon: 'pi pi-arrow-circle-right',
        items: [
          {
            label: "Préfinancement",
            icon: 'pi pi-list',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'prefinancement-deblocage']
          },
          {
            label: "Droits Constatés",
            icon: 'pi pi-list',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'dc-deblocage']
          },
          {
            label: "Décomptes",
            icon: 'pi pi-list',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'decomptes']
          }
        ]
      }
    ];
  }

  toggleMenuItem(item: MenuItem): void {
    if (item.items) {
      item.expanded = !item.expanded;
    }
  }

}
