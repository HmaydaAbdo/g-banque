export interface LogSearchCriteria {
  userName?: string;
  operationType?: string;
  entityName?: string;
  timestampStart?: string | Date;
  timestampEnd?: string | Date;
}
