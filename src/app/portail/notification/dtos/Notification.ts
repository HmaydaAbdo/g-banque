import {NotificationType} from "../enums/NotificationType";


export interface NotificationModel {
  id: number;
  title: string;
  content: string;
  type: NotificationType;
  timestamp: string;
  readStatus: boolean;
}
