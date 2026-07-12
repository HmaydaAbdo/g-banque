import { Component, EventEmitter, Input, Output, ViewChild, OnInit, inject } from '@angular/core';
import { Sidebar, SidebarModule } from 'primeng/sidebar';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { StyleClassModule } from 'primeng/styleclass';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NgClass } from '@angular/common';
import { AuthService } from "../../../../auth/services/auth.service";

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
  /**
   * Authority(s) requise(s) pour voir l'item (ex. 'SYSTEM_ADMIN', 'DEPT_TRESORERIE:DIRECTOR').
   * Un tableau exprime un OR (ex. MEMBER ou DIRECTOR d'un même département).
   * SYSTEM_ADMIN voit toujours tout, quel que soit l'item.
   */
  requiredAuthority?: string | string[];
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
    NgClass,
  ],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent implements OnInit {

  @Input() sidebarVisible: boolean = false;
  @Output() sidebarVisibleChange = new EventEmitter<boolean>();

  @ViewChild('sidebarRef') sidebarRef!: Sidebar;

  private authService = inject(AuthService);
  private router = inject(Router);

  // Garde la référence à l'élément de menu parent de niveau 1 actuellement ouvert pour l'expansion unique.
  private currentlyExpandedParent: SidebarItem | null = null;

  sidebarItems: SidebarItem[] = [
    { label: 'Accueil', icon: 'pi pi-home', routerLink: '/portail/accueil' },
    {
      label: 'Les Comptes Bancaires',
      icon: 'pi pi-wallet',
      routerLink: '/portail/comptes-bancaires',
      requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
    },
    {
      label: 'Les Lignes Fonctionnelles',
      icon: 'pi pi-receipt',
      requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
      items: [
        {
          label: 'Les Lignes ponctuelles', icon: 'pi pi-list',
          requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
          items: [
            { label: 'Les Lignes encours', icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-encours', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
            { label: 'Les Lignes archivées', icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-archived', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
          ]
        },
        {
          label: 'Les Lignes spécifiques', icon: 'pi pi-list',
          requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
          items: [
            { label: 'Les lignes encours', icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-specifiques-encours', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
            { label: 'Les lignes archivées', icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-specifiques-archived', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
          ]
        },
        { label: 'Les Natures des lignes Fonctionnelles', icon: 'pi pi-tag', routerLink: '/portail/natures-fonctionnelles', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] }
      ]
    },
    {
      label: 'Les Crédits Moyen Terme',
      icon: 'pi pi-money-bill',
      requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
      items: [
        { label: 'Les Crédits encours', icon: 'pi pi-list', routerLink: '/portail/cmt/encours', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
        { label: 'Les Crédits en attente de main levées', icon: 'pi pi-list', routerLink: '/portail/cmt/en-attente-de-ML', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
        { label: 'Les Crédits archivés', icon: 'pi pi-list', routerLink: '/portail/cmt/archives', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
      ]
    },
    {
      label: 'Leasing',
      icon: 'pi pi-money-bill',
      requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
      items: [
        { label: 'Les Contrats encours', icon: 'pi pi-list', routerLink: '/portail/leasing/contratsLeasing/encours', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
        { label: 'Les Contrats en attente de main levées', icon: 'pi pi-list', routerLink: '/portail/leasing/contratsLeasing/en-attente-de-ML', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
        { label: 'Les Contrats archivés', icon: 'pi pi-list', routerLink: '/portail/leasing/contratsLeasing/archives', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
        { label: 'Les organismes leasing', icon: 'pi pi-list', routerLink: '/portail/leasing/organismesLeasing', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
        { label: 'Les Contrats cadres',    icon: 'pi pi-file-edit', routerLink: '/portail/leasing/contratsCadres', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
      ]
    },
    {
      label: 'Les moyens de paiement',
      icon: 'pi pi-credit-card',
      requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
      items: [
        {
          label: 'Chèques', icon: 'pi pi-arrow-right-arrow-left',
          requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
          items: [
            { label: "Encaissements", icon: 'pi pi-plus-circle', routerLink: '/portail/Cheques/encaissements', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
            { label: "Décaissements", icon: 'pi pi-minus-circle', routerLink: '/portail/Cheques/decaissements', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
          ]
        },
        {
          label: 'LCN', icon: 'pi pi-file-o',
          requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
          items: [
            { label: "Encaissements", icon: 'pi pi-plus-circle', routerLink: '/portail/LCN/encaissements', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
            { label: "Décaissements", icon: 'pi pi-minus-circle', routerLink: '/portail/LCN/decaissements', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
          ]
        },
        {
          label: 'Virements nationaux', icon: 'pi pi-arrow-right-arrow-left',
          requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
          items: [
            { label: "Encaissements", icon: 'pi pi-plus-circle', routerLink: '/portail/Virements/encaissement', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
            { label: "Décaissements", icon: 'pi pi-minus-circle', routerLink: '/portail/Virements/decaissement', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
          ]
        },
        {
          label: 'Virements internationaux',
          icon: 'pi pi-globe',
          routerLink: '/portail/virements-internationaux',
          requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
        },

        { label: 'Mises à disposition (Décaissements)', icon: 'pi pi-arrow-right-arrow-left', routerLink: '/portail/Mise-a-disposition/decaissement', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] }
      ]
    },
    {
      label: "Les Marchés",
      icon: "pi pi-sliders-h",
      items: [
        { label: "La liste des marchés", icon: "pi pi-list", routerLink: "/portail/marches", requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
        { label: "Les préfinancements", icon: "pi pi-list", routerLink: "/portail/préfinancements" },
        { label: "Les droits constatés", icon: "pi pi-list", routerLink: "/portail/droits-constate" },
        { label: "Factoring", icon: "pi pi-list", routerLink: "/portail/factoring" },
        { label: "Les décomptes", icon: "pi pi-list", routerLink: "/portail/decomptes" },
      ]
    },
    {
      label: 'Les Agios',
      icon: 'pi pi-money-bill',
      requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
      items: [
        { label: 'Liste des Agios', icon: 'pi pi-list', routerLink: '/portail/agios', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
        { label: 'Calcul des Agios', icon: 'pi pi-calculator', routerLink: '/portail/agios/calcul', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
      ]
    },
    {
      label: 'Les Cautions',
      icon: 'pi pi-shield',
      requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'],
      items: [
        { label: 'Liste des cautions',       icon: 'pi pi-list',        routerLink: '/portail/cautions',              requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
        { label: 'Commissions sur cautions', icon: 'pi pi-percentage',  routerLink: '/portail/cautions/commissions',  requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
        { label: 'TVA sur cautions',         icon: 'pi pi-receipt',     routerLink: '/portail/cautions/tva',          requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
      ],
    },
    { label: "Les Commissions", icon: "pi pi-list", routerLink: '/portail/commissions' },
    { label: "Les Prélèvements", icon: "pi pi-list", routerLink: '/portail/prelevements', requiredAuthority: ['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR'] },
    { label: "Les Frais bancaires", icon: "pi pi-list", routerLink: '/portail/frais-bancaire' },
    { label: "Les Restitutions", icon: "pi pi-list", routerLink: '/portail/restitutions' },
    { label: "Impôts et Taxes", icon: "pi pi-list", routerLink: '/portail/impots-taxes' },
    { label: "Impayées", icon: "pi pi-list", routerLink: '/portail/impayees' },
    {label: 'Opérations Erronées', icon: 'pi pi-exclamation-triangle', routerLink: '/portail/operations/errones'},
    { label: "Historique des Rapprochements", routerLink: "/portail/rapprochement/historique", icon: "pi pi-history" },
    {
      label: "Historique des transactions",
      requiredAuthority: 'SYSTEM_ADMIN',
      routerLink: "/portail/historique-des-transactions",
      icon: "pi pi-history"
    },
    {
      label: 'Administration',
      icon: 'pi pi-shield',
      requiredAuthority: 'SYSTEM_ADMIN',
      items: [
        { label: 'Départements',        icon: 'pi pi-sitemap',    routerLink: '/portail/admin/departements'      },
        { label: 'Utilisateurs',         icon: 'pi pi-users',      routerLink: '/portail/admin/utilisateurs'      },
        { label: 'Règles de commission', icon: 'pi pi-sliders-h',  routerLink: '/portail/admin/commission-config' },
        { label: 'Configuration leasing', icon: 'pi pi-sliders-h', routerLink: '/portail/admin/leasing-config'    },
        { label: 'Configuration CMT',     icon: 'pi pi-sliders-h', routerLink: '/portail/admin/cmt-config'        },
        { label: 'Configuration Lignes Fonctionnelles', icon: 'pi pi-sliders-h', routerLink: '/portail/admin/lignes-fonctionnelles-config' },
        { label: 'Commissions sur cautions', icon: 'pi pi-percentage', routerLink: '/portail/admin/caution-commission-config' },
      ]
    },
    {
      label: 'Paramètres',
      icon: 'pi pi-cog',
      requiredAuthority: 'SYSTEM_ADMIN',
      items: [
        { label: 'Les Journaux du portail', icon: 'pi pi-history', routerLink: '/portail/logs' },
      ]
    },
  ];

  filteredSidebarItems: SidebarItem[] = [];

  ngOnInit(): void {
    this.filterSidebarItems();
    this.expandActiveMenu();

    // Subscribe to isAuthenticated$ to re-filter if auth state changes
    this.authService.isAuthenticated$.subscribe(() => {
      this.filterSidebarItems();
      this.expandActiveMenu();
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

  private hasRequiredAuthority(item: SidebarItem): boolean {
    // Si aucune authority n'est spécifiée, l'item est visible par tous les authentifiés.
    if (!item.requiredAuthority) {
      return true;
    }
    // SYSTEM_ADMIN a un accès transversal — voit toujours tout.
    if (this.authService.isSystemAdmin()) {
      return true;
    }
    const authorities = this.authService.getAuthorities();
    return Array.isArray(item.requiredAuthority)
        ? item.requiredAuthority.some(auth => authorities.includes(auth))
        : authorities.includes(item.requiredAuthority);
  }

  private filterSidebarItems(): void {
    const filterAndMap = (items: SidebarItem[]): SidebarItem[] => {
      return items.filter(item => {
        // Si l'élément est un séparateur, on le garde toujours.
        if (item.separator) {
          return true;
        }

        // Check if the current item itself should be visible based on authority
        if (!this.hasRequiredAuthority(item)) {
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

        // If no children, and hasRequiredAuthority passed, keep the item
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
