import {LogEntryDetailResponse} from "./LogEntryDetailResponse";

export interface LogResponse {
  id: number;
  timestamp: Date;
  userName: string;
  operationType: string;
  entityName: string;
  entityId: number;
  logDetails: LogEntryDetailResponse[];
}
