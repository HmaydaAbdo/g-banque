import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject, takeUntil, tap } from 'rxjs';
import {environment} from "../../../../environments/environment";
import {NotificationModel} from "../dtos/Notification";
import {WebSocketService} from "./WebSocketService";


@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {

  private readonly API_URL = `${environment.apiUrl}notification`;
  private destroy$ = new Subject<void>();

  // Use a BehaviorSubject for the list of unread notifications
  private unreadNotificationsSubject = new BehaviorSubject<NotificationModel[]>([]);
  public unreadNotifications$: Observable<NotificationModel[]> = this.unreadNotificationsSubject.asObservable();

  // Use a BehaviorSubject for the unread count
  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$: Observable<number> = this.unreadCountSubject.asObservable();

  // Pagination state
  private currentPage = 0;
  private pageSize = 10;
  private hasMoreUnread = true; // To know if there are more pages to load

  constructor(
    private http: HttpClient,
    private webSocketService: WebSocketService
  ) {
    // Subscribe to new public notifications from WebSocket
    this.webSocketService.publicNotifications$
      .pipe(takeUntil(this.destroy$))
      .subscribe(newNotification => {
        console.log('NotificationService: New public notification received via WebSocket', newNotification);
        // Add new notification to the beginning of the unread list
        const currentNotifications = this.unreadNotificationsSubject.getValue();
        this.unreadNotificationsSubject.next([newNotification, ...currentNotifications]);
        // Increment unread count
        this.unreadCountSubject.next(this.unreadCountSubject.getValue() + 1);
      });


  }

  /**
   * Initializes the notification service by fetching the first page of unread notifications.
   * This should be called once, e.g., on application startup or user login.
   */
  public initializeNotifications(): void {
    console.log('Initializing notification service...');
    this.currentPage = 0; // Reset page for initial load
    this.hasMoreUnread = true; // Assume there's more until proven otherwise
    this.unreadNotificationsSubject.next([]); // Clear existing notifications
    this.loadMoreUnreadNotifications(); // Load the first set
  }

  /**
   * Fetches a paginated list of unread notifications from the backend.
   * Updates the unreadNotificationsSubject and unreadCountSubject.
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
      .set('sort', 'timestamp,desc'); // Always sort by timestamp descending

    this.http.get<any>(`${this.API_URL}/unread`, { params })
      .pipe(
        tap(response => {
          const newNotifications: NotificationModel[] = response.content;
          const currentNotifications = this.unreadNotificationsSubject.getValue();

          // Append new notifications to the existing list
          this.unreadNotificationsSubject.next([...currentNotifications, ...newNotifications]);

          // Update pagination state
          this.hasMoreUnread = !response.last; // 'last' property from Spring Page
          this.currentPage++; // Increment for next load
          this.unreadCountSubject.next(response.totalElements);
          console.log(`Loaded ${newNotifications.length} notifications. Total unread: ${this.unreadCountSubject.getValue()}`);
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        error: (err) => console.error('Error loading unread notifications:', err)
      });
  }

  /**
   * Marks a notification as read on the backend and updates the frontend state.
   * @param notificationId The ID of the notification to mark as read.
   */
  public markNotificationAsRead(notificationId: number): Observable<any> {
    return this.http.put(`${this.API_URL}/${notificationId}/mark-as-read`, {})
      .pipe(
        tap(() => {
          // Update the local list: remove the marked notification and decrement count
          const currentNotifications = this.unreadNotificationsSubject.getValue();
          const updatedNotifications = currentNotifications.filter(n => n.id !== notificationId);
          this.unreadNotificationsSubject.next(updatedNotifications);
          this.unreadCountSubject.next(this.unreadCountSubject.getValue() - 1);
          console.log(`Notification ${notificationId} marked as read. New unread count: ${this.unreadCountSubject.getValue()}`);
        }),
        takeUntil(this.destroy$)
      );
  }

  /**
   * Resets the pagination state and clears current notifications.
   * Useful when the user logs out or count needs a full re-sync.
   */
  public resetNotifications(): void {
    this.currentPage = 0;
    this.hasMoreUnread = true;
    this.unreadNotificationsSubject.next([]);
    this.unreadCountSubject.next(0);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
