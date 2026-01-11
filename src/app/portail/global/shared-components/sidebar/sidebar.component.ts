import { Component, EventEmitter, Input, Output, ViewChild, OnInit, inject } from '@angular/core';
import { Sidebar, SidebarModule } from 'primeng/sidebar';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { StyleClassModule } from 'primeng/styleclass';
import { Router, RouterLink, RouterLinkActive } from '@angular/router'; // Import de Router
import { CommonModule } from '@angular/common';
import {Role} from "../../../../auth/enum/Role";
import {AuthService} from "../../../../auth/services/auth.service";


interface SidebarItem {
  label?: string;
  icon?: string;
  routerLink?: string;
  command?: () => void;
  separator?: boolean;
  items?: SidebarItem[];
  styleClass?: string;
  expanded?: boolean;
  badge?: string;
  badgeClass?: string;
  roles?: Role[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    SidebarModule,
    ButtonModule,
    RippleModule,
    StyleClassModule,
    RouterLink,
    RouterLinkActive,
    CommonModule,
  ],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent implements OnInit {

  @Input() sidebarVisible: boolean = false;
  @Output() sidebarVisibleChange = new EventEmitter<boolean>();

  @ViewChild('sidebarRef') sidebarRef!: Sidebar;

  private authService = inject(AuthService);
  private router = inject(Router); // Injection du Router

  // Garde la référence à l'élément de menu parent de niveau 1 actuellement ouvert pour l'expansion unique.
  private currentlyExpandedParent: SidebarItem | null = null;

  sidebarItems: SidebarItem[] = [
    { label: 'Accueil', icon: 'pi pi-home', routerLink: '/portail/accueil' },
    { label: 'Tableau de bord ', icon: 'pi pi-chart-bar', routerLink: '/portail/tableau-de-bord', roles: [Role.ADMIN, Role.SUPER_ADMIN] },
    { label: 'Les Comptes Bancaires', icon: 'pi pi-wallet', routerLink: '/portail/comptes-bancaires'},
    {
      label: 'Les Lignes Fonctionnelles',
      icon: 'pi pi-receipt',
      items: [
        { label: 'Les Lignes ponctuelles', icon: 'pi pi-list',
          items:[
            { label: 'Les Lignes encours', icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-encours' },
            { label: 'Les Lignes archivées', icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-archived' },
          ] },
        { label: 'Les Lignes spécifiques', icon: 'pi pi-list',
          items:[
            { label: 'Les lignes encours',icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-specifiques-encours' },
            { label: 'Les lignes archivées',icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-specifiques-archived' },
          ] },

        { label: 'Les Natures des lignes Fonctionnelles', icon: 'pi pi-tag', routerLink: '/portail/natures-fonctionnelles' }
      ]
    },
    {
      label: 'Les Crédits Moyen Terme',
      icon: 'pi pi-money-bill',
      items: [
        { label: 'Les Crédits encours', icon: 'pi pi-list', routerLink: '/portail/cmt/encours' },
        { label: 'Les Crédits en attente de main levées', icon: 'pi pi-list', routerLink: '/portail/cmt/en-attente-de-ML' },
        { label: 'Les Crédits archivés', icon: 'pi pi-list', routerLink: '/portail/cmt/archives' },
      ]
    },
    {
      label: 'Leasing',
      icon: 'pi pi-money-bill',
      items: [
        { label: 'Les Contras encours', icon: 'pi pi-list', routerLink: '/portail/leasing/contratsLeasing/encours' },
        { label: 'Les Contras en attente de main levées ', icon: 'pi pi-list', routerLink: '/portail/leasing/contratsLeasing/en-attente-de-ML' },
        { label: 'Les Contras archivés', icon: 'pi pi-list', routerLink: '/portail/leasing/contratsLeasing/archives' },
        { label: 'Les organismes leasing', icon: 'pi pi-list', routerLink: '/portail/leasing/organismesLeasing' },

      ]
    },
    {
      label: 'Les moyens de paiement',
      icon: 'pi pi-credit-card',
      items: [
        { label: 'Chèques', icon: 'pi pi-arrow-right-arrow-left',
          items: [
            {label:"Encaissements", icon: 'pi pi-plus-circle' , routerLink: '/portail/Cheques/encaissements' },
            {label:"Décaissements", icon: 'pi pi-minus-circle' , routerLink: '/portail/Cheques/decaissements' },
          ]
        },
        { label: 'LCN', icon: 'pi pi-file-o',
          items: [
            {label:"Encaissements", icon: 'pi pi-plus-circle' , routerLink: '/portail/LCN/encaissements' },
            {label:"Décaissements", icon: 'pi pi-minus-circle' , routerLink: '/portail/LCN/decaissements' },
          ]},
        { label: 'Virements', icon: 'pi pi-arrow-right-arrow-left',
          items: [
            {label:"Encaissements", icon: 'pi pi-plus-circle' , routerLink: '/portail/Virements/encaissement' },
            {label:"Décaissements", icon: 'pi pi-minus-circle' , routerLink: '/portail/Virements/decaissement' },
          ]
        },
        { label: 'Mises à disposition ( Décaissements ) ', icon: 'pi pi-arrow-right-arrow-left', routerLink: '/portail/Mise-a-disposition/decaissement'}
      ]
    },
    {
      label:"Les Marchés ",
      icon:"pi pi-sliders-h",
      items:[
        {label: "la liste des marchés",icon: "pi pi-list",routerLink: "/portail/marches"},
        {label: "les préfinancements",icon: "pi pi-list",routerLink: "/portail/préfinancements"},
        {label: "les droits constatés",icon: "pi pi-list",routerLink: "/portail/droits-constate"},
        {label: "Factoring",icon: "pi pi-list",routerLink: "/portail/factoring"},
        {label: "Les décomptes",icon: "pi pi-list",routerLink: "/portail/decomptes"},
      ]},
    {label:"Les Commissions",icon:"pi pi-list",routerLink: '/portail/commissions'},
    {label:"Les Agios",icon:"pi pi-list",routerLink: '/portail/agios'},
    {label:"Les Prélèvements",icon:"pi pi-list",routerLink: '/portail/prelevements'},
    {label:"Les Frais bancaire",icon:"pi pi-list",routerLink: '/portail/frais-bancaire'},
    {label:"Les Intérêts ",icon:"pi pi-list",routerLink: '/portail/interets'},
    {label:"Les Restitutions",icon:"pi pi-list  ",routerLink: '/portail/restitutions'},
    {label:"Impôts et Taxes  ",icon:"pi pi-list",routerLink: '/portail/impots-taxes'},
    {label:"Historique des Rapprochements ",routerLink:"/portail/rapprochement/historique",icon:"pi pi-history"},
    {
      label:"Historique des transactions",
      roles: [Role.ADMIN, Role.SUPER_ADMIN],
      routerLink:"/portail/historique-des-transactions",icon:"pi pi-history"},
    {
      label: 'Paramètres',
      icon: 'pi pi-cog',
      roles: [Role.ADMIN, Role.SUPER_ADMIN],
      items: [
        { label: 'Les Journaux du portail ', icon: 'pi pi-history',routerLink: '/portail/logs' },
      ]
    },
  ];

  filteredSidebarItems: SidebarItem[] = [];

  ngOnInit(): void {
    this.filterSidebarItems();
    this.expandActiveMenu(); // <--- Ouvre l'élément actif au rechargement

    // Subscribe to isAuthenticated$ to re-filter if auth state changes
    this.authService.isAuthenticated$.subscribe(() => {
      this.filterSidebarItems();
      this.expandActiveMenu(); // <--- Ouvre l'élément actif après filtrage/changement d'authentification
    });
  }

  closeCallback(): void {
    this.sidebarVisible = false;
    this.sidebarVisibleChange.emit(this.sidebarVisible);
  }

  // Permet d'ouvrir un seul élément de menu parent de niveau 1 à la fois, et gère l'expansion de n'importe quel sous-menu.
  toggleMenuItem(item: SidebarItem): void {
    if (item.items) {
      // Pour les éléments de niveau 1 (ceux dans filteredSidebarItems)
      const isTopLevelItem = this.filteredSidebarItems.includes(item);

      if (item.expanded) {
        // Si l'élément est déjà ouvert, le fermer.
        item.expanded = false;
        if (isTopLevelItem) {
          this.currentlyExpandedParent = null;
        }
      } else {
        // Si un autre élément parent de niveau 1 est ouvert, le fermer.
        if (isTopLevelItem && this.currentlyExpandedParent && this.currentlyExpandedParent !== item) {
          this.collapseItem(this.currentlyExpandedParent);
        }

        // Ouvrir l'élément cliqué
        item.expanded = true;
        if (isTopLevelItem) {
          this.currentlyExpandedParent = item;
        }
      }
    }
  }

  // Ferme l'élément et tous ses sous-éléments récursivement.
  private collapseItem(item: SidebarItem): void {
    if (item.items) {
      item.expanded = false;
      item.items.forEach(subItem => this.collapseItem(subItem));
    }
  }

  openSidebar(): void {
    this.sidebarVisible = true;
    this.sidebarVisibleChange.emit(this.sidebarVisible);
  }

  private hasRequiredRole(item: SidebarItem): boolean {
    // If no roles are specified for the item, it's visible to everyone
    if (!item.roles || item.roles.length === 0) {
      return true;
    }
    // Check if the user has at least one of the required roles
    return item.roles.some(role => this.authService.hasRole(role));
  }

  private filterSidebarItems(): void {
    const filterAndMap = (items: SidebarItem[]): SidebarItem[] => {
      return items.filter(item => {
        // Si l'élément est un séparateur, on le garde toujours.
        if (item.separator) {
          return true;
        }

        // Check if the current item itself should be visible based on roles
        if (!this.hasRequiredRole(item)) {
          return false;
        }

        // If the item has children, filter them recursively
        if (item.items) {
          const originalItems = [...item.items];
          // Utilise filterAndMap pour le filtrage récursif
          item.items = filterAndMap(originalItems);
          // Garder le parent s'il a au moins un enfant visible.
          return item.items.length > 0;
        }

        // If no children, and hasRequiredRole passed, keep the item
        return true;
      });
    };

    this.filteredSidebarItems = filterAndMap(this.sidebarItems);
  }

  /**
   * Parcourt le menu filtré, vérifie l'URL actuelle et définit `expanded = true`
   * pour les éléments parents de l'élément de menu actif.
   */
  private expandActiveMenu(): void {
    const currentUrl = this.router.url;
    this.currentlyExpandedParent = null;

    const findActiveParent = (items: SidebarItem[], parent: SidebarItem | null = null): boolean => {
      for (const item of items) {
        // Vérifie si le routerLink de l'élément correspond au début de l'URL actuelle.
        // Cela permet de gérer les chemins enfants (ex: /portail/cmt/encours correspond à /portail/cmt)
        if (item.routerLink && currentUrl.startsWith(item.routerLink)) {
          return true; // Un élément actif a été trouvé.
        }

        // Si l'élément a des enfants, chercher récursivement
        if (item.items) {
          if (findActiveParent(item.items, item)) {
            // Un enfant actif a été trouvé, ouvrir ce parent.
            item.expanded = true;

            // Si c'est un parent de niveau 1 (parent === null), stockez-le comme l'actuel.
            if (parent === null) {
              this.currentlyExpandedParent = item;
            }
            return true;
          }
        }
      }
      return false; // Aucun élément actif trouvé dans cette branche.
    };

    findActiveParent(this.filteredSidebarItems);
  }
}
