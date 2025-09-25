import { Component, EventEmitter, Output, OnInit, OnDestroy, ViewChild, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgForOf, NgIf } from "@angular/common";
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from "../../../../auth/services/auth.service";
import { TooltipModule } from "primeng/tooltip";
import { Role } from "../../../../auth/enum/Role";
import { BadgeModule } from 'primeng/badge';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { OverlayPanel } from 'primeng/overlaypanel';
import { ToastModule } from 'primeng/toast';
import { NotificationService } from "../../../notification/service/NotificationService";
import { WebSocketService } from "../../../notification/service/WebSocketService";
import { NotificationListComponent } from "../../../notification/views/notification-list/notification-list.component";

interface MenuItem {
  label: string;
  icon: string;
  routerLink: string;
  roles?: Role[];
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
    OverlayPanelModule,
    ToastModule,
    NotificationListComponent
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
  @ViewChild('op') op!: OverlayPanel;

  loading: boolean = false;
  private destroy$ = new Subject<void>();
  private authService = inject(AuthService);

  unreadCount: number = 0;

  constructor(
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    public notificationService: NotificationService,
    public webSocketService: WebSocketService
  ) { }

  allMenuItems: MenuItem[] = [
    { label: 'Accueil', icon: 'pi pi-home', routerLink: '/portail/accueil' },
    { label: 'Tableau de bord ', icon: 'pi pi-chart-bar', routerLink: '/portail/tableau-de-bord', roles: [Role.ADMIN, Role.SUPER_ADMIN] },
    { label: 'Comptes Bancaires', icon: 'pi pi-wallet', routerLink: '/portail/comptes-bancaires' },
    { label: 'Lignes Fonctionnelle', icon: 'pi pi-receipt', routerLink: '/portail/lignes-fonctionnelles-encours' },
  ];

  filteredMenuItems: MenuItem[] = [];

  ngOnInit(): void {
    this.filterMenuItems();

    this.authService.isAuthenticated$.pipe(takeUntil(this.destroy$)).subscribe(isAuthenticated => {
      this.filterMenuItems();
      const username = this.authService.getUsername();

      if (isAuthenticated && username) {
        // --- LOGGED IN: Initialize Services and Subscriptions ---
        this.webSocketService.connect(username);
        this.notificationService.initializeNotifications();

        // 1. Subscribe to unread notification count
        this.notificationService.unreadCount$
          .pipe(takeUntil(this.destroy$))
          .subscribe(count => {
            this.unreadCount = count;
          });

        // 2. Subscribe to new public notifications for toast alerts
        this.webSocketService.publicNotifications$
          .pipe(takeUntil(this.destroy$))
          .subscribe(notification => {
            this.messageService.add({
              severity: 'info',
              summary: 'Nouvelle notification ',
              detail: notification.title,
              life: 5000
            });
          });

        // 3. Subscribe to new private notifications for toast alerts
        this.webSocketService.privateNotifications$
          .pipe(takeUntil(this.destroy$))
          .subscribe(notification => {
            this.messageService.add({
              severity: 'warn',
              summary: 'Nouvelle notification privée',
              detail: notification.title,
              life: 5000
            });
          });

      } else {
        // --- LOGGED OUT: Clean up ---
        this.webSocketService.disconnect();
        this.notificationService.resetNotifications();
        this.unreadCount = 0;
      }
    });
  }

  private hasRequiredRole(item: MenuItem): boolean {
    if (!item.roles || item.roles.length === 0) {
      return true;
    }
    return item.roles.some(role => this.authService.hasRole(role));
  }

  private filterMenuItems(): void {
    this.filteredMenuItems = this.allMenuItems.filter(item => this.hasRequiredRole(item));
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
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Une erreur est survenue lors de la déconnexion.' });
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
