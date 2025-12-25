import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import SockJS from 'sockjs-client';
import { environment } from "../../../../environments/environment";
import { NotificationModel } from "../dtos/Notification";
import { Client, IMessage, IFrame, StompSubscription } from '@stomp/stompjs';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  private stompClient: Client | undefined;
  private isConnected = new BehaviorSubject<boolean>(false);

  // Observable for components to subscribe to connection status
  public isConnected$: Observable<boolean> = this.isConnected.asObservable();

  // RxJS Subjects to emit incoming notifications
  private publicNotificationsSubject = new Subject<NotificationModel>();
  private privateNotificationsSubject = new Subject<NotificationModel>();

  // Expose these Subjects as Observables for components to subscribe
  public publicNotifications$: Observable<NotificationModel> = this.publicNotificationsSubject.asObservable();
  public privateNotifications$: Observable<NotificationModel> = this.privateNotificationsSubject.asObservable();

  private readonly WEBSOCKET_ENDPOINT = `${environment.apiUrl}ws`;

  constructor() { }

  /**
   * Establishes the WebSocket connection and emits connection status.
   * @param username The unique identifier of the user for private message subscriptions.
   */
  public connect(username: string): void {
    if (this.stompClient && this.stompClient.active) {
      console.log('STOMP client already connected.');
      return;
    }

    console.log('Attempting to connect to WebSocket...');

    // Create new STOMP client
    this.stompClient = new Client({
      // WebSocket factory using SockJS
      webSocketFactory: () => new SockJS(this.WEBSOCKET_ENDPOINT),

      // Connection timeout
      connectionTimeout: 5000,

      // Reconnect settings
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      // Debug logging (suppressed)
      debug: (str: string) => {
        // Uncomment for debugging:
        // console.log('STOMP Debug:', str);
      },

      // On successful connection
      onConnect: (frame: IFrame) => {
        console.log('WebSocket Connected:', frame);
        this.isConnected.next(true);

        // Subscribe to Public Notifications
        this.stompClient!.subscribe('/topic/public-notifications', (message: IMessage) => {
          const notification: NotificationModel = JSON.parse(message.body);
          console.log('Received public notification:', notification);
          this.publicNotificationsSubject.next(notification);
        });

        // Subscribe to Private Notifications
        this.stompClient!.subscribe(`/user/queue/private-notifications`, (message: IMessage) => {
          const notification: NotificationModel = JSON.parse(message.body);
          console.log(`Received private notification for ${username}:`, notification);
          this.privateNotificationsSubject.next(notification);
        });

        console.log(`STOMP: Subscribed to notifications for user: ${username}`);
      },

      // On disconnection
      onDisconnect: () => {
        console.log('Disconnected from WebSocket');
        this.isConnected.next(false);
      },

      // On error
      onStompError: (frame: IFrame) => {
        console.error('WebSocket connection error:', frame.headers['message']);
        console.error('Error details:', frame.body);
        this.isConnected.next(false);
      }
    });

    // Activate the connection
    this.stompClient.activate();
  }

  /**
   * Disconnects from the WebSocket.
   */
  public disconnect(): void {
    if (this.stompClient && this.stompClient.active) {
      this.stompClient.deactivate();
      console.log('Disconnecting from WebSocket...');
    } else {
      console.log('Not connected to WebSocket.');
    }
  }

  /**
   * Send message from client to server
   */
  public sendMessage(destination: string, body: any): void {
    if (this.stompClient && this.stompClient.active) {
      this.stompClient.publish({
        destination: destination,
        body: JSON.stringify(body)
      });
      console.log('STOMP: Sent message to ' + destination, body);
    } else {
      console.warn('STOMP: Not connected, message not sent.');
    }
  }

  /**
   * Check if currently connected
   */
  public isActive(): boolean {
    return this.stompClient ? this.stompClient.active : false;
  }
}
