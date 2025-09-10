import { Component, EventEmitter, Input, Output, ViewChild, OnInit, inject } from '@angular/core';
import { Sidebar, SidebarModule } from 'primeng/sidebar';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { StyleClassModule } from 'primeng/styleclass';
import { RouterLink, RouterLinkActive } from '@angular/router';
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
            { label: 'Les Lignes expirées', icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-expired' },
            { label: 'Les Lignes archivées', icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-archived' },
          ] },
        { label: 'Les Lignes spécifiques', icon: 'pi pi-list',
          items:[
            { label: 'Les lignes encours',icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-specifiques-encours' },
            { label: 'Les lignes expirées',icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-specifiques-expired' },
            { label: 'Les lignes archivées',icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles-specifiques-archived' },
          ] },

        { label: 'Les Natures des lignes Fonctionnelles', icon: 'pi pi-tag', routerLink: '/portail/natures-fonctionnelles' }
      ]
    },
    {
      label: 'Leasing',
      icon: 'pi pi-money-bill',
      items: [
        { label: 'Les Contras encours', icon: 'pi pi-list', routerLink: '/portail/leasing/contratsLeasing/encours' },
        { label: 'Les Contras en attente de main levées ', icon: 'pi pi-list', routerLink: '/portail/leasing/contratsLeasing/en-attente-de-ML' },
        { label: 'Les Contras archivés', icon: 'pi pi-list', routerLink: '/portail/leasing/contratsLeasing/archives' },
      ]
    },
    {
      label: 'Les moyens de paiement',
      icon: 'pi pi-credit-card',
      items: [
        { label: 'Chèques', icon: 'pi pi-file-check', routerLink: '/portail/Chèques' },
        { label: 'LCN', icon: 'pi pi-file-o', routerLink: '/portail/LCN' },
        { label: 'Virements', icon: 'pi pi-arrow-right-arrow-left', routerLink: '/portail/Virements' },
        { label: 'Mises à disposition', icon: 'pi pi-briefcase', routerLink: '/portail/Mise-a-disposition' }
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
      label:"Les Marchés ",
      icon:"pi pi-sliders-h",
      items:[
        {label: "la liste des marchés",icon: "pi pi-list",routerLink: "/portail/marches"}
      ]},
    {label:"Historique des transactions",routerLink:"/portail/historique-des-transactions",icon:"pi pi-history"},
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
    // Subscribe to isAuthenticated$ to re-filter if auth state changes
    this.authService.isAuthenticated$.subscribe(() => {
      this.filterSidebarItems();
    });
  }

  closeCallback(): void {
    this.sidebarVisible = false;
    this.sidebarVisibleChange.emit(this.sidebarVisible);
  }

  toggleMenuItem(item: SidebarItem): void {
    if (item.items) {
      item.expanded = !item.expanded;
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
        // First, check if the current item itself should be visible based on roles
        if (!this.hasRequiredRole(item)) {
          return false; // User doesn't have the role for this item, so hide it and its children
        }

        // If the item has children, filter them recursively
        if (item.items) {
          const originalItems = [...item.items]; // Create a shallow copy to avoid modifying the original array during filtering
          item.items = filterAndMap(originalItems); // Recursively filter children
          // Only keep the parent if it has at least one visible child or if the parent itself has no roles defined
          // The hasRequiredRole(item) check ensures the parent is hidden if roles are required and not met.
          // So, if item.items is empty after filtering, we hide the parent.
          return item.items.length > 0;
        }

        // If no children, and hasRequiredRole passed, keep the item
        return true;
      });
    };

    this.filteredSidebarItems = filterAndMap(this.sidebarItems);
  }
}
