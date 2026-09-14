import { Component, OnInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule, NgClass } from '@angular/common';
import { NotificationModel } from '../../dtos/Notification';
import { Subject, takeUntil } from 'rxjs';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { NotificationService } from "../../service/NotificationService";
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';


@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [
    CommonModule,
    CardModule,
    TagModule,
    SkeletonModule,
    NgClass,
    TooltipModule,
    ConfirmDialogModule
  ],
  providers: [ConfirmationService],
  templateUrl: './notification-list.component.html'
})
export class NotificationListComponent implements OnInit, OnDestroy {
  @Input() notificationService!: NotificationService;
  @Output() notificationMarkedAsRead = new EventEmitter<void>();

  notifications: NotificationModel[] = [];
  private destroy$ = new Subject<void>();
  isLoading: boolean = false;

  constructor(private confirmationService: ConfirmationService) { }

  ngOnInit(): void {
    this.notificationService.unreadNotifications$
      .pipe(takeUntil(this.destroy$))
      .subscribe(notifications => {
        this.notifications = notifications;
      });
  }

  onScroll(event: Event): void {
    const element = event.target as HTMLElement;
    if (element.scrollHeight - element.scrollTop <= element.clientHeight + 1) {
      if (!this.isLoading) {
        this.isLoading = true;
        this.notificationService.loadMoreUnreadNotifications();
        setTimeout(() => {
          this.isLoading = false;
        }, 500);
      }
    }
  }

  /**
   * Affiche un dialogue de confirmation avec les détails de la notification.
   * Lors de la confirmation, marque la notification comme lue.
   * @param notification Le modèle de notification à afficher et potentiellement marquer comme lu.
   */
  confirmNotificationAction(notification: NotificationModel): void {
    // Construction du message pour le dialogue de confirmation en français
    const messageContent = `
      <p> ${notification.title}</p>
      <p>${notification.content}</p>
      ${notification.readStatus ? '<p class="text-green-500">Cette notification est déjà lue.</p>' : '<p class="text-blue-500">Voulez-vous la marquer comme lue ?</p>'}
    `;

    this.confirmationService.confirm({
      key: 'notificationConfirm', // Clé unique si vous avez plusieurs dialogues de confirmation
      header: 'Détails de la notification', // En-tête en français
      message: messageContent, // Message construits
      acceptLabel: 'Oui',
      rejectLabel: 'Non', // Labels en français
      acceptVisible: true, // Toujours afficher le bouton accepter
      rejectVisible: !notification.readStatus, // Cacher le bouton rejeter si déjà lu

      accept: () => {
        // Marquer comme lu uniquement si ce n'est pas déjà lu
        if (!notification.readStatus) {
          this.markAsRead(notification);
        }
      },
      reject: () => {
        // L'utilisateur a choisi de garder non lu ou a annulé
        console.log(`Notification ${notification.id} gardée non lue.`);
      }
    });
  }

  markAsRead(notification: NotificationModel): void {
    if (!notification.readStatus) {
      this.notificationService.markNotificationAsRead(notification.id)
        .subscribe({
          next: () => {
            console.log(`Notification ${notification.id} marked as read.`);
            this.notificationMarkedAsRead.emit();
          },
          error: (err) => {
            console.error('Error marking notification as read:', err);
          }
        });
    }
    window.location.reload()
  }

  getSeverity(type: string): "info" | "warning" | "danger" | "success" | "secondary" | "contrast" {
    switch (type) {
      case 'INFO': return 'info';
      case 'WARNING': return 'warning';
      case 'ALERT': return 'danger';
      case 'SUCCESS': return 'success';
      default: return 'info';
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
