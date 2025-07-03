import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs'; // Import Subject
import SockJS from 'sockjs-client';
import * as Stomp from 'stompjs';
import {environment} from "../../../../environments/environment";
import {NotificationModel} from "../dtos/Notification"; // Import your NotificationModel

@Injectable({
  providedIn: 'root'
})
export class WebSocketService { // Renamed from NotificationWebSocketService

  private stompClient: Stomp.Client | undefined;
  private isConnected = new BehaviorSubject<boolean>(false);

  // Observable for components to subscribe to connection status
  public isConnected$: Observable<boolean> = this.isConnected.asObservable();

  // RxJS Subjects to emit incoming notifications (Re-added)
  private publicNotificationsSubject = new Subject<NotificationModel>();
  private privateNotificationsSubject = new Subject<NotificationModel>();

  // Expose these Subjects as Observables for components to subscribe (Re-added)
  public publicNotifications$: Observable<NotificationModel> = this.publicNotificationsSubject.asObservable();
  public privateNotifications$: Observable<NotificationModel> = this.privateNotificationsSubject.asObservable();

  private readonly WEBSOCKET_ENDPOINT =`${environment.apiUrl}ws`;

  constructor() { }

  /**
   * Establishes the WebSocket connection and emits connection status.
   * @param username The unique identifier of the user for private message subscriptions.
   */
  public connect(username: string): void { // username parameter is essential for private subs
    if (this.stompClient && this.stompClient.connected) {
      console.log('STOMP client already connected.');
      return;
    }

    console.log('Attempting to connect to WebSocket...');
    const ws = new SockJS(this.WEBSOCKET_ENDPOINT);
    this.stompClient = Stomp.over(ws);

    // Suppress console logs from stompjs library itself
    this.stompClient.debug = () => {};

    this.stompClient.connect(
      {}, // Empty headers for now
      (frame: Stomp.Frame | undefined) => {
        // CONNECT CALLBACK - Logic for successful connection
        console.log('WebSocket Connected:', frame ? frame.toString() : 'No frame info');
        this.isConnected.next(true); // Update connection status to true

        // --- RE-ADDED: Subscribe to Public Notifications ---
        this.stompClient!.subscribe('/topic/public-notifications', (message: Stomp.Message) => {
          const notification: NotificationModel = JSON.parse(message.body);
          console.log('Received public notification:', notification);
          this.publicNotificationsSubject.next(notification); // Emit to subscribers
        }, { id: 'public-sub' }); // Optional: give subscription an ID

        // --- RE-ADDED: Subscribe to Private Notifications (if username is provided) ---
        // Note: `username` is now a required parameter for `connect` as per your usage.
        this.stompClient!.subscribe(`/user/queue/private-notifications`, (message: Stomp.Message) => {
          const notification: NotificationModel = JSON.parse(message.body);
          console.log(`Received private notification for ${username}:`, notification);
          this.privateNotificationsSubject.next(notification); // Emit to subscribers
        }, { id: `private-sub-${username}` }); // Optional: give subscription an ID
        console.log(`STOMP: Subscribed to private notifications for user: ${username}`);

      },
      (error: string | Stomp.Frame) => {
        // ERROR CALLBACK - Handle connection errors.
        console.error('WebSocket connection error:', error);
        this.isConnected.next(false); // Update connection status to false
        // You might want to implement a reconnection strategy here, e.g.:
        // setTimeout(() => this.connect(username), 5000); // Pass username for reconnection
      }
    );
  }

  /**
   * Disconnects from the WebSocket.
   */
  public disconnect(): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.disconnect(() => {
        console.log('Disconnected from WebSocket');
        this.isConnected.next(false); // Update connection status to false
      });
    } else {
      console.log('Not connected to WebSocket.');
    }
  }

  // Optional: Method to send messages from client to server (for future use)
  public sendMessage(destination: string, body: any): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.send(destination, {}, JSON.stringify(body));
      console.log('STOMP: Sent message to ' + destination, body);
    } else {
      console.warn('STOMP: Not connected, message not sent.');
    }
  }
}
