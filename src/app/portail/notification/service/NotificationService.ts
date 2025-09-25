import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject, takeUntil, tap } from 'rxjs';
import { environment } from "../../../../environments/environment";
import { NotificationModel } from "../dtos/Notification";
import { WebSocketService } from "./WebSocketService";
import { PageResponse } from "../../global/types/PageResponse"; // Assuming this type is defined


@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {

  private readonly API_URL = `${environment.apiUrl}notification`;
  private destroy$ = new Subject<void>();

  private unreadNotificationsSubject = new BehaviorSubject<NotificationModel[]>([]);
  public unreadNotifications$: Observable<NotificationModel[]> = this.unreadNotificationsSubject.asObservable();

  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$: Observable<number> = this.unreadCountSubject.asObservable();

  // Single source of truth for the total unread count
  private totalUnreadElements: number = 0;
  // Pagination state (Spring pages are 0-indexed)
  private currentPage = 0;
  private pageSize = 10;
  private hasMoreUnread = true;

  constructor(
    private http: HttpClient,
    private webSocketService: WebSocketService
  ) {
    // 1. WebSocket Subscription: Updates local state and increments the total count
    this.webSocketService.publicNotifications$
      .pipe(takeUntil(this.destroy$))
      .subscribe(newNotification => {
        console.log('NotificationService: New public notification received via WebSocket', newNotification);

        const currentNotifications = this.unreadNotificationsSubject.getValue();
        this.unreadNotificationsSubject.next([newNotification, ...currentNotifications]);

        // Synchronized increment
        this.totalUnreadElements++;
        this.unreadCountSubject.next(this.totalUnreadElements);
      });
  }

  /**
   * Initializes the notification service by fetching the first page of unread notifications.
   */
  public initializeNotifications(): void {
    console.log('Initializing notification service...');
    this.currentPage = 0;
    this.hasMoreUnread = true;
    this.totalUnreadElements = 0;
    this.unreadNotificationsSubject.next([]);
    this.unreadCountSubject.next(0);
    this.loadMoreUnreadNotifications();
  }

  /**
   * Fetches a paginated list of unread notifications from the backend.
   */
  public loadMoreUnreadNotifications(): void {
    if (!this.hasMoreUnread) {
      console.log('No more unread notifications to load.');
      return;
    }

    console.log(`Loading unread notifications: page ${this.currentPage}, size ${this.pageSize}`);
    let params = new HttpParams()
      .set('page', this.currentPage.toString())
      .set('size', this.pageSize.toString())
      .set('sort', 'timestamp,desc');

    this.http.get<PageResponse<NotificationModel>>(`${this.API_URL}/unread`, { params })
      .pipe(
        tap(response => {
          const newNotifications: NotificationModel[] = response.content;
          const currentNotifications = this.unreadNotificationsSubject.getValue();

          // Append new notifications to the existing list
          this.unreadNotificationsSubject.next([...currentNotifications, ...newNotifications]);

          // Get pagination indices
          const totalPages = response.page.totalPages;
          const currentPageIndex = response.page.number;

          // CRITICAL FIX: Determine if there are more pages using totalPages and current index
          this.hasMoreUnread = (currentPageIndex + 1) < totalPages;

          // Increment current page counter for the *next* request
          this.currentPage++;

          // Synchronization FIX: Only set the total count from the API on the first page load
          if (currentPageIndex === 0) {
            this.totalUnreadElements = response.page.totalElements;
          }

          // Emit the total count from the synchronized variable
          this.unreadCountSubject.next(this.totalUnreadElements);
          console.log(`Loaded ${newNotifications.length} notifications. Total unread: ${this.totalUnreadElements}`);
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        error: (err) => console.error('Error loading unread notifications:', err)
      });
  }

  /**
   * Marks a notification as read on the backend and updates the frontend state.
   */
  public markNotificationAsRead(notificationId: number): Observable<any> {
    return this.http.put(`${this.API_URL}/${notificationId}/mark-as-read`, {})
      .pipe(
        tap(() => {
          // Update the local list
          const currentNotifications = this.unreadNotificationsSubject.getValue();
          const updatedNotifications = currentNotifications.filter(n => n.id !== notificationId);
          this.unreadNotificationsSubject.next(updatedNotifications);

          // Synchronization FIX: Decrement the private total count variable
          this.totalUnreadElements = Math.max(0, this.totalUnreadElements - 1);
          this.unreadCountSubject.next(this.totalUnreadElements);
          console.log(`Notification ${notificationId} marked as read. New unread count: ${this.totalUnreadElements}`);
        }),
        takeUntil(this.destroy$)
      );
  }

  /**
   * Resets the pagination state and clears current notifications.
   */
  public resetNotifications(): void {
    this.currentPage = 0;
    this.hasMoreUnread = true;
    this.totalUnreadElements = 0;
    this.unreadNotificationsSubject.next([]);
    this.unreadCountSubject.next(0);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
