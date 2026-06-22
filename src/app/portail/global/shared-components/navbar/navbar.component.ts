import { Component, EventEmitter, Output, OnInit, OnDestroy, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgForOf, NgIf } from "@angular/common";
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from "../../../../auth/services/auth.service";
import { TooltipModule } from "primeng/tooltip";
import { BadgeModule } from 'primeng/badge';
import { ToastModule } from 'primeng/toast';
import { NotificationService } from "../../../notification/service/NotificationService";
import { NotificationListComponent } from "../../../notification/views/notification-list/notification-list.component";
import { DialogModule } from "primeng/dialog";
import { ThemeService } from "../../services/ThemeService";

interface MenuItem {
  label: string;
  icon: string;
  routerLink: string;
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    ButtonModule,
    RippleModule,
    RouterLink,
    RouterLinkActive,
    NgForOf,
    NgIf,
    ConfirmDialogModule,
    TooltipModule,
    BadgeModule,
    ToastModule,
    NotificationListComponent,
    DialogModule
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
  providers: [
    ConfirmationService,
    MessageService
  ]
})
export class NavbarComponent implements OnInit, OnDestroy {
  @Output() openSidebar = new EventEmitter<void>();

  loading: boolean = false;
  private destroy$ = new Subject<void>();
  private authService = inject(AuthService);

  unreadCount: number = 0;
  notificationDialogVisible: boolean = false;

  constructor(
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    public notificationService: NotificationService,
    public themeService: ThemeService,
  ) { }

  allMenuItems: MenuItem[] = [
    { label: 'Accueil', icon: 'pi pi-home', routerLink: '/portail/accueil' },
    {label:"Historique des Rapprochements ",routerLink:"/portail/rapprochement/historique",icon:"pi pi-history"},
    { label: 'Comptes Bancaires', icon: 'pi pi-wallet', routerLink: '/portail/comptes-bancaires' },
    { label: 'Lignes Ponctuelles', icon: 'pi pi-receipt', routerLink: '/portail/lignes-fonctionnelles-encours' },
  ];

  filteredMenuItems: MenuItem[] = [];

  ngOnInit(): void {
    this.filterMenuItems();

    this.authService.isAuthenticated$.pipe(takeUntil(this.destroy$)).subscribe(isAuthenticated => {
      this.filterMenuItems();
      const username = this.authService.getUsername();

      if (isAuthenticated && username) {
        // --- LOGGED IN: Initialize Services and Subscriptions ---
        this.notificationService.initializeNotifications();

        // 1. Subscribe to unread notification count
        this.notificationService.unreadCount$
          .pipe(takeUntil(this.destroy$))
          .subscribe(count => {
            this.unreadCount = count;
          });




      } else {
        this.notificationService.resetNotifications();
        this.unreadCount = 0;
      }
    });
  }

  showNotificationDialog(): void {
    this.notificationDialogVisible = true;
  }

  private filterMenuItems(): void {
    this.filteredMenuItems = this.allMenuItems;
  }

  confirmLogout(event: Event) {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: 'Êtes-vous sûr de vouloir vous déconnecter ?',
      acceptLabel: 'Oui, Déconnexion',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text p-button-secondary',
      accept: () => {
        this.logout();
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'La déconnexion a été annulée.' });
      }
    });
  }

  logout() {
    this.loading = true;
    this.authService.logout().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (success) => {
        if (success) {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Vous avez été déconnecté(e) avec succès !' });
        } else {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec de la déconnexion. Veuillez réessayer.' });
        }
      },
      error: (err) => {
        console.error('Logout error:', err);
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
