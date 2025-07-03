import { Component, EventEmitter, Input, Output, ViewChild, OnInit, inject } from '@angular/core';
import { Sidebar, SidebarModule } from 'primeng/sidebar';
import { Button } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { StyleClassModule } from 'primeng/styleclass';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgFor, NgIf, NgClass } from '@angular/common';
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
    Button,
    Ripple,
    StyleClassModule,
    RouterLink,
    RouterLinkActive,
    NgFor,
    NgIf,
    NgClass,
  ],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent implements OnInit { // Implement OnInit
  @Input() sidebarVisible: boolean = false;
  @Output() sidebarVisibleChange = new EventEmitter<boolean>();

  @ViewChild('sidebarRef') sidebarRef!: Sidebar;

  private authService = inject(AuthService); // Inject AuthService

  sidebarItems: SidebarItem[] = [
    { label: 'Tableau de bord ', icon: 'pi pi-chart-bar', routerLink: '/portail/tableau-de-bord', roles: [Role.ADMIN, Role.SUPER_ADMIN] },
    { label: 'Accueil', icon: 'pi pi-home', routerLink: '/portail/accueil' },
    { label: 'Comptes Bancaires', icon: 'pi pi-wallet', items:
        [
          { label: 'Gestion des comptes', icon: 'pi pi-list', routerLink: '/portail/comptes-bancaires' },
          { label: 'Historique', icon: 'pi pi-history', routerLink: '/portail/select-for-history' }
       ]
    },
    {
      label: 'Lignes Fonctionnelle',
      icon: 'pi pi-receipt',
      items: [
        { label: 'Liste des Lignes', icon: 'pi pi-list', routerLink: '/portail/lignes-fonctionnelles' },
        { label: 'Natures Fonctionnelles', icon: 'pi pi-tag', routerLink: '/portail/natures-fonctionnelles' }
      ]
    },
    {
      label: 'Settings',
      icon: 'pi pi-cog',
      roles: [Role.ADMIN, Role.SUPER_ADMIN],
      items: [
        { label: 'Logs', icon: 'pi pi-history',routerLink: '/portail/logs' },
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

  closeCallback(e: MouseEvent): void {
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
